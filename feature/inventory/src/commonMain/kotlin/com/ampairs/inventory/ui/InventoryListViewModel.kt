package com.ampairs.inventory.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.inventory.data.repository.InventoryItemRepository
import com.ampairs.inventory.domain.InventoryItem
import com.ampairs.inventory.domain.InventorySettings
import com.ampairs.inventory.domain.InventorySettingsProvider
import com.ampairs.sync.CentralSyncService
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEvent
import com.ampairs.sync.SyncStatus
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

enum class InventoryFilter { ALL, LOW, OUT, INACTIVE }
enum class InventorySort { NAME, UPDATED, STOCK_ASC, VALUE_DESC }

data class InventoryListUiState(
    val items: List<InventoryItem> = emptyList(),
    val totalItems: Int = 0,
    val lowStockCount: Int = 0,
    val outOfStockCount: Int = 0,
    val totalStockValue: Double = 0.0,
    val filter: InventoryFilter = InventoryFilter.ALL,
    val sort: InventorySort = InventorySort.NAME,
    val searchQuery: String = "",
    val selectedItemId: String? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val lowStockAlerts: Boolean = true,
)

@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class InventoryListViewModel(
    private val itemRepository: InventoryItemRepository,
    private val settingsProvider: InventorySettingsProvider,
    private val syncService: CentralSyncService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryListUiState())
    val uiState: StateFlow<InventoryListUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _filter = MutableStateFlow(InventoryFilter.ALL)
    private val _sort = MutableStateFlow(InventorySort.NAME)

    init {
        observeItems()
        observeDashboard()
        observeSyncState()
        observeSettings()
        syncService.emit(SyncEvent.TriggerPull(SyncEntity.INVENTORY))
        syncService.emit(SyncEvent.TriggerPull(SyncEntity.INVENTORY_TRANSACTION))
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setFilter(filter: InventoryFilter) {
        _filter.value = filter
        _uiState.update { it.copy(filter = filter) }
    }

    fun setSort(sort: InventorySort) {
        _sort.value = sort
        _uiState.update { it.copy(sort = sort) }
    }

    fun selectItem(id: String?) {
        _uiState.update { it.copy(selectedItemId = id) }
    }

    fun refresh() {
        syncService.emit(SyncEvent.TriggerFullSync(SyncEntity.INVENTORY))
        syncService.emit(SyncEvent.TriggerFullSync(SyncEntity.INVENTORY_TRANSACTION))
    }

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private fun observeItems() {
        _uiState.update { it.copy(isLoading = true) }
        combine(
            _searchQuery.debounce(250).distinctUntilChanged(),
            _filter,
            _sort,
        ) { query, filter, sort -> Triple(query, filter, sort) }
            .flatMapLatest { (query, filter, sort) ->
                val source = when (filter) {
                    InventoryFilter.ALL -> itemRepository.searchItems(query)
                    InventoryFilter.LOW -> itemRepository.observeLowStock()
                    InventoryFilter.OUT -> itemRepository.observeOutOfStock()
                    InventoryFilter.INACTIVE -> itemRepository.observeInactiveItems()
                }
                combineSort(source, sort)
            }
            .onEach { items -> _uiState.update { it.copy(items = items, isLoading = false, error = null) } }
            .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
            .launchIn(viewModelScope)
    }

    private fun combineSort(
        source: Flow<List<InventoryItem>>,
        sort: InventorySort,
    ): Flow<List<InventoryItem>> =
        source.map { list ->
            when (sort) {
                InventorySort.NAME -> list.sortedBy { it.name.lowercase() }
                InventorySort.UPDATED -> list.sortedByDescending { it.updatedAt ?: "" }
                InventorySort.STOCK_ASC -> list.sortedBy { it.currentStock }
                InventorySort.VALUE_DESC -> list.sortedByDescending { it.stockValue }
            }
        }

    private fun observeDashboard() {
        combine(
            itemRepository.observeActiveItemCount(),
            itemRepository.observeLowStock(),
            itemRepository.observeOutOfStock(),
            itemRepository.observeTotalStockValue(),
        ) { total, low, out, value ->
            _uiState.update {
                it.copy(totalItems = total, lowStockCount = low.size, outOfStockCount = out.size, totalStockValue = value)
            }
        }.launchIn(viewModelScope)
    }

    private fun observeSyncState() {
        syncService.observeEntity(SyncEntity.INVENTORY)
            .onEach { state -> _uiState.update { it.copy(isRefreshing = state?.status is SyncStatus.Syncing) } }
            .launchIn(viewModelScope)
    }

    private fun observeSettings() {
        settingsProvider.observe()
            .onEach { s: InventorySettings -> _uiState.update { it.copy(lowStockAlerts = s.lowStockAlerts) } }
            .launchIn(viewModelScope)
    }
}
