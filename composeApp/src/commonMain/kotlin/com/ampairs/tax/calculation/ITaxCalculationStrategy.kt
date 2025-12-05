package com.ampairs.tax.calculation

import com.ampairs.tax.calculation.model.TaxCalculationRequest
import com.ampairs.tax.calculation.model.TaxCalculationResult

/**
 * Tax Calculation Strategy Interface
 * Country-specific implementations for tax calculation
 */
interface ITaxCalculationStrategy {
    /**
     * Country code this strategy handles
     */
    val countryCode: String

    /**
     * Strategy name for identification
     */
    val strategyName: String

    /**
     * Calculate tax using local rules from Room database
     * Must work completely offline
     */
    suspend fun calculateTax(request: TaxCalculationRequest): Result<TaxCalculationResult>

    /**
     * Validate tax code format (offline validation)
     */
    fun validateTaxCode(code: String, codeType: String): Boolean
}
