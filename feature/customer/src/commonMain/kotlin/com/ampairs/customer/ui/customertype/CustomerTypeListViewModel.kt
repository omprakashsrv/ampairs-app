package com.ampairs.customer.ui.customertype

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.customer.data.repository.CustomerTypeRepository
import com.ampairs.customer.domain.CustomerType
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

data class CustomerTypeListUiState(
    val customerTypes: List<CustomerType> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val availableCustomerTypesForImport: List<CustomerType> = emptyList(),
    val isLoadingImportCustomerTypes: Boolean = false
)

@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class CustomerTypeListViewModel(
    private val customerTypeRepository: CustomerTypeRepository,
    private val syncService: CentralSyncService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomerTypeListUiState())
    val uiState: StateFlow<CustomerTypeListUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    init {
        observeCustomerTypes()
        observeSyncState()
        syncService.emit(SyncEvent.TriggerPull(SyncEntity.CUSTOMER_TYPE))
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun deleteCustomerType(customerTypeId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            val result = customerTypeRepository.deleteCustomerType(customerTypeId)
            if (result.isSuccess) {
                syncService.markPendingPush(SyncEntity.CUSTOMER_TYPE)
            } else {
                _uiState.update {
                    it.copy(error = result.exceptionOrNull()?.message ?: "Failed to delete customer type")
                }
            }
        }
    }

    fun syncCustomerTypes() {
        syncService.emit(SyncEvent.TriggerFullSync(SyncEntity.CUSTOMER_TYPE))
    }

    fun loadAvailableCustomerTypesForImport() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingImportCustomerTypes = true) }
            val result = customerTypeRepository.getAvailableCustomerTypesForImport()
            _uiState.update {
                it.copy(
                    isLoadingImportCustomerTypes = false,
                    availableCustomerTypesForImport = result.getOrElse { emptyList() }
                )
            }
        }
    }

    fun importCustomerType(customerType: CustomerType) {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            val result = customerTypeRepository.importCustomerType(customerType)
            if (result.isSuccess) {
                syncService.markPendingPush(SyncEntity.CUSTOMER_TYPE)
                loadAvailableCustomerTypesForImport()
            } else {
                _uiState.update {
                    it.copy(error = result.exceptionOrNull()?.message ?: "Failed to import customer type")
                }
            }
        }
    }

    fun bulkImportCustomerTypes(customerTypes: List<CustomerType>) {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            val result = customerTypeRepository.bulkImportCustomerTypes(customerTypes)
            if (result.isSuccess) {
                syncService.markPendingPush(SyncEntity.CUSTOMER_TYPE)
                loadAvailableCustomerTypesForImport()
            } else {
                _uiState.update {
                    it.copy(error = result.exceptionOrNull()?.message ?: "Failed to import customer types")
                }
            }
        }
    }

    private fun observeSyncState() {
        syncService.observeEntity(SyncEntity.CUSTOMER_TYPE)
            .onEach { state -> _uiState.update { it.copy(isRefreshing = state?.status is SyncStatus.Syncing) } }
            .launchIn(viewModelScope)
    }

    @OptIn(FlowPreview::class)
    private fun observeCustomerTypes() {
        _uiState.update { it.copy(isLoading = true) }
        _searchQuery
            .debounce(300)
            .distinctUntilChanged()
            .flatMapLatest { query -> customerTypeRepository.searchCustomerTypes(query) }
            .onEach { types ->
                _uiState.update { it.copy(customerTypes = types, isLoading = false, error = null) }
            }
            .catch { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
            .launchIn(viewModelScope)
    }
}
