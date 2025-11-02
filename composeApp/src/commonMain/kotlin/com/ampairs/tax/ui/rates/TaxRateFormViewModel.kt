package com.ampairs.tax.ui.rates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.tax.domain.BusinessType
import com.ampairs.tax.domain.TaxRate
import com.ampairs.tax.domain.TaxStore
import com.ampairs.tax.domain.TaxType
import com.ampairs.tax.domain.TaxRateByIdKey
import org.mobilenativefoundation.store.store5.StoreReadRequest
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class, kotlin.time.ExperimentalTime::class)
class TaxRateFormViewModel(
    private val taxRateId: String?,
    private val taxStore: TaxStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaxRateFormUiState())
    val uiState: StateFlow<TaxRateFormUiState> = _uiState.asStateFlow()

    init {
        if (taxRateId != null) {
            loadTaxRate(taxRateId)
        } else {
            // Set default effective date to today
            val today = Clock.System.now().toEpochMilliseconds()
            _uiState.value = _uiState.value.copy(effectiveFrom = today)
        }
    }

    private fun loadTaxRate(id: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                taxStore.taxRateByIdStore
                    .stream(StoreReadRequest.cached(
                        key = TaxRateByIdKey(id),
                        refresh = false
                    ))
                    .collect { response ->
                        when (response) {
                            is org.mobilenativefoundation.store.store5.StoreReadResponse.Data -> {
                                val taxRate = response.value

                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    hsnCode = taxRate.hsnCode,
                                    taxType = taxRate.taxType,
                                    ratePercentage = taxRate.ratePercentage,
                                    ratePercentageText = taxRate.ratePercentage.toString(),
                                    cessRate = taxRate.cessRate,
                                    cessRateText = taxRate.cessRate?.toString() ?: "",
                                    cessAmountPerUnit = taxRate.cessAmountPerUnit,
                                    cessAmountPerUnitText = taxRate.cessAmountPerUnit?.toString() ?: "",
                                    effectiveFrom = taxRate.effectiveFrom,
                                    effectiveTo = taxRate.effectiveTo,
                                    geographicalZone = taxRate.geographicalZone,
                                    businessType = taxRate.businessType,
                                    isActive = taxRate.isActive,
                                    originalTaxRate = taxRate,
                                    error = null
                                )
                            }
                            is org.mobilenativefoundation.store.store5.StoreReadResponse.Error.Exception -> {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    error = "Failed to load tax rate: ${response.error.message}"
                                )
                            }
                            is org.mobilenativefoundation.store.store5.StoreReadResponse.Error.Message -> {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    error = "Failed to load tax rate: ${response.message}"
                                )
                            }
                            else -> {
                                // Loading state - continue
                            }
                        }
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load tax rate: ${e.message}"
                )
            }
        }
    }

    fun updateHsnCode(hsnCode: String) {
        _uiState.value = _uiState.value.copy(
            hsnCode = hsnCode,
            error = null
        )
    }

    fun updateTaxType(taxType: TaxType) {
        _uiState.value = _uiState.value.copy(
            taxType = taxType,
            error = null
        )
    }

    fun updateRatePercentage(rate: String) {
        val doubleRate = rate.toDoubleOrNull()
        if (doubleRate != null && doubleRate >= 0 && doubleRate <= 100) {
            _uiState.value = _uiState.value.copy(
                ratePercentage = doubleRate,
                ratePercentageText = rate,
                error = null
            )
        } else if (rate.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                ratePercentage = 0.0,
                ratePercentageText = rate,
                error = null
            )
        }
    }

    fun updateCessRate(rate: String) {
        val doubleRate = rate.toDoubleOrNull()
        if (doubleRate != null && doubleRate >= 0 && doubleRate <= 100) {
            _uiState.value = _uiState.value.copy(
                cessRate = doubleRate,
                cessRateText = rate,
                error = null
            )
        } else if (rate.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                cessRate = null,
                cessRateText = rate,
                error = null
            )
        }
    }

    fun updateCessAmountPerUnit(amount: String) {
        val doubleAmount = amount.toDoubleOrNull()
        if (doubleAmount != null && doubleAmount >= 0) {
            _uiState.value = _uiState.value.copy(
                cessAmountPerUnit = doubleAmount,
                cessAmountPerUnitText = amount,
                error = null
            )
        } else if (amount.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                cessAmountPerUnit = null,
                cessAmountPerUnitText = amount,
                error = null
            )
        }
    }

    fun updateBusinessType(businessType: BusinessType) {
        _uiState.value = _uiState.value.copy(
            businessType = businessType,
            error = null
        )
    }

    fun updateGeographicalZone(zone: String) {
        _uiState.value = _uiState.value.copy(
            geographicalZone = zone,
            error = null
        )
    }

    fun updateEffectiveFrom(timestamp: Long) {
        _uiState.value = _uiState.value.copy(
            effectiveFrom = timestamp,
            error = null
        )
    }

    fun updateEffectiveTo(timestamp: Long?) {
        _uiState.value = _uiState.value.copy(
            effectiveTo = timestamp,
            error = null
        )
    }

    fun updateIsActive(isActive: Boolean) {
        _uiState.value = _uiState.value.copy(
            isActive = isActive,
            error = null
        )
    }

    fun saveTaxRate(onSuccess: () -> Unit) {
        val currentState = _uiState.value

        if (!currentState.canSave) {
            _uiState.value = currentState.copy(
                error = "Please fill in all required fields with valid data"
            )
            return
        }

        _uiState.value = currentState.copy(isSaving = true, error = null)

        viewModelScope.launch {
            try {
                val taxRateToSave = if (taxRateId != null && currentState.originalTaxRate != null) {
                    // Update existing
                    currentState.originalTaxRate.copy(
                        hsnCode = currentState.hsnCode,
                        taxType = currentState.taxType,
                        ratePercentage = currentState.ratePercentage,
                        cessRate = currentState.cessRate,
                        cessAmountPerUnit = currentState.cessAmountPerUnit,
                        effectiveFrom = currentState.effectiveFrom,
                        effectiveTo = currentState.effectiveTo,
                        geographicalZone = currentState.geographicalZone,
                        businessType = currentState.businessType,
                        isActive = currentState.isActive
                    )
                } else {
                    // Create new
                    TaxRate(
                        id = Uuid.random().toString(),
                        hsnCode = currentState.hsnCode,
                        taxType = currentState.taxType,
                        ratePercentage = currentState.ratePercentage,
                        cessRate = currentState.cessRate,
                        cessAmountPerUnit = currentState.cessAmountPerUnit,
                        effectiveFrom = currentState.effectiveFrom,
                        effectiveTo = currentState.effectiveTo,
                        geographicalZone = currentState.geographicalZone,
                        businessType = currentState.businessType,
                        isActive = currentState.isActive
                    )
                }

                val result = if (taxRateId != null) {
                    taxStore.updateTaxRate(taxRateToSave)
                } else {
                    taxStore.createTaxRate(taxRateToSave)
                }

                if (result.isSuccess) {
                    _uiState.value = currentState.copy(
                        isSaving = false,
                        error = null
                    )
                    onSuccess()
                } else {
                    _uiState.value = currentState.copy(
                        isSaving = false,
                        error = "Failed to save tax rate: ${result.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = currentState.copy(
                    isSaving = false,
                    error = "Failed to save tax rate: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class TaxRateFormUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val hsnCode: String = "",
    val taxType: TaxType = TaxType.GST,
    val ratePercentage: Double = 0.0,
    val ratePercentageText: String = "",
    val cessRate: Double? = null,
    val cessRateText: String = "",
    val cessAmountPerUnit: Double? = null,
    val cessAmountPerUnitText: String = "",
    val effectiveFrom: Long = 0,
    val effectiveTo: Long? = null,
    val geographicalZone: String = "PAN_INDIA",
    val businessType: BusinessType = BusinessType.REGULAR,
    val isActive: Boolean = true,
    val originalTaxRate: TaxRate? = null
) {
    val canSave: Boolean
        get() = hsnCode.isNotBlank() &&
                ratePercentage > 0 &&
                effectiveFrom > 0 &&
                !isSaving

    val isValidHsnCode: Boolean
        get() = hsnCode.matches(Regex("^\\d{4,8}$"))

    val isValidRate: Boolean
        get() = ratePercentage in 0.0..100.0

    val totalTaxRate: Double
        get() = ratePercentage + (cessRate ?: 0.0)

    val formattedTaxRate: String
        get() = if (cessRate != null && cessRate > 0) {
            "${ratePercentage}% + ${cessRate}% Cess"
        } else {
            "${ratePercentage}%"
        }
}