package com.ampairs.tax.ui.calculator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.tax.domain.BusinessType
import com.ampairs.tax.domain.IndianStates
import com.ampairs.tax.domain.TaxCalculationEngine
import com.ampairs.tax.domain.TaxCalculationRequest
import com.ampairs.tax.domain.TaxCalculationResult
import com.ampairs.tax.domain.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TaxCalculatorViewModel(
    private val taxCalculationEngine: TaxCalculationEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaxCalculatorUiState())
    val uiState: StateFlow<TaxCalculatorUiState> = _uiState.asStateFlow()

    fun updateForm(newFormState: TaxCalculationFormState) {
        val validatedFormState = validateForm(newFormState)
        _uiState.value = _uiState.value.copy(
            formState = validatedFormState,
            error = null
        )
    }

    fun calculateTax() {
        val currentState = _uiState.value
        if (!currentState.formState.isValid) return

        _uiState.value = currentState.copy(isCalculating = true, error = null)

        viewModelScope.launch {
            try {
                val request = TaxCalculationRequest(
                    hsnCode = currentState.formState.hsnCode,
                    baseAmount = currentState.formState.baseAmount.toDoubleOrNull() ?: 0.0,
                    quantity = currentState.formState.quantity.toIntOrNull() ?: 1,
                    sourceState = currentState.formState.sourceState,
                    destinationState = currentState.formState.destinationState,
                    businessType = currentState.formState.businessType,
                    transactionType = currentState.formState.transactionType
                )

                val result = taxCalculationEngine.calculateTax(request)

                if (result.isSuccess) {
                    _uiState.value = currentState.copy(
                        isCalculating = false,
                        calculationResult = result.getOrThrow(),
                        error = null
                    )
                } else {
                    _uiState.value = currentState.copy(
                        isCalculating = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to calculate tax"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = currentState.copy(
                    isCalculating = false,
                    error = e.message ?: "Failed to calculate tax"
                )
            }
        }
    }

    fun clearResult() {
        _uiState.value = _uiState.value.copy(
            calculationResult = null,
            error = null
        )
    }

    private fun validateForm(formState: TaxCalculationFormState): TaxCalculationFormState {
        val hsnCodeError = when {
            formState.hsnCode.isBlank() -> "HSN code is required"
            !formState.hsnCode.matches(Regex("^\\d{4,8}$")) -> "HSN code must be 4-8 digits"
            else -> null
        }

        val baseAmountError = when {
            formState.baseAmount.isBlank() -> "Base amount is required"
            formState.baseAmount.toDoubleOrNull() == null -> "Invalid amount format"
            (formState.baseAmount.toDoubleOrNull() ?: 0.0) <= 0 -> "Amount must be greater than 0"
            else -> null
        }

        val sourceStateError = when {
            formState.sourceState.isBlank() -> "Source state is required"
            !IndianStates.isValidStateCode(formState.sourceState) -> "Invalid state code"
            else -> null
        }

        val destinationStateError = when {
            formState.destinationState.isBlank() -> "Destination state is required"
            !IndianStates.isValidStateCode(formState.destinationState) -> "Invalid state code"
            else -> null
        }

        return formState.copy(
            hsnCodeError = hsnCodeError,
            baseAmountError = baseAmountError,
            sourceStateError = sourceStateError,
            destinationStateError = destinationStateError
        )
    }
}

data class TaxCalculatorUiState(
    val formState: TaxCalculationFormState = TaxCalculationFormState(),
    val isCalculating: Boolean = false,
    val calculationResult: TaxCalculationResult? = null,
    val error: String? = null
)

data class TaxCalculationFormState(
    val hsnCode: String = "",
    val baseAmount: String = "",
    val quantity: String = "1",
    val sourceState: String = "",
    val destinationState: String = "",
    val businessType: BusinessType = BusinessType.REGULAR,
    val transactionType: TransactionType = TransactionType.B2B,
    val hsnCodeError: String? = null,
    val baseAmountError: String? = null,
    val sourceStateError: String? = null,
    val destinationStateError: String? = null
) {
    val isValid: Boolean
        get() = hsnCode.isNotBlank() &&
                baseAmount.isNotBlank() &&
                sourceState.isNotBlank() &&
                destinationState.isNotBlank() &&
                hsnCodeError == null &&
                baseAmountError == null &&
                sourceStateError == null &&
                destinationStateError == null
}