package com.ampairs.business.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.business.data.repository.BusinessRepository
import com.ampairs.business.domain.BusinessOperations
import com.ampairs.business.domain.BusinessOperationsUpdateRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Business Operations Screen.
 * Manages loading, editing, and saving operational settings.
 */
class BusinessOperationsViewModel(
    private val repository: BusinessRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BusinessOperationsUiState())
    val uiState: StateFlow<BusinessOperationsUiState> = _uiState.asStateFlow()

    init {
        loadOperations()
    }

    fun loadOperations() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            repository.fetchBusinessOperations()
                .onSuccess { operations ->
                    _uiState.value = _uiState.value.copy(
                        operations = operations,
                        formState = operations.toFormState(),
                        isLoading = false,
                        error = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load business operations"
                    )
                }
        }
    }

    fun updateFormState(formState: BusinessOperationsFormState) {
        _uiState.value = _uiState.value.copy(formState = formState)
    }

    fun saveOperations() {
        val formState = _uiState.value.formState

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)

            val request = BusinessOperationsUpdateRequest(
                timezone = formState.timezone,
                currency = formState.currency,
                language = formState.language,
                dateFormat = formState.dateFormat,
                timeFormat = formState.timeFormat,
                openingHours = formState.openingHours.takeIf { it.isNotBlank() },
                closingHours = formState.closingHours.takeIf { it.isNotBlank() },
                operatingDays = formState.selectedDays
            )

            repository.updateBusinessOperations(request)
                .onSuccess { updatedOperations ->
                    _uiState.value = _uiState.value.copy(
                        operations = updatedOperations,
                        formState = updatedOperations.toFormState(),
                        isSaving = false,
                        saveSuccess = true,
                        error = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = error.message ?: "Failed to save operations settings"
                    )
                }
        }
    }

    fun clearSaveSuccess() {
        _uiState.value = _uiState.value.copy(saveSuccess = false)
    }

    fun refresh() {
        loadOperations()
    }
}

data class BusinessOperationsUiState(
    val operations: BusinessOperations? = null,
    val formState: BusinessOperationsFormState = BusinessOperationsFormState(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
)

data class BusinessOperationsFormState(
    val timezone: String = "UTC",
    val currency: String = "INR",
    val language: String = "en",
    val dateFormat: String = "DD-MM-YYYY",
    val timeFormat: String = "12H",
    val openingHours: String = "",
    val closingHours: String = "",
    val selectedDays: List<String> = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")
)

private fun BusinessOperations.toFormState() = BusinessOperationsFormState(
    timezone = timezone,
    currency = currency,
    language = language,
    dateFormat = dateFormat,
    timeFormat = timeFormat,
    openingHours = openingHours ?: "",
    closingHours = closingHours ?: "",
    selectedDays = operatingDays
)
