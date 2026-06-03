package com.ampairs.customer.ui.customergroup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.customer.data.repository.CustomerGroupRepository
import com.ampairs.customer.domain.CustomerGroup
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
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CustomerGroupListUiState(
    val customerGroups: List<CustomerGroup> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val availableCustomerGroupsForImport: List<CustomerGroup> = emptyList(),
    val isLoadingImportCustomerGroups: Boolean = false
)

@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class CustomerGroupListViewModel(
    private val customerGroupRepository: CustomerGroupRepository,
    private val syncService: CentralSyncService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomerGroupListUiState())
    val uiState: StateFlow<CustomerGroupListUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    init {
        observeCustomerGroups()
        observeSyncState()
        syncService.emit(SyncEvent.TriggerPull(SyncEntity.CUSTOMER_GROUP))
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun deleteCustomerGroup(customerGroupId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            val result = customerGroupRepository.deleteCustomerGroup(customerGroupId)
            if (result.isSuccess) {
                syncService.markPendingPush(SyncEntity.CUSTOMER_GROUP)
            } else {
                _uiState.update {
                    it.copy(error = result.exceptionOrNull()?.message ?: "Failed to delete customer group")
                }
            }
        }
    }

    fun syncCustomerGroups() {
        syncService.emit(SyncEvent.TriggerFullSync(SyncEntity.CUSTOMER_GROUP))
    }

    fun loadAvailableCustomerGroupsForImport() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingImportCustomerGroups = true) }
            val result = customerGroupRepository.getAvailableCustomerGroupsForImport()
            _uiState.update {
                it.copy(
                    isLoadingImportCustomerGroups = false,
                    availableCustomerGroupsForImport = result.getOrElse { emptyList() }
                )
            }
        }
    }

    fun importCustomerGroup(customerGroup: CustomerGroup) {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            val result = customerGroupRepository.importCustomerGroup(customerGroup)
            if (result.isSuccess) {
                syncService.markPendingPush(SyncEntity.CUSTOMER_GROUP)
                loadAvailableCustomerGroupsForImport()
            } else {
                _uiState.update {
                    it.copy(error = result.exceptionOrNull()?.message ?: "Failed to import customer group")
                }
            }
        }
    }

    fun bulkImportCustomerGroups(customerGroups: List<CustomerGroup>) {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            val result = customerGroupRepository.bulkImportCustomerGroups(customerGroups)
            if (result.isSuccess) {
                syncService.markPendingPush(SyncEntity.CUSTOMER_GROUP)
                loadAvailableCustomerGroupsForImport()
            } else {
                _uiState.update {
                    it.copy(error = result.exceptionOrNull()?.message ?: "Failed to import customer groups")
                }
            }
        }
    }

    private fun observeSyncState() {
        syncService.observeEntity(SyncEntity.CUSTOMER_GROUP)
            .onEach { state -> _uiState.update { it.copy(isRefreshing = state?.status is SyncStatus.Syncing) } }
            .launchIn(viewModelScope)
    }

    @OptIn(FlowPreview::class)
    private fun observeCustomerGroups() {
        _uiState.update { it.copy(isLoading = true) }
        _searchQuery
            .debounce(300)
            .distinctUntilChanged()
            .flatMapLatest { query -> customerGroupRepository.searchCustomerGroups(query) }
            .onEach { groups ->
                _uiState.update { it.copy(customerGroups = groups, isLoading = false, error = null) }
            }
            .catch { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
            .launchIn(viewModelScope)
    }
}
