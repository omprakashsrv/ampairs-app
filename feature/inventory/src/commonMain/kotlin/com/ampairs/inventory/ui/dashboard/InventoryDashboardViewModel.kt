package com.ampairs.inventory.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.inventory.data.repository.InventoryItemRepository
import com.ampairs.inventory.data.repository.InventoryTransactionRepository
import com.ampairs.inventory.domain.InventoryMovement
import com.ampairs.sync.CentralSyncService
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEvent
import com.ampairs.sync.SyncStatus
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

enum class DashboardSyncState { SYNCED, SYNCING }

data class InventoryDashboardUiState(
    val totalItems: Int = 0,
    val totalStockValue: Double = 0.0,
    val lowStockCount: Int = 0,
    val outOfStockCount: Int = 0,
    val recentMovements: List<InventoryMovement> = emptyList(),
    val itemNames: Map<String, String> = emptyMap(),
    val lowStockAlerts: Boolean = true,
    val syncState: DashboardSyncState = DashboardSyncState.SYNCED,
    val isLoading: Boolean = true,
)

@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class InventoryDashboardViewModel(
    private val itemRepository: InventoryItemRepository,
    private val transactionRepository: InventoryTransactionRepository,
    private val settingsProvider: com.ampairs.inventory.domain.InventorySettingsProvider,
    private val syncService: CentralSyncService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryDashboardUiState())
    val uiState: StateFlow<InventoryDashboardUiState> = _uiState.asStateFlow()

    init {
        combine(
            itemRepository.observeActiveItemCount(),
            itemRepository.observeLowStock(),
            itemRepository.observeOutOfStock(),
            itemRepository.observeTotalStockValue(),
        ) { total, low, out, value ->
            _uiState.update {
                it.copy(
                    totalItems = total,
                    lowStockCount = low.size,
                    outOfStockCount = out.size,
                    totalStockValue = value,
                    isLoading = false,
                )
            }
        }.launchIn(viewModelScope)

        combine(
            transactionRepository.observeRecentMovements(8),
            itemRepository.observeAllItemsIncludingInactive(),
        ) { movements, items ->
            _uiState.update {
                it.copy(recentMovements = movements, itemNames = items.associate { i -> i.uid to i.name })
            }
        }.launchIn(viewModelScope)

        settingsProvider.observe()
            .onEach { s -> _uiState.update { it.copy(lowStockAlerts = s.lowStockAlerts) } }
            .launchIn(viewModelScope)

        syncService.observeEntity(SyncEntity.INVENTORY)
            .onEach { st ->
                _uiState.update {
                    it.copy(syncState = if (st?.status is SyncStatus.Syncing) DashboardSyncState.SYNCING else DashboardSyncState.SYNCED)
                }
            }
            .launchIn(viewModelScope)

        syncService.emit(SyncEvent.TriggerPull(SyncEntity.INVENTORY))
        syncService.emit(SyncEvent.TriggerPull(SyncEntity.INVENTORY_TRANSACTION))
    }

    fun refresh() {
        syncService.emit(SyncEvent.TriggerFullSync(SyncEntity.INVENTORY))
        syncService.emit(SyncEvent.TriggerFullSync(SyncEntity.INVENTORY_TRANSACTION))
    }
}
