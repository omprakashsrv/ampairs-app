package com.ampairs.customer.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.components.FilterOption
import com.ampairs.customer.domain.CustomerListItem
import com.ampairs.customer.domain.CustomerStore
import com.ampairs.customer.domain.StateStore
import com.ampairs.customer.data.repository.CustomerGroupRepository
import com.ampairs.customer.data.repository.CustomerTypeRepository
import com.ampairs.common.di.WorkspaceScope
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Applied customer-list filter selection. Empty sets mean the dimension is not filtered. */
data class CustomerFilter(
    val states: Set<String> = emptySet(),
    val types: Set<String> = emptySet(),
    val groups: Set<String> = emptySet(),
) {
    val activeCount: Int get() = states.size + types.size + groups.size
}

data class CustomersListUiState(
    val customers: List<CustomerListItem> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val filter: CustomerFilter = CustomerFilter(),
    val stateOptions: List<FilterOption> = emptyList(),
    val typeOptions: List<FilterOption> = emptyList(),
    val groupOptions: List<FilterOption> = emptyList(),
)

@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class CustomersListViewModel(
    private val customerStore: CustomerStore,
    private val customerTypeRepository: CustomerTypeRepository,
    private val customerGroupRepository: CustomerGroupRepository,
    private val stateStore: StateStore,
    private val syncService: CentralSyncService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomersListUiState())
    val uiState: StateFlow<CustomersListUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _filter = MutableStateFlow(CustomerFilter())

    init {
        observeCustomers()
        syncService.observeEntity(SyncEntity.CUSTOMER)
            .onEach { state -> _uiState.update { it.copy(isRefreshing = state?.status is SyncStatus.Syncing) } }
            .launchIn(viewModelScope)
        loadFilterOptions()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    /** Reload available filter options — call when opening the filter sheet so they reflect latest data. */
    fun onFilterOpened() {
        loadFilterOptions()
    }

    fun applyFilter(filter: CustomerFilter) {
        _filter.value = filter
        _uiState.update { it.copy(filter = filter) }
    }

    fun clearFilter() {
        applyFilter(CustomerFilter())
    }

    fun loadCustomers() {
        // Retry hook: re-run a full sync; the reactive flow re-emits once data lands locally.
        syncCustomers()
    }

    fun syncCustomers() {
        syncService.emit(SyncEvent.TriggerFullSync(SyncEntity.CUSTOMER))
    }

    @OptIn(FlowPreview::class)
    private fun observeCustomers() {
        _uiState.update { it.copy(isLoading = true) }
        combine(
            _searchQuery.debounce(300).distinctUntilChanged(),
            _filter,
        ) { query, filter -> query to filter }
            .flatMapLatest { (query, filter) ->
                // FTS-backed, capped search/browse — scales to 100k+ customers per workspace.
                customerStore.searchAndFilter(
                    query = query,
                    states = filter.states.toList(),
                    types = filter.types.toList(),
                    groups = filter.groups.toList(),
                )
            }
            .onEach { customers ->
                _uiState.update { it.copy(customers = customers, isLoading = false, error = null) }
            }
            .catch { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Unknown error") }
            }
            .launchIn(viewModelScope)
    }

    private fun loadFilterOptions() {
        viewModelScope.launch {
            // States: the primary `state` column is free-text and often empty, so the master states
            // list (StateStore) is the source of options, unioned with any value actually present on
            // customers. Filter matches by name, so value == label == state name.
            val masterStates = runCatching { stateStore.observeStates().first().map { it.name } }.getOrDefault(emptyList())
            val customerStates = runCatching { customerStore.getDistinctStates() }.getOrDefault(emptyList())
            val stateValues = (masterStates + customerStates).filter { it.isNotBlank() }.distinct().sortedBy { it.lowercase() }

            // Types/groups are stored as identifiers; map to names, falling back to the raw value.
            val typeValues = runCatching { customerStore.getDistinctCustomerTypes() }.getOrDefault(emptyList())
            val groupValues = runCatching { customerStore.getDistinctCustomerGroups() }.getOrDefault(emptyList())
            val typeNames = runCatching { customerTypeRepository.observeCustomerTypes().first() }.getOrDefault(emptyList())
                .associate { it.uid to it.name }
            val groupNames = runCatching { customerGroupRepository.observeCustomerGroups().first() }.getOrDefault(emptyList())
                .associate { it.uid to it.name }

            _uiState.update {
                it.copy(
                    stateOptions = stateValues.map { v -> FilterOption(v, v) },
                    typeOptions = typeValues.map { v -> FilterOption(v, typeNames[v] ?: v) },
                    groupOptions = groupValues.map { v -> FilterOption(v, groupNames[v] ?: v) },
                )
            }
        }
    }
}
