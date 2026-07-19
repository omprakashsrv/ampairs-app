package com.ampairs.ecom.ui.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.ecom.api.model.CheckoutRequest
import com.ampairs.ecom.api.model.EcomApiException
import com.ampairs.ecom.api.model.LinkCandidateResponse
import com.ampairs.ecom.data.db.entity.CustomerAddressEntity
import com.ampairs.ecom.data.repository.AddressRepository
import com.ampairs.ecom.data.repository.CartRepository
import com.ampairs.ecom.data.repository.CustomerLinkRepository
import com.ampairs.ecom.data.repository.EcomOrderRepository
import com.ampairs.ecom.domain.EcomSession
import com.ampairs.sync.CentralSyncService
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEvent
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CheckoutUiState(
    val addresses: List<CustomerAddressEntity> = emptyList(),
    val selectedAddressId: String? = null,
    val notes: String = "",
    val subtotal: Double = 0.0,
    val itemCount: Int = 0,
    val isPlacing: Boolean = false,
)

sealed interface CheckoutEvent {
    data class OrderPlaced(val orderRef: String, val orderNumber: String) : CheckoutEvent
    data class Error(val message: String) : CheckoutEvent
    /** No distributor link and no phone-match candidate either — the backend blocked checkout (ECOM_NOT_LINKED). */
    data class NotLinked(val message: String) : CheckoutEvent
    /** A CRM account matching the buyer's own phone was found — offer to link before retrying checkout. */
    data class LinkCandidateFound(val candidate: LinkCandidateResponse) : CheckoutEvent
}

private const val DEFAULT_NOT_LINKED_MESSAGE =
    "Your account isn't linked to this business yet. Please contact the business owner to get linked before placing an order."

@Inject
@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
class CheckoutViewModel(
    private val addressRepository: AddressRepository,
    private val cartRepository: CartRepository,
    private val orderRepository: EcomOrderRepository,
    private val linkRepository: CustomerLinkRepository,
    private val session: EcomSession,
    private val syncService: CentralSyncService,
) : ViewModel() {

    private val form = MutableStateFlow(CheckoutForm())
    private data class CheckoutForm(
        val selectedAddressId: String? = null,
        val notes: String = "",
        val isPlacing: Boolean = false,
    )

    private val _events = MutableSharedFlow<CheckoutEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<CheckoutEvent> = _events.asSharedFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<CheckoutUiState> = session.active.flatMapLatest { active ->
        if (active == null) flowOf(CheckoutUiState())
        else combine(
            addressRepository.observeAddresses(),
            cartRepository.observeItems(active.storefrontId),
            form,
        ) { addresses, items, f ->
            val selected = f.selectedAddressId
                ?: addresses.firstOrNull { it.is_default == 1 }?.uid
                ?: addresses.firstOrNull()?.uid
            CheckoutUiState(
                addresses = addresses,
                selectedAddressId = selected,
                notes = f.notes,
                subtotal = items.sumOf { it.unit_price * it.quantity },
                itemCount = items.sumOf { it.quantity },
                isPlacing = f.isPlacing,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CheckoutUiState())

    init {
        // Make sure saved addresses are fresh for selection.
        syncService.emit(SyncEvent.TriggerPull(SyncEntity.ECOM_ADDRESS))
    }

    fun selectAddress(addressId: String) = form.update { it.copy(selectedAddressId = addressId) }
    fun setNotes(notes: String) = form.update { it.copy(notes = notes) }

    fun placeOrder() {
        val slug = session.activeSlug ?: return
        val storefrontId = session.activeStorefrontId ?: return
        val addressId = uiState.value.selectedAddressId
        if (addressId == null) {
            _events.tryEmit(CheckoutEvent.Error("Please select a delivery address"))
            return
        }
        if (form.value.isPlacing) return
        form.update { it.copy(isPlacing = true) }
        viewModelScope.launch {
            // Addresses are offline-first — a just-created one may not have reached the server yet,
            // and checkout looks it up by id. Push it now rather than racing the reactive sync cycle.
            val resolvedAddressId = addressRepository.ensureSynced(addressId).getOrElse {
                form.update { it.copy(isPlacing = false) }
                _events.tryEmit(CheckoutEvent.Error(it.message ?: "Couldn't sync your delivery address"))
                return@launch
            }
            // The cart is built offline; materialise it to the server now. This is where stock /
            // availability are validated — a failure here is surfaced before the order is placed.
            val token = cartRepository.materializeToServer(slug, storefrontId).getOrElse {
                form.update { it.copy(isPlacing = false) }
                _events.tryEmit(CheckoutEvent.Error(it.message ?: "Couldn't validate your cart"))
                return@launch
            }
            val request = CheckoutRequest(deliveryAddressId = resolvedAddressId, notes = uiState.value.notes.ifBlank { null })
            orderRepository.checkout(slug, token, request).fold(
                onSuccess = { order ->
                    // The local cart has been turned into an order — clear it.
                    cartRepository.clear(storefrontId)
                    syncService.emit(SyncEvent.TriggerPull(SyncEntity.ECOM_ORDER))
                    _events.tryEmit(CheckoutEvent.OrderPlaced(order.ecomOrderRef, order.orderNumber))
                },
                onFailure = {
                    if (it is EcomApiException && it.code == "ECOM_NOT_LINKED") {
                        handleNotLinked(slug, it.message)
                    } else {
                        _events.tryEmit(CheckoutEvent.Error(it.message ?: "Couldn't place order"))
                    }
                },
            )
            form.update { it.copy(isPlacing = false) }
        }
    }

    /** Checkout was blocked as ECOM_NOT_LINKED — check for a phone-match candidate before giving up. */
    private suspend fun handleNotLinked(slug: String, fallbackMessage: String?) {
        linkRepository.getLinkCandidate(slug).fold(
            onSuccess = { candidate ->
                if (candidate != null) {
                    _events.tryEmit(CheckoutEvent.LinkCandidateFound(candidate))
                } else {
                    _events.tryEmit(CheckoutEvent.NotLinked(fallbackMessage ?: DEFAULT_NOT_LINKED_MESSAGE))
                }
            },
            onFailure = { _events.tryEmit(CheckoutEvent.NotLinked(fallbackMessage ?: DEFAULT_NOT_LINKED_MESSAGE)) },
        )
    }

    /** The buyer approved a [CheckoutEvent.LinkCandidateFound] — link, then retry the original checkout. */
    fun confirmLink(customerId: String) {
        val slug = session.activeSlug ?: return
        if (form.value.isPlacing) return
        form.update { it.copy(isPlacing = true) }
        viewModelScope.launch {
            linkRepository.confirmLink(slug, customerId).fold(
                onSuccess = {
                    form.update { it.copy(isPlacing = false) }
                    placeOrder()
                },
                onFailure = {
                    form.update { it.copy(isPlacing = false) }
                    _events.tryEmit(CheckoutEvent.Error(it.message ?: "Couldn't link your account"))
                },
            )
        }
    }
}
