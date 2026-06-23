package com.ampairs.inventory.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.inventory.data.repository.InventoryItemRepository
import com.ampairs.inventory.data.repository.InventoryTransactionRepository
import com.ampairs.inventory.domain.InventoryItem
import com.ampairs.inventory.domain.InventoryMovement
import com.ampairs.sync.CentralSyncService
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEvent
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InventoryDetailUiState(
    val item: InventoryItem? = null,
    val movements: List<InventoryMovement> = emptyList(),
    val isLoading: Boolean = true,
    val showRefreshBanner: Boolean = false,
    val error: String? = null,
)

@AssistedInject
class InventoryDetailViewModel(
    @Assisted val itemId: String,
    private val itemRepository: InventoryItemRepository,
    private val transactionRepository: InventoryTransactionRepository,
    private val syncService: CentralSyncService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryDetailUiState())
    val uiState: StateFlow<InventoryDetailUiState> = _uiState.asStateFlow()

    init {
        if (itemId.isNotBlank()) observe()
    }

    private fun observe() {
        combine(
            itemRepository.observeItem(itemId),
            transactionRepository.observeMovements(itemId),
        ) { live, movements -> live to movements }
            .onEach { (live, movements) ->
                _uiState.update { st ->
                    // First load adopts the live value; later, if on-hand drifts (e.g. a sale's
                    // auto-deduct on another device), keep the displayed snapshot and surface a
                    // non-destructive Refresh banner.
                    val displayed = st.item ?: live
                    val drifted = displayed != null && live != null && live.currentStock != displayed.currentStock
                    st.copy(
                        item = displayed,
                        movements = movements,
                        isLoading = false,
                        showRefreshBanner = st.showRefreshBanner || drifted,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    /** Adopt the latest on-hand and re-pull from the server. */
    fun refresh() {
        viewModelScope.launch {
            val latest = itemRepository.getItem(itemId)
            _uiState.update { it.copy(item = latest ?: it.item, showRefreshBanner = false) }
            syncService.emit(SyncEvent.TriggerPull(SyncEntity.INVENTORY))
            syncService.emit(SyncEvent.TriggerPull(SyncEntity.INVENTORY_TRANSACTION))
        }
    }

    fun deactivate() {
        viewModelScope.launch {
            val result = itemRepository.deleteItem(itemId)
            if (result.isFailure) {
                _uiState.update { it.copy(error = result.exceptionOrNull()?.message) }
            }
        }
    }

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(WorkspaceScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(itemId: String): InventoryDetailViewModel
    }
}
