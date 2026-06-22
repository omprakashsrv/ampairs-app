package com.ampairs.inventory.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.id_generator.UidGenerator
import com.ampairs.inventory.data.repository.InventoryItemRepository
import com.ampairs.inventory.data.repository.InventoryTransactionRepository
import com.ampairs.inventory.domain.InventoryItem
import com.ampairs.inventory.domain.InventoryMovement
import com.ampairs.inventory.util.InventoryConstants
import com.ampairs.sync.CentralSyncService
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEvent
import com.ampairs.sync.SyncStatus
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InventoryListUiState(
    val items: List<InventoryItem> = emptyList(),
    val lowStockCount: Int = 0,
    val outOfStockCount: Int = 0,
    val totalStockValue: Double = 0.0,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
)

@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class InventoryListViewModel(
    private val itemRepository: InventoryItemRepository,
    private val transactionRepository: InventoryTransactionRepository,
    private val syncService: CentralSyncService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryListUiState())
    val uiState: StateFlow<InventoryListUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    init {
        observeItems()
        observeDashboard()
        observeSyncState()
        syncService.emit(SyncEvent.TriggerPull(SyncEntity.INVENTORY))
        syncService.emit(SyncEvent.TriggerPull(SyncEntity.INVENTORY_TRANSACTION))
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun refresh() {
        syncService.emit(SyncEvent.TriggerFullSync(SyncEntity.INVENTORY))
        syncService.emit(SyncEvent.TriggerFullSync(SyncEntity.INVENTORY_TRANSACTION))
    }

    /** Quick create/update of an item (caller passes a built item without uid for create). */
    fun saveItem(item: InventoryItem) {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            val withUid = if (item.uid.isBlank()) {
                item.copy(uid = UidGenerator.generateUid(InventoryConstants.ITEM_UID_PREFIX))
            } else item
            val result = itemRepository.saveItem(withUid)
            if (result.isFailure) {
                _uiState.update { it.copy(error = result.exceptionOrNull()?.message ?: "Failed to save item") }
            }
        }
    }

    fun deleteItem(id: String) {
        viewModelScope.launch {
            val result = itemRepository.deleteItem(id)
            if (result.isFailure) {
                _uiState.update { it.copy(error = result.exceptionOrNull()?.message ?: "Failed to delete item") }
            }
        }
    }

    /** Record a manual stock adjustment as an append-only movement. */
    fun adjustStock(item: InventoryItem, quantity: Double, stockIn: Boolean, reason: String) {
        if (quantity <= 0.0) {
            _uiState.update { it.copy(error = "Quantity must be greater than zero") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            val movement = InventoryMovement(
                uid = UidGenerator.generateUid(InventoryConstants.TXN_UID_PREFIX),
                transactionType = if (stockIn) InventoryConstants.TXN_TYPE_STOCK_IN else InventoryConstants.TXN_TYPE_STOCK_OUT,
                transactionReason = reason.ifBlank { if (stockIn) InventoryConstants.REASON_PURCHASE else InventoryConstants.REASON_CORRECTION },
                inventoryItemId = item.uid,
                quantity = quantity,
                sourceType = "MANUAL",
            )
            val result = transactionRepository.recordMovement(movement)
            if (result.isFailure) {
                _uiState.update { it.copy(error = result.exceptionOrNull()?.message ?: "Failed to adjust stock") }
            }
        }
    }

    private fun observeSyncState() {
        syncService.observeEntity(SyncEntity.INVENTORY)
            .onEach { state -> _uiState.update { it.copy(isRefreshing = state?.status is SyncStatus.Syncing) } }
            .launchIn(viewModelScope)
    }

    @OptIn(FlowPreview::class)
    private fun observeItems() {
        _uiState.update { it.copy(isLoading = true) }
        _searchQuery
            .debounce(300)
            .distinctUntilChanged()
            .flatMapLatest { query -> itemRepository.searchItems(query) }
            .onEach { items -> _uiState.update { it.copy(items = items, isLoading = false, error = null) } }
            .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
            .launchIn(viewModelScope)
    }

    private fun observeDashboard() {
        combine(
            itemRepository.observeLowStock(),
            itemRepository.observeOutOfStock(),
            itemRepository.observeTotalStockValue(),
        ) { low, out, value ->
            Triple(low.size, out.size, value)
        }
            .onEach { (low, out, value) ->
                _uiState.update { it.copy(lowStockCount = low, outOfStockCount = out, totalStockValue = value) }
            }
            .launchIn(viewModelScope)
    }
}
