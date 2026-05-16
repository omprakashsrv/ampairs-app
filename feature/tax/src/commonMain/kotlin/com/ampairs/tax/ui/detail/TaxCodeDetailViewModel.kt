package com.ampairs.tax.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.tax.calculation.TaxCalculationEngine
import com.ampairs.tax.calculation.model.*
import com.ampairs.tax.data.repository.TaxCodeRepository
import com.ampairs.tax.data.repository.TaxRuleRepository
import com.ampairs.tax.domain.model.TaxCode
import com.ampairs.tax.domain.model.TaxRule
import com.ampairs.workspace.context.WorkspaceContextManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Tax Code Detail ViewModel
 *
 * Features:
 * - Display full tax code information
 * - Edit notes and custom tax rules
 * - Toggle favorite status
 * - View usage history
 * - Unsubscribe from code
 */
class TaxCodeDetailViewModel(
    private val taxCodeId: String,
    private val taxCodeRepository: TaxCodeRepository,
    private val taxRuleRepository: TaxRuleRepository,
    private val taxCalculationEngine: TaxCalculationEngine,
    private val workspaceContext: WorkspaceContextManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<TaxCodeDetailUiState>(TaxCodeDetailUiState.Loading)
    val uiState: StateFlow<TaxCodeDetailUiState> = _uiState.asStateFlow()

    init {
        loadTaxCodeDetailsAndRules()
    }

    private fun loadTaxCodeDetailsAndRules() {
        viewModelScope.launch {
            _uiState.value = TaxCodeDetailUiState.Loading

            println("🔍 [TaxCodeDetailVM] Loading tax code and rules for ID: $taxCodeId")

            // Observe tax code from database (reactive)
            launch {
                taxCodeRepository.observeWorkspaceTaxCodes().collect { taxCodes ->
                    val taxCode = taxCodes.find { it.id == taxCodeId }
                    if (taxCode != null) {
                        val currentState = _uiState.value
                        val existingRules = if (currentState is TaxCodeDetailUiState.Success) {
                            currentState.taxRules
                        } else {
                            emptyList()
                        }

                        _uiState.value = TaxCodeDetailUiState.Success(
                            taxCode = taxCode,
                            taxRules = existingRules,
                            isLoadingRules = false,
                            isEditing = false,
                            isSaving = false,
                            saveError = null
                        )
                    } else {
                        _uiState.value = TaxCodeDetailUiState.Error("Tax code not found")
                    }
                }
            }

            // Observe tax rules from database (reactive)
            // Note: Backend returns tax_code_id as empty, so we query by tax_code string instead
            launch {
                taxCodeRepository.observeWorkspaceTaxCodes().collect { taxCodes ->
                    val taxCode = taxCodes.find { it.id == taxCodeId }
                    if (taxCode != null) {
                        println("🔍 [TaxCodeDetailVM] Tax code found: ${taxCode.code}, querying rules by code string")
                        taxRuleRepository.observeTaxRulesByCode(taxCode.code).collect { rules ->
                            println("🔄 [TaxCodeDetailVM] Tax rules updated: ${rules.size} rules for code ${taxCode.code}")
                            val currentState = _uiState.value
                            if (currentState is TaxCodeDetailUiState.Success) {
                                _uiState.update {
                                    currentState.copy(
                                        taxRules = rules,
                                        isLoadingRules = false,
                                        saveError = null
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun toggleFavorite() {
        val currentState = _uiState.value
        if (currentState is TaxCodeDetailUiState.Success) {
            viewModelScope.launch {
                val newFavoriteStatus = !currentState.taxCode.isFavorite
                taxCodeRepository.setFavorite(taxCodeId, newFavoriteStatus)
                // State will update automatically via Flow observation
            }
        }
    }

    fun updateNotes(notes: String) {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is TaxCodeDetailUiState.Success) {
                _uiState.update {
                    currentState.copy(
                        taxCode = currentState.taxCode.copy(notes = notes.ifBlank { null })
                    )
                }
            }
        }
    }

    fun saveNotes(notes: String) {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is TaxCodeDetailUiState.Success) {
                _uiState.update { currentState.copy(isSaving = true, saveError = null) }

                // Note: This would require a repository method to update notes
                // For now, we'll just update the local state
                // TODO: Add repository.updateNotes(taxCodeId, notes) method

                _uiState.update {
                    currentState.copy(
                        isSaving = false,
                        isEditing = false,
                        saveError = null
                    )
                }
            }
        }
    }

    fun setEditing(isEditing: Boolean) {
        val currentState = _uiState.value
        if (currentState is TaxCodeDetailUiState.Success) {
            _uiState.update { currentState.copy(isEditing = isEditing) }
        }
    }

    fun unsubscribe(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is TaxCodeDetailUiState.Success) {
                _uiState.update { currentState.copy(isSaving = true, saveError = null) }

                val result = taxCodeRepository.unsubscribeFromTaxCode(taxCodeId)

                result.fold(
                    onSuccess = {
                        onSuccess()
                    },
                    onFailure = { error ->
                        _uiState.update {
                            currentState.copy(
                                isSaving = false,
                                saveError = error.message ?: "Failed to unsubscribe"
                            )
                        }
                    }
                )
            }
        }
    }

    fun clearError() {
        val currentState = _uiState.value
        if (currentState is TaxCodeDetailUiState.Success) {
            _uiState.update { currentState.copy(saveError = null) }
        }
    }

    fun calculateTaxPreview(amount: Double, quantity: Int, sourceState: String, destState: String) {
        val currentState = _uiState.value
        if (currentState is TaxCodeDetailUiState.Success) {
            viewModelScope.launch {
                _uiState.update { currentState.copy(isCalculating = true) }

                // Check if we have tax rules loaded
                if (currentState.taxRules.isEmpty()) {
                    _uiState.update {
                        currentState.copy(
                            isCalculating = false,
                            saveError = "No tax rules available for this code. Please sync tax rules first."
                        )
                    }
                    return@launch
                }

                // Find the appropriate rule for the source jurisdiction
                val applicableRule = currentState.taxRules.find { rule ->
                    rule.jurisdiction == sourceState && rule.isActive
                } ?: currentState.taxRules.firstOrNull { it.isActive }

                if (applicableRule == null) {
                    _uiState.update {
                        currentState.copy(
                            isCalculating = false,
                            saveError = "No active tax rule found for jurisdiction: $sourceState"
                        )
                    }
                    return@launch
                }

                // Use the calculation engine with the loaded rule
                val countryCode = workspaceContext.getCurrentCountryCode() ?: "IN"

                val request = TaxCalculationRequest(
                    taxCode = currentState.taxCode.code,
                    taxCodeType = currentState.taxCode.codeType,
                    baseAmount = amount,
                    quantity = quantity,
                    sourceLocation = TaxJurisdiction(
                        country = countryCode,
                        state = sourceState
                    ),
                    destinationLocation = TaxJurisdiction(
                        country = countryCode,
                        state = destState
                    ),
                    transactionType = TransactionType.B2B,
                    transactionContext = TransactionContext(
                        businessType = "REGULAR"
                    )
                )

                val result = taxCalculationEngine.calculateTax(request)

                result.fold(
                    onSuccess = { calculationResult ->
                        _uiState.update {
                            currentState.copy(
                                isCalculating = false,
                                calculationResult = calculationResult,
                                saveError = null
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            currentState.copy(
                                isCalculating = false,
                                saveError = "Calculation failed: ${error.message}"
                            )
                        }
                    }
                )
            }
        }
    }
}

/**
 * UI State for Tax Code Detail
 */
sealed class TaxCodeDetailUiState {
    data object Loading : TaxCodeDetailUiState()
    data class Error(val message: String) : TaxCodeDetailUiState()
    data class Success(
        val taxCode: TaxCode,
        val taxRules: List<TaxRule> = emptyList(),
        val isLoadingRules: Boolean = false,
        val isEditing: Boolean = false,
        val isSaving: Boolean = false,
        val saveError: String? = null,
        val isCalculating: Boolean = false,
        val calculationResult: TaxCalculationResult? = null
    ) : TaxCodeDetailUiState()
}
