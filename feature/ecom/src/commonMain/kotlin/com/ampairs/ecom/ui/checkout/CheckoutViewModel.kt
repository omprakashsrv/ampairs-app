package com.ampairs.ecom.ui.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.ecom.api.model.CheckoutRequest
import com.ampairs.ecom.data.db.entity.CustomerAddressEntity
import com.ampairs.ecom.data.repository.AddressRepository
import com.ampairs.ecom.data.repository.CartRepository
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
    data class OrderPlaced(val orderRef: String) : CheckoutEvent
    data class Error(val message: String) : CheckoutEvent
}

@Inject
@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
class CheckoutViewModel(
    private val addressRepository: AddressRepository,
    private val cartRepository: CartRepository,
    private val orderRepository: EcomOrderRepository,
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
            val token = cartRepository.sessionToken(storefrontId)
            if (token == null) {
                form.update { it.copy(isPlacing = false) }
                _events.tryEmit(CheckoutEvent.Error("Cart session expired"))
                return@launch
            }
            val request = CheckoutRequest(deliveryAddressId = addressId, notes = uiState.value.notes.ifBlank { null })
            orderRepository.checkout(slug, token, request).fold(
                onSuccess = { order ->
                    syncService.emit(SyncEvent.TriggerPull(SyncEntity.ECOM_ORDER))
                    _events.tryEmit(CheckoutEvent.OrderPlaced(order.ecomOrderRef))
                },
                onFailure = { _events.tryEmit(CheckoutEvent.Error(it.message ?: "Couldn't place order")) },
            )
            form.update { it.copy(isPlacing = false) }
        }
    }
}
