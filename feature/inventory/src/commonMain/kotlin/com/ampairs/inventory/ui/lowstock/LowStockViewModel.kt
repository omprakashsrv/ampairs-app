package com.ampairs.inventory.ui.lowstock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.inventory.data.repository.InventoryItemRepository
import com.ampairs.inventory.domain.InventoryItem
import com.ampairs.inventory.domain.InventorySettingsProvider
import com.ampairs.sync.CentralSyncService
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEvent
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

data class LowStockUiState(
    val items: List<InventoryItem> = emptyList(),
    val alertsEnabled: Boolean = true,
    val isLoading: Boolean = true,
)

@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class LowStockViewModel(
    private val itemRepository: InventoryItemRepository,
    private val settingsProvider: InventorySettingsProvider,
    private val syncService: CentralSyncService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LowStockUiState())
    val uiState: StateFlow<LowStockUiState> = _uiState.asStateFlow()

    init {
        // Out-of-stock first (most urgent), then low — both sorted by how far below reorder they sit.
        combine(
            itemRepository.observeOutOfStock(),
            itemRepository.observeLowStock(),
            settingsProvider.observe(),
        ) { out, low, settings ->
            val ordered = (out.sortedBy { it.currentStock } + low.sortedBy { it.currentStock - it.reorderLevel })
            _uiState.update { it.copy(items = ordered, alertsEnabled = settings.lowStockAlerts, isLoading = false) }
        }.launchIn(viewModelScope)
        syncService.emit(SyncEvent.TriggerPull(SyncEntity.INVENTORY))
    }
}
