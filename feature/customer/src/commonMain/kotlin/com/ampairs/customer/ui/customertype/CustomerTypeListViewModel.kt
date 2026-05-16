package com.ampairs.customer.ui.customertype

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.customer.data.repository.CustomerTypeRepository
import com.ampairs.customer.domain.CustomerType
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.mobilenativefoundation.store.store5.StoreReadResponse
import kotlinx.coroutines.FlowPreview

data class CustomerTypeListUiState(
    val customerTypes: List<CustomerType> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val availableCustomerTypesForImport: List<CustomerType> = emptyList(),
    val isLoadingImportCustomerTypes: Boolean = false
)

class CustomerTypeListViewModel(
    private val customerTypeRepository: CustomerTypeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomerTypeListUiState())
    val uiState: StateFlow<CustomerTypeListUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        refreshCustomerTypes()
        observeSearchQuery()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun deleteCustomerType(customerTypeId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }

            val result = customerTypeRepository.deleteCustomerType(customerTypeId)
            if (result.isFailure) {
                _uiState.update {
                    it.copy(error = result.exceptionOrNull()?.message ?: "Failed to delete customer type")
                }
            } else {
                // Refresh the list after successful deletion
                refreshCustomerTypes()
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
                    searchCustomerTypes(query)
                } else {
                    refreshCustomerTypes()
                }
            }
            .launchIn(viewModelScope)
    }

    private fun refreshCustomerTypes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            customerTypeRepository.getCustomerTypesFlow(page = 0, size = 100, forceRefresh = true)
                .collect { response ->
                    when (response) {
                        is StoreReadResponse.Data -> {
                            _uiState.update {
                                it.copy(
                                    customerTypes = response.value,
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
                                    error = response.error.message ?: "Failed to load customer types"
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

    private fun searchCustomerTypes(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            customerTypeRepository.searchCustomerTypes(query)
                .collect { customerTypes ->
                    _uiState.update {
                        it.copy(
                            customerTypes = customerTypes,
                            isLoading = false,
                            error = null
                        )
                    }
                }
        }
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
            if (result.isFailure) {
                _uiState.update {
                    it.copy(error = result.exceptionOrNull()?.message ?: "Failed to import customer type")
                }
            } else {
                refreshCustomerTypes()
                loadAvailableCustomerTypesForImport() // Refresh available list
            }
        }
    }

    fun bulkImportCustomerTypes(customerTypes: List<CustomerType>) {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }

            val result = customerTypeRepository.bulkImportCustomerTypes(customerTypes)
            if (result.isFailure) {
                _uiState.update {
                    it.copy(error = result.exceptionOrNull()?.message ?: "Failed to import customer types")
                }
            } else {
                refreshCustomerTypes()
                loadAvailableCustomerTypesForImport() // Refresh available list
            }
        }
    }

    /**
     * Load customer types from local database (reactive)
     */
    fun loadCustomerTypes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                customerTypeRepository.getAllCustomerTypesFlow()
                    .catch { throwable ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = throwable.message ?: "Unknown error"
                            )
                        }
                    }
                    .collect { types ->
                        _uiState.update {
                            it.copy(
                                customerTypes = types,
                                isLoading = false,
                                error = null
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load customer types"
                    )
                }
            }
        }
    }

    /**
     * Sync customer types with server in background
     */
    fun syncCustomerTypes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }

            try {
                val result = customerTypeRepository.syncCustomerTypes()
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