package com.ampairs.customer.ui.customertype

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.customer.data.repository.CustomerTypeRepository
import com.ampairs.customer.domain.CustomerType
import com.ampairs.common.di.AppScope
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

@ContributesIntoMap(AppScope::class)
@ViewModelKey
@Inject
class CustomerTypeListViewModel(
    private val customerTypeRepository: CustomerTypeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomerTypeListUiState())
    val uiState: StateFlow<CustomerTypeListUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    init {
        observeCustomerTypes()
        syncCustomerTypes()
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
            }
        }
    }

    fun syncCustomerTypes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
            try {
                customerTypeRepository.syncCustomerTypes()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Sync failed") }
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
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
                loadAvailableCustomerTypesForImport()
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
                loadAvailableCustomerTypesForImport()
            }
        }
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
