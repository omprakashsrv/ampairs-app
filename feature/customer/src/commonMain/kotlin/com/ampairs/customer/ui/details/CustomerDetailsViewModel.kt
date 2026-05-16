package com.ampairs.customer.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.customer.domain.Customer
import com.ampairs.customer.domain.CustomerStore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CustomerDetailsUiState(
    val customer: Customer? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class CustomerDetailsViewModel(
    private val customerId: String,
    private val customerStore: CustomerStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomerDetailsUiState())
    val uiState: StateFlow<CustomerDetailsUiState> = _uiState.asStateFlow()

    fun loadCustomer() {

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                customerStore.observeCustomer(customerId)
                    .catch { throwable ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = throwable.message ?: "Unknown error"
                            )
                        }
                    }
                    .collect { customer ->
                        _uiState.update {
                            it.copy(
                                customer = customer,
                                isLoading = false,
                                error = if (customer == null) "Customer not found" else null
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load customer"
                    )
                }
            }
        }
    }

    fun deleteCustomer(onSuccess: () -> Unit) {

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val result = customerStore.deleteCustomer(customerId)
                if (result.isSuccess) {
                    onSuccess()
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.exceptionOrNull()?.message ?: "Failed to delete customer"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to delete customer"
                    )
                }
            }
        }
    }
}