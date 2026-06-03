package com.ampairs.ecom.ui.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.ecom.data.db.entity.EcomOrderEntity
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

data class OrdersUiState(
    val orders: List<EcomOrderEntity> = emptyList(),
    val isRefreshing: Boolean = false,
)

@Inject
@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
class OrdersViewModel(
    private val orderRepository: EcomOrderRepository,
    private val session: EcomSession,
    private val syncService: CentralSyncService,
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<OrdersUiState> = session.active.flatMapLatest { active ->
        if (active == null) flowOf(OrdersUiState())
        else combine(
            orderRepository.observeOrders(active.storefrontId),
            syncService.observeEntity(SyncEntity.ECOM_ORDER),
        ) { orders, syncState ->
            OrdersUiState(orders = orders, isRefreshing = syncState?.status is SyncStatus.Syncing)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OrdersUiState())

    init {
        syncService.emit(SyncEvent.TriggerPull(SyncEntity.ECOM_ORDER))
    }

    fun refresh() {
        syncService.emit(SyncEvent.TriggerPull(SyncEntity.ECOM_ORDER))
    }
}
