package com.ampairs.tax.ui.calculator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.tax.calculation.TaxCalculationEngine
import com.ampairs.tax.calculation.model.*
import com.ampairs.business.data.repository.BusinessRepository
import com.ampairs.sync.CentralSyncService
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEvent
import com.ampairs.tax.data.repository.TaxCodeRepository
import com.ampairs.tax.domain.model.TaxCode
import com.ampairs.common.di.WorkspaceScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Tax Calculator ViewModel
 */
@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class TaxCalculatorViewModel(
    private val taxCalculationEngine: TaxCalculationEngine,
    private val taxCodeRepository: TaxCodeRepository,
    private val syncService: CentralSyncService,
    private val businessRepository: BusinessRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaxCalculatorUiState())
    val uiState: StateFlow<TaxCalculatorUiState> = _uiState.asStateFlow()

    // Workspace codes (offline available)
    val workspaceCodes: StateFlow<List<TaxCode>> = taxCodeRepository
        .observeWorkspaceTaxCodes()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        // Set default country from business profile
        viewModelScope.launch {
            val countryCode = businessRepository.getCachedBusiness()?.country ?: "IN"
            _uiState.update {
                it.copy(
                    sourceCountry = countryCode,
                    destinationCountry = countryCode
                )
            }
        }

        // Ensure rules/components are present for calculations — pull via central sync.
        syncService.emit(SyncEvent.TriggerPull(SyncEntity.TAX))
    }

    fun onTaxCodeSelected(taxCode: TaxCode) {
        _uiState.update {
            it.copy(
                selectedTaxCode = taxCode,
                errorMessage = null
            )
        }
    }

    fun onBaseAmountChange(amount: String) {
        _uiState.update {
            it.copy(
                baseAmount = amount,
                errorMessage = null
            )
        }
    }

    fun onQuantityChange(quantity: String) {
        _uiState.update {
            it.copy(
                quantity = quantity,
                errorMessage = null
            )
        }
    }

    fun onTransactionTypeChange(type: TransactionType) {
        _uiState.update {
            it.copy(
                transactionType = type,
                errorMessage = null
            )
        }
    }

    fun onSourceStateChange(state: String) {
        _uiState.update {
            it.copy(
                sourceState = state,
                errorMessage = null
            )
        }
    }

    fun onSourceCountryChange(country: String) {
        _uiState.update {
            it.copy(
                sourceCountry = country,
                errorMessage = null
            )
        }
    }

    fun onDestinationStateChange(state: String) {
        _uiState.update {
            it.copy(
                destinationState = state,
                errorMessage = null
            )
        }
    }

    fun onDestinationCountryChange(country: String) {
        _uiState.update {
            it.copy(
                destinationCountry = country,
                errorMessage = null
            )
        }
    }

    fun calculateTax() {
        val state = _uiState.value

        // Validate inputs
        val taxCode = state.selectedTaxCode
        if (taxCode == null) {
            _uiState.update { it.copy(errorMessage = "Please select a tax code") }
            return
        }

        val baseAmount = state.baseAmount.toDoubleOrNull()
        if (baseAmount == null || baseAmount <= 0) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid base amount") }
            return
        }

        val quantity = state.quantity.toIntOrNull()
        if (quantity == null || quantity <= 0) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid quantity") }
            return
        }

        if (state.sourceState.isBlank() || state.sourceCountry.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter source location") }
            return
        }

        if (state.destinationState.isBlank() || state.destinationCountry.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter destination location") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isCalculating = true, errorMessage = null) }

            // Build calculation request
            val request = TaxCalculationRequest(
                taxCode = taxCode.code,
                taxCodeType = taxCode.codeType,
                baseAmount = baseAmount,
                quantity = quantity,
                sourceLocation = TaxJurisdiction(
                    country = state.sourceCountry,
                    state = state.sourceState
                ),
                destinationLocation = TaxJurisdiction(
                    country = state.destinationCountry,
                    state = state.destinationState
                ),
                transactionType = state.transactionType,
                transactionContext = TransactionContext(
                    businessType = "REGULAR"
                )
            )

            // Calculate tax using engine
            val result = taxCalculationEngine.calculateTax(request)

            result.fold(
                onSuccess = { calculationResult ->
                    _uiState.update {
                        it.copy(
                            isCalculating = false,
                            calculationResult = calculationResult,
                            errorMessage = null
                        )
                    }

                    // Increment usage count for the tax code
                    taxCodeRepository.incrementUsageCount(taxCode.id)
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isCalculating = false,
                            calculationResult = null,
                            errorMessage = error.message ?: "Calculation failed"
                        )
                    }
                }
            )
        }
    }

    fun clearCalculation() {
        _uiState.update {
            TaxCalculatorUiState(
                sourceCountry = it.sourceCountry,
                destinationCountry = it.destinationCountry
            )
        }
    }
}

/**
 * UI State for Tax Calculator
 */
data class TaxCalculatorUiState(
    val selectedTaxCode: TaxCode? = null,
    val baseAmount: String = "",
    val quantity: String = "1",
    val transactionType: TransactionType = TransactionType.B2B,

    // Source location (seller)
    val sourceState: String = "",
    val sourceCountry: String = "IN",

    // Destination location (buyer)
    val destinationState: String = "",
    val destinationCountry: String = "IN",

    val isCalculating: Boolean = false,
    val calculationResult: TaxCalculationResult? = null,
    val errorMessage: String? = null
) {
    val isValid: Boolean
        get() = selectedTaxCode != null &&
            baseAmount.toDoubleOrNull() != null &&
            quantity.toIntOrNull() != null &&
            sourceState.isNotBlank() &&
            sourceCountry.isNotBlank() &&
            destinationState.isNotBlank() &&
            destinationCountry.isNotBlank()
}
