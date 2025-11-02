package com.ampairs.customer.ui.customertype

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.id_generator.UidGenerator
import com.ampairs.customer.data.repository.CustomerTypeRepository
import com.ampairs.customer.domain.CustomerType
import com.ampairs.customer.util.CustomerConstants
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CustomerTypeFormState(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val typeCode: String = "",
    val displayOrder: String = "",
    val defaultCreditLimit: String = "",
    val defaultCreditDays: String = "",
    val metadata: String = "",
    val active: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isEditMode: Boolean = false
)

class CustomerTypeFormViewModel(
    private val customerTypeRepository: CustomerTypeRepository,
    private val customerTypeId: String? = null
) : ViewModel() {

    private val _formState = MutableStateFlow(CustomerTypeFormState())
    val formState: StateFlow<CustomerTypeFormState> = _formState.asStateFlow()

    init {
        if (!customerTypeId.isNullOrBlank()) {
            _formState.update { it.copy(isEditMode = true, id = customerTypeId) }
            loadCustomerType(customerTypeId)
        }
    }

    fun updateName(name: String) {
        _formState.update { it.copy(name = name, error = null) }
    }

    fun updateDescription(description: String) {
        _formState.update { it.copy(description = description) }
    }

    fun updateTypeCode(typeCode: String) {
        _formState.update { it.copy(typeCode = typeCode) }
    }

    fun updateDisplayOrder(displayOrder: String) {
        // Validate numeric input
        if (displayOrder.isEmpty() || displayOrder.toIntOrNull() != null) {
            _formState.update { it.copy(displayOrder = displayOrder) }
        }
    }

    fun updateDefaultCreditLimit(defaultCreditLimit: String) {
        // Validate numeric input
        if (defaultCreditLimit.isEmpty() || defaultCreditLimit.toDoubleOrNull() != null) {
            _formState.update { it.copy(defaultCreditLimit = defaultCreditLimit) }
        }
    }

    fun updateDefaultCreditDays(defaultCreditDays: String) {
        // Validate numeric input
        if (defaultCreditDays.isEmpty() || defaultCreditDays.toIntOrNull() != null) {
            _formState.update { it.copy(defaultCreditDays = defaultCreditDays) }
        }
    }

    fun updateMetadata(metadata: String) {
        _formState.update { it.copy(metadata = metadata) }
    }

    fun updateActive(active: Boolean) {
        _formState.update { it.copy(active = active) }
    }

    fun saveCustomerType(onSuccess: () -> Unit) {
        val state = _formState.value

        // Validate input
        if (state.name.isBlank()) {
            _formState.update { it.copy(error = "Customer type name is required") }
            return
        }

        // Validate numeric fields
        val displayOrder = if (state.displayOrder.isNotBlank()) {
            state.displayOrder.toIntOrNull()
        } else null

        val defaultCreditLimit = if (state.defaultCreditLimit.isNotBlank()) {
            val limit = state.defaultCreditLimit.toDoubleOrNull()
            if (limit == null || limit < 0) {
                _formState.update { it.copy(error = "Default credit limit must be a positive number") }
                return
            }
            limit
        } else null

        val defaultCreditDays = if (state.defaultCreditDays.isNotBlank()) {
            val days = state.defaultCreditDays.toIntOrNull()
            if (days == null || days < 0) {
                _formState.update { it.copy(error = "Default credit days must be a positive number") }
                return
            }
            days
        } else null

        _formState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                val customerType = CustomerType(
                    uid = if (state.isEditMode) state.id else UidGenerator.generateUid(CustomerConstants.UID_PREFIX),
                    name = state.name.trim(),
                    description = state.description.trim().ifBlank { null },
                    typeCode = state.typeCode.trim().ifBlank { null },
                    displayOrder = displayOrder,
                    defaultCreditLimit = defaultCreditLimit,
                    defaultCreditDays = defaultCreditDays,
                    metadata = state.metadata.trim().ifBlank { null },
                    active = state.active
                )

                val result = if (state.isEditMode) {
                    customerTypeRepository.updateCustomerType(customerType)
                } else {
                    customerTypeRepository.createCustomerType(customerType)
                }

                if (result.isSuccess) {
                    _formState.update { it.copy(isLoading = false) }
                    onSuccess()
                } else {
                    _formState.update {
                        it.copy(
                            isLoading = false,
                            error = result.exceptionOrNull()?.message ?: "Failed to save customer type"
                        )
                    }
                }
            } catch (e: Exception) {
                _formState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "An error occurred while saving"
                    )
                }
            }
        }
    }

    private fun loadCustomerType(customerTypeId: String) {
        viewModelScope.launch {
            _formState.update { it.copy(isLoading = true) }

            // Load from local database first
            try {
                val customerType = customerTypeRepository.getCustomerTypeById(customerTypeId)
                if (customerType != null) {
                    _formState.update {
                        it.copy(
                            id = customerType.uid,
                            name = customerType.name,
                            description = customerType.description ?: "",
                            typeCode = customerType.typeCode ?: "",
                            displayOrder = customerType.displayOrder?.toString() ?: "",
                            defaultCreditLimit = customerType.defaultCreditLimit?.toString() ?: "",
                            defaultCreditDays = customerType.defaultCreditDays?.toString() ?: "",
                            metadata = customerType.metadata ?: "",
                            active = customerType.active,
                            isLoading = false
                        )
                    }
                } else {
                    _formState.update {
                        it.copy(
                            isLoading = false,
                            error = "Customer type not found"
                        )
                    }
                }
            } catch (e: Exception) {
                _formState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load customer type"
                    )
                }
            }
        }
    }

    fun clearError() {
        _formState.update { it.copy(error = null) }
    }
}