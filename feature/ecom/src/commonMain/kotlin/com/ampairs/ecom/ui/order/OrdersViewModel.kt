package com.ampairs.ecom.ui.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.ecom.data.db.entity.EcomOrderEntity
import com.ampairs.ecom.data.repository.BuyerInvoiceRepository
import com.ampairs.ecom.data.repository.EcomOrderRepository
import com.ampairs.ecom.domain.EcomSession
import com.ampairs.sync.CentralSyncService
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEvent
import com.ampairs.sync.SyncStatus
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class OrdersUiState(
    val orders: List<EcomOrderEntity> = emptyList(),
    val isRefreshing: Boolean = false,
    /** Spec 029 — first invoice raised for each order, keyed by ecom order ref; drives the
     * per-order "view invoice" action in the list. Empty when the buyer isn't linked / none yet. */
    val invoiceByOrderRef: Map<String, String> = emptyMap(),
)

@Inject
@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
class OrdersViewModel(
    private val orderRepository: EcomOrderRepository,
    private val invoiceRepository: BuyerInvoiceRepository,
    private val session: EcomSession,
    private val syncService: CentralSyncService,
) : ViewModel() {

    // orderRef → invoiceUid, from one buyer-invoice read (not per-order). Empty when unlinked.
    private val invoiceByOrderRef = MutableStateFlow<Map<String, String>>(emptyMap())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<OrdersUiState> = session.active.flatMapLatest { active ->
        if (active == null) flowOf(OrdersUiState())
        else combine(
            orderRepository.observeOrders(active.storefrontId),
            syncService.observeEntity(SyncEntity.ECOM_ORDER),
            invoiceByOrderRef,
        ) { orders, syncState, invoices ->
            OrdersUiState(
                orders = orders,
                isRefreshing = syncState?.status is SyncStatus.Syncing,
                invoiceByOrderRef = invoices,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OrdersUiState())

    init {
        syncService.emit(SyncEvent.TriggerPull(SyncEntity.ECOM_ORDER))
        loadInvoices()
    }

    fun refresh() {
        syncService.emit(SyncEvent.TriggerPull(SyncEntity.ECOM_ORDER))
        loadInvoices()
    }

    private fun loadInvoices() {
        viewModelScope.launch {
            invoiceRepository.getInvoices(page = 0, size = 100).onSuccess { page ->
                // orderRef → newest invoice uid. The feed is newest-first, so associateBy (which keeps
                // the LAST value per key) is fed the reversed list to leave the newest as the winner.
                invoiceByOrderRef.value = page.content
                    .mapNotNull { inv -> inv.orderRef?.takeIf { it.isNotBlank() }?.let { ref -> ref to inv.invoiceUid } }
                    .asReversed()
                    .toMap()
            }
        }
    }
}
