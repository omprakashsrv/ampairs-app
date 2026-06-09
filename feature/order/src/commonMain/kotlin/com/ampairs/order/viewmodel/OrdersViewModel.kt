package com.ampairs.order.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.map
import com.ampairs.common.model.UiState
import com.ampairs.product.domain.Constants.Companion.PAGE_SIZE
import com.ampairs.order.db.OrderRepository
import com.ampairs.order.db.dto.asDomainModel
import kotlinx.coroutines.flow.map
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.sync.CentralSyncService
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEvent
import com.ampairs.sync.SyncStatus
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class OrdersViewModel(
    val orderRepository: OrderRepository,
    private val syncService: CentralSyncService,
) : ViewModel() {

    var searchText by mutableStateOf("")
    val ordersState = mutableStateOf<UiState<Boolean>>(UiState.Empty)

    init {
        syncService.observeEntity(SyncEntity.ORDER)
            .onEach { state ->
                val status = state?.status
                ordersState.value = when (status) {
                    is SyncStatus.Syncing -> UiState.Loading(false)
                    is SyncStatus.Failed -> UiState.Error(status.reason)
                    else -> UiState.Success(true)
                }
            }
            .launchIn(viewModelScope)
        syncService.emit(SyncEvent.TriggerPull(SyncEntity.ORDER))
    }

    val orders = Pager(config = PagingConfig(
        pageSize = PAGE_SIZE,
        prefetchDistance = 10,
        initialLoadSize = PAGE_SIZE,
    ), pagingSourceFactory = {
        orderRepository.getOrders(searchText)
    }).flow.map { pagingData -> pagingData.map { it.asDomainModel() } }
        .cachedIn(viewModelScope)
}
