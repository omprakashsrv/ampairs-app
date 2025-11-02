package com.ampairs.customer.ui.customergroup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.id_generator.UidGenerator
import com.ampairs.customer.data.repository.CustomerGroupRepository
import com.ampairs.customer.domain.CustomerGroup
import com.ampairs.customer.util.CustomerConstants
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CustomerGroupFormState(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val groupCode: String = "",
    val displayOrder: String = "",
    val defaultDiscountPercentage: String = "",
    val priorityLevel: String = "",
    val metadata: String = "",
    val active: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isEditMode: Boolean = false
)

class CustomerGroupFormViewModel(
    private val customerGroupRepository: CustomerGroupRepository,
    private val customerGroupId: String? = null
) : ViewModel() {

    private val _formState = MutableStateFlow(CustomerGroupFormState())
    val formState: StateFlow<CustomerGroupFormState> = _formState.asStateFlow()

    init {
        if (!customerGroupId.isNullOrBlank()) {
            _formState.update { it.copy(isEditMode = true, id = customerGroupId) }
            loadCustomerGroup(customerGroupId)
        }
    }

    fun updateName(name: String) {
        _formState.update { it.copy(name = name, error = null) }
    }

    fun updateDescription(description: String) {
        _formState.update { it.copy(description = description) }
    }

    fun updateGroupCode(groupCode: String) {
        _formState.update { it.copy(groupCode = groupCode) }
    }

    fun updateDisplayOrder(displayOrder: String) {
        // Validate numeric input
        if (displayOrder.isEmpty() || displayOrder.toIntOrNull() != null) {
            _formState.update { it.copy(displayOrder = displayOrder) }
        }
    }

    fun updateDefaultDiscountPercentage(defaultDiscountPercentage: String) {
        // Validate numeric input
        if (defaultDiscountPercentage.isEmpty() || defaultDiscountPercentage.toDoubleOrNull() != null) {
            _formState.update { it.copy(defaultDiscountPercentage = defaultDiscountPercentage) }
        }
    }

    fun updatePriorityLevel(priorityLevel: String) {
        // Validate numeric input
        if (priorityLevel.isEmpty() || priorityLevel.toIntOrNull() != null) {
            _formState.update { it.copy(priorityLevel = priorityLevel) }
        }
    }

    fun updateMetadata(metadata: String) {
        _formState.update { it.copy(metadata = metadata) }
    }

    fun updateActive(active: Boolean) {
        _formState.update { it.copy(active = active) }
    }

    fun saveCustomerGroup(onSuccess: () -> Unit) {
        val state = _formState.value

        // Validate input
        if (state.name.isBlank()) {
            _formState.update { it.copy(error = "Customer group name is required") }
            return
        }

        // Validate numeric fields
        val displayOrder = if (state.displayOrder.isNotBlank()) {
            state.displayOrder.toIntOrNull()
        } else null

        val defaultDiscountPercentage = if (state.defaultDiscountPercentage.isNotBlank()) {
            val percentage = state.defaultDiscountPercentage.toDoubleOrNull()
            if (percentage == null || percentage < 0 || percentage > 100) {
                _formState.update { it.copy(error = "Default discount percentage must be between 0 and 100") }
                return
            }
            percentage
        } else null

        val priorityLevel = if (state.priorityLevel.isNotBlank()) {
            state.priorityLevel.toIntOrNull()
        } else null

        _formState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                val customerGroup = CustomerGroup(
                    uid = if (state.isEditMode) state.id else UidGenerator.generateUid(CustomerConstants.UID_PREFIX),
                    name = state.name.trim(),
                    description = state.description.trim().ifBlank { null },
                    groupCode = state.groupCode.trim().ifBlank { null },
                    displayOrder = displayOrder,
                    defaultDiscountPercentage = defaultDiscountPercentage,
                    priorityLevel = priorityLevel,
                    metadata = state.metadata.trim().ifBlank { null },
                    active = state.active
                )

                val result = if (state.isEditMode) {
                    customerGroupRepository.updateCustomerGroup(customerGroup)
                } else {
                    customerGroupRepository.createCustomerGroup(customerGroup)
                }

                if (result.isSuccess) {
                    _formState.update { it.copy(isLoading = false) }
                    onSuccess()
                } else {
                    _formState.update {
                        it.copy(
                            isLoading = false,
                            error = result.exceptionOrNull()?.message ?: "Failed to save customer group"
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

    private fun loadCustomerGroup(customerGroupId: String) {
        viewModelScope.launch {
            _formState.update { it.copy(isLoading = true) }

            // Load from local database first
            try {
                val customerGroup = customerGroupRepository.getCustomerGroupById(customerGroupId)
                if (customerGroup != null) {
                    _formState.update {
                        it.copy(
                            id = customerGroup.uid,
                            name = customerGroup.name,
                            description = customerGroup.description ?: "",
                            groupCode = customerGroup.groupCode ?: "",
                            displayOrder = customerGroup.displayOrder?.toString() ?: "",
                            defaultDiscountPercentage = customerGroup.defaultDiscountPercentage?.toString() ?: "",
                            priorityLevel = customerGroup.priorityLevel?.toString() ?: "",
                            metadata = customerGroup.metadata ?: "",
                            active = customerGroup.active,
                            isLoading = false
                        )
                    }
                } else {
                    _formState.update {
                        it.copy(
                            isLoading = false,
                            error = "Customer group not found"
                        )
                    }
                }
            } catch (e: Exception) {
                _formState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load customer group"
                    )
                }
            }
        }
    }

    fun clearError() {
        _formState.update { it.copy(error = null) }
    }
}