package com.ampairs.business.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.business.data.repository.BusinessRepository
import com.ampairs.business.domain.TaxConfiguration
import com.ampairs.business.domain.TaxConfigurationUpdateRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Business Tax Configuration Screen.
 * Manages loading, editing, and saving tax settings.
 */
class BusinessTaxConfigViewModel(
    private val repository: BusinessRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BusinessTaxConfigUiState())
    val uiState: StateFlow<BusinessTaxConfigUiState> = _uiState.asStateFlow()

    init {
        loadTaxConfig()
    }

    fun loadTaxConfig() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            repository.fetchTaxConfiguration()
                .onSuccess { taxConfig ->
                    _uiState.value = _uiState.value.copy(
                        taxConfig = taxConfig,
                        formState = taxConfig.toFormState(),
                        isLoading = false,
                        error = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load tax configuration"
                    )
                }
        }
    }

    fun updateFormState(formState: BusinessTaxConfigFormState) {
        _uiState.value = _uiState.value.copy(formState = formState)
    }

    fun saveTaxConfig() {
        val formState = _uiState.value.formState

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)

            val request = TaxConfigurationUpdateRequest(
                taxId = formState.taxId.takeIf { it.isNotBlank() },
                registrationNumber = formState.registrationNumber.takeIf { it.isNotBlank() },
                taxSettings = if (formState.taxSettings.isNotEmpty()) formState.taxSettings else null
            )

            repository.updateTaxConfiguration(request)
                .onSuccess { updatedTaxConfig ->
                    _uiState.value = _uiState.value.copy(
                        taxConfig = updatedTaxConfig,
                        formState = updatedTaxConfig.toFormState(),
                        isSaving = false,
                        saveSuccess = true,
                        error = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = error.message ?: "Failed to save tax configuration"
                    )
                }
        }
    }

    fun clearSaveSuccess() {
        _uiState.value = _uiState.value.copy(saveSuccess = false)
    }

    fun refresh() {
        loadTaxConfig()
    }
}

data class BusinessTaxConfigUiState(
    val taxConfig: TaxConfiguration? = null,
    val formState: BusinessTaxConfigFormState = BusinessTaxConfigFormState(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
)

data class BusinessTaxConfigFormState(
    val taxId: String = "",
    val registrationNumber: String = "",
    val taxSettings: Map<String, String> = emptyMap()
)

private fun TaxConfiguration.toFormState() = BusinessTaxConfigFormState(
    taxId = taxId ?: "",
    registrationNumber = registrationNumber ?: "",
    taxSettings = taxSettings ?: emptyMap()
)
