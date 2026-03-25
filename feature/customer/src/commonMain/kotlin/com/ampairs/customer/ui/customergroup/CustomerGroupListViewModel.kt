package com.ampairs.customer.ui.customergroup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.customer.data.repository.CustomerGroupRepository
import com.ampairs.customer.domain.CustomerGroup
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.mobilenativefoundation.store.store5.StoreReadResponse
import kotlinx.coroutines.FlowPreview

data class CustomerGroupListUiState(
    val customerGroups: List<CustomerGroup> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val availableCustomerGroupsForImport: List<CustomerGroup> = emptyList(),
    val isLoadingImportCustomerGroups: Boolean = false
)

class CustomerGroupListViewModel(
    private val customerGroupRepository: CustomerGroupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomerGroupListUiState())
    val uiState: StateFlow<CustomerGroupListUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        refreshCustomerGroups()
        observeSearchQuery()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun deleteCustomerGroup(customerGroupId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }

            val result = customerGroupRepository.deleteCustomerGroup(customerGroupId)
            if (result.isFailure) {
                _uiState.update {
                    it.copy(error = result.exceptionOrNull()?.message ?: "Failed to delete customer group")
                }
            } else {
                // Refresh the list after successful deletion
                refreshCustomerGroups()
            }
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeSearchQuery() {
        _searchQuery
            .debounce(300) // Wait 300ms after the user stops typing
            .distinctUntilChanged()
            .onEach { query ->
                if (query.isNotBlank()) {
                    searchCustomerGroups(query)
                } else {
                    refreshCustomerGroups()
                }
            }
            .launchIn(viewModelScope)
    }

    private fun refreshCustomerGroups() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            customerGroupRepository.getCustomerGroupsFlow(page = 0, size = 100, forceRefresh = true)
                .collect { response ->
                    when (response) {
                        is StoreReadResponse.Data -> {
                            _uiState.update {
                                it.copy(
                                    customerGroups = response.value,
                                    isLoading = false,
                                    error = null
                                )
                            }
                        }
                        is StoreReadResponse.Loading -> {
                            _uiState.update { it.copy(isLoading = true) }
                        }
                        is StoreReadResponse.Error.Exception -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = response.error.message ?: "Failed to load customer groups"
                                )
                            }
                        }
                        is StoreReadResponse.Error.Message -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = response.message
                                )
                            }
                        }
                        else -> {
                            // Handle other response types if needed
                        }
                    }
                }
        }
    }

    private fun searchCustomerGroups(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            customerGroupRepository.searchCustomerGroups(query)
                .collect { customerGroups ->
                    _uiState.update {
                        it.copy(
                            customerGroups = customerGroups,
                            isLoading = false,
                            error = null
                        )
                    }
                }
        }
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
            if (result.isFailure) {
                _uiState.update {
                    it.copy(error = result.exceptionOrNull()?.message ?: "Failed to import customer group")
                }
            } else {
                refreshCustomerGroups()
                loadAvailableCustomerGroupsForImport() // Refresh available list
            }
        }
    }

    fun bulkImportCustomerGroups(customerGroups: List<CustomerGroup>) {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }

            val result = customerGroupRepository.bulkImportCustomerGroups(customerGroups)
            if (result.isFailure) {
                _uiState.update {
                    it.copy(error = result.exceptionOrNull()?.message ?: "Failed to import customer groups")
                }
            } else {
                refreshCustomerGroups()
                loadAvailableCustomerGroupsForImport() // Refresh available list
            }
        }
    }

    /**
     * Load customer groups from local database (reactive)
     */
    fun loadCustomerGroups() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                customerGroupRepository.getAllCustomerGroupsFlow()
                    .catch { throwable ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = throwable.message ?: "Unknown error"
                            )
                        }
                    }
                    .collect { groups ->
                        _uiState.update {
                            it.copy(
                                customerGroups = groups,
                                isLoading = false,
                                error = null
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load customer groups"
                    )
                }
            }
        }
    }

    /**
     * Sync customer groups with server in background
     */
    fun syncCustomerGroups() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }

            try {
                val result = customerGroupRepository.syncCustomerGroups()
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        error = if (result.isFailure) {
                            result.exceptionOrNull()?.message ?: "Sync failed"
                        } else null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        error = e.message ?: "Sync failed"
                    )
                }
            }
        }
    }
}