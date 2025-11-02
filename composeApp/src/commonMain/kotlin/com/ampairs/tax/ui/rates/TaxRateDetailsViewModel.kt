package com.ampairs.tax.ui.rates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.tax.domain.TaxRate
import com.ampairs.tax.domain.TaxStore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TaxRateDetailsViewModel(
    private val taxRateId: String,
    private val taxStore: TaxStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaxRateDetailsUiState())
    val uiState: StateFlow<TaxRateDetailsUiState> = _uiState.asStateFlow()

    init {
        loadTaxRateDetails()
    }

    private fun loadTaxRateDetails() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                // Simulate loading delay
                kotlinx.coroutines.delay(500)

                // For now, create a mock tax rate to show the UI
                // In real implementation, you'd fetch from taxRateStore
                val mockTaxRate = createMockTaxRate(taxRateId)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    taxRate = mockTaxRate,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load tax rate: ${e.message}"
                )
            }
        }
    }

    fun refreshTaxRate() {
        loadTaxRateDetails()
    }

    fun deleteTaxRate(onSuccess: () -> Unit) {
        _uiState.value = _uiState.value.copy(isDeleting = true, error = null)

        viewModelScope.launch {
            try {
                // Simulate delete operation
                kotlinx.coroutines.delay(1000)

                // For now, just simulate success
                // In real implementation, you'd call taxStore.deleteTaxRate(taxRateId)
                _uiState.value = _uiState.value.copy(isDeleting = false, error = null)
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isDeleting = false,
                    error = "Failed to delete tax rate: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    @OptIn(kotlin.time.ExperimentalTime::class)
    private fun createMockTaxRate(id: String): TaxRate {
        val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
        return TaxRate(
            id = id,
            hsnCode = "12345678",
            taxType = com.ampairs.tax.domain.TaxType.GST,
            ratePercentage = 18.0,
            cessRate = 2.0,
            cessAmountPerUnit = null,
            effectiveFrom = now - (30 * 24 * 60 * 60 * 1000), // 30 days ago
            effectiveTo = null,
            geographicalZone = "PAN_INDIA",
            businessType = com.ampairs.tax.domain.BusinessType.REGULAR,
            versionNumber = 1,
            isActive = true,
            createdAt = now - (30 * 24 * 60 * 60 * 1000),
            updatedAt = now - (7 * 24 * 60 * 60 * 1000)
        )
    }
}

data class TaxRateDetailsUiState(
    val isLoading: Boolean = false,
    val isDeleting: Boolean = false,
    val error: String? = null,
    val taxRate: TaxRate? = null
) {
    val hasData: Boolean get() = taxRate != null
    val showContent: Boolean get() = !isLoading && hasData
    val showEmptyState: Boolean get() = !isLoading && !hasData && error == null
}