package com.ampairs.customer.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.customer.domain.Customer
import com.ampairs.customer.domain.CustomerStore
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.form.data.repository.ConfigLookup
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class CustomerImagesConfig(
    val visible: Boolean = true,
    val readOnly: Boolean = false
)

data class CustomerDetailsUiState(
    val customer: Customer? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@AssistedInject
class CustomerDetailsViewModel(
    @Assisted private val customerId: String,
    private val customerStore: CustomerStore,
    private val configRepository: ConfigLookup
) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(WorkspaceScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(customerId: String): CustomerDetailsViewModel
    }

    private val _uiState = MutableStateFlow(CustomerDetailsUiState())
    val uiState: StateFlow<CustomerDetailsUiState> = _uiState.asStateFlow()

    private val _imagesConfig = MutableStateFlow(CustomerImagesConfig())
    val imagesConfig: StateFlow<CustomerImagesConfig> = _imagesConfig.asStateFlow()

    init {
        loadCustomerImagesConfig()
    }

    private fun loadCustomerImagesConfig() {
        viewModelScope.launch {
            val config = configRepository.observeConfigSchema("customer").first()
            val fieldConfig = config?.fieldConfigs?.find { it.fieldName == "customerImages" }
            _imagesConfig.value = CustomerImagesConfig(
                visible = fieldConfig?.visible != false,
                readOnly = fieldConfig?.enabled == false
            )
        }
    }

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