package com.ampairs.tax.ui.configuration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.tax.data.repository.TaxConfigurationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Tax Configuration Screen
 *
 * Manages:
 * - Loading existing configuration or detecting new setup
 * - Form state for all configuration fields
 * - Create/Update operations
 * - Validation
 */
class TaxConfigurationViewModel(
    private val repository: TaxConfigurationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaxConfigurationUiState())
    val uiState: StateFlow<TaxConfigurationUiState> = _uiState.asStateFlow()

    init {
        loadConfiguration()
    }

    fun loadConfiguration() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            repository.getConfiguration()
                .onSuccess { config ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isNewConfiguration = false,
                        formState = TaxConfigurationFormState(
                            countryCode = config.countryCode,
                            taxStrategy = config.taxStrategy,
                            defaultTaxCodeSystem = config.defaultTaxCodeSystem ?: "",
                            industry = config.industry,
                            autoSubscribeNewCodes = config.autoSubscribeNewCodes
                        ),
                        error = null
                    )
                }
                .onFailure { error ->
                    // Configuration not found - this is a new setup
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isNewConfiguration = true,
                        formState = TaxConfigurationFormState(),
                        error = null
                    )
                }
        }
    }

    fun updateFormState(formState: TaxConfigurationFormState) {
        _uiState.value = _uiState.value.copy(formState = formState)
    }

    fun saveConfiguration() {
        val formState = _uiState.value.formState

        if (!formState.isValid()) {
            _uiState.value = _uiState.value.copy(
                error = "Please fill all required fields"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)

            val result = if (_uiState.value.isNewConfiguration) {
                repository.createConfiguration(
                    countryCode = formState.countryCode,
                    taxStrategy = formState.taxStrategy,
                    defaultTaxCodeSystem = formState.defaultTaxCodeSystem,
                    industry = formState.industry,
                    autoSubscribeNewCodes = formState.autoSubscribeNewCodes
                )
            } else {
                repository.updateConfiguration(
                    countryCode = formState.countryCode,
                    taxStrategy = formState.taxStrategy,
                    defaultTaxCodeSystem = formState.defaultTaxCodeSystem,
                    industry = formState.industry,
                    autoSubscribeNewCodes = formState.autoSubscribeNewCodes
                )
            }

            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    saveSuccess = true,
                    isNewConfiguration = false,
                    error = null
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = error.message ?: "Failed to save configuration"
                )
            }
        }
    }

    fun clearSaveSuccess() {
        _uiState.value = _uiState.value.copy(saveSuccess = false)
    }

    fun refresh() {
        loadConfiguration()
    }
}

/**
 * UI State for Tax Configuration Screen
 */
data class TaxConfigurationUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isNewConfiguration: Boolean = true,
    val formState: TaxConfigurationFormState = TaxConfigurationFormState(),
    val saveSuccess: Boolean = false,
    val error: String? = null
)

/**
 * Form State for Tax Configuration
 */
data class TaxConfigurationFormState(
    val countryCode: String = "IN",
    val taxStrategy: String = "INDIA_GST",
    val defaultTaxCodeSystem: String = "HSN_CODE",
    val industry: String? = null,
    val autoSubscribeNewCodes: Boolean = false
) {
    fun isValid(): Boolean {
        return countryCode.isNotBlank() &&
                taxStrategy.isNotBlank() &&
                defaultTaxCodeSystem.isNotBlank()
    }
}
