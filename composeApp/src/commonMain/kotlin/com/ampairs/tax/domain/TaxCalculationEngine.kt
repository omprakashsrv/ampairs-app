package com.ampairs.tax.domain

import kotlin.math.round

@OptIn(kotlin.time.ExperimentalTime::class)

class TaxCalculationEngine(
    private val taxStore: TaxStore
) {

    suspend fun calculateTax(request: TaxCalculationRequest): Result<TaxCalculationResult> {
        return try {
            // Get effective tax rate for the HSN code and business type
            val taxRate = taxStore.getEffectiveTaxRate(
                hsnCode = request.hsnCode,
                businessType = request.businessType
            ) ?: return Result.failure(Exception("Tax rate not found for HSN code: ${request.hsnCode}"))

            val baseAmount = request.baseAmount * request.quantity
            val isIntraState = request.sourceState == request.destinationState

            // Calculate GST amounts
            val gstRate = taxRate.ratePercentage / 100
            val totalGstAmount = roundToTwoDecimals(baseAmount * gstRate)

            // Calculate CESS amounts
            val cessAmount = calculateCessAmount(baseAmount, request.quantity, taxRate)

            val result = if (isIntraState) {
                calculateIntraStateTax(
                    request = request,
                    baseAmount = baseAmount,
                    totalGstAmount = totalGstAmount,
                    cessAmount = cessAmount,
                    taxRate = taxRate
                )
            } else {
                calculateInterStateTax(
                    request = request,
                    baseAmount = baseAmount,
                    totalGstAmount = totalGstAmount,
                    cessAmount = cessAmount,
                    taxRate = taxRate
                )
            }

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun calculateBulkTax(
        requests: List<TaxCalculationRequest>
    ): Result<BulkTaxCalculationResult> {
        return try {
            val results = mutableListOf<TaxCalculationResult>()
            var totalBaseAmount = 0.0
            var totalCgstAmount = 0.0
            var totalSgstAmount = 0.0
            var totalIgstAmount = 0.0
            var totalCessAmount = 0.0

            for (request in requests) {
                val result = calculateTax(request)
                if (result.isSuccess) {
                    val taxResult = result.getOrThrow()
                    results.add(taxResult)

                    totalBaseAmount += taxResult.baseAmount
                    totalCgstAmount += taxResult.cgstAmount
                    totalSgstAmount += taxResult.sgstAmount
                    totalIgstAmount += taxResult.igstAmount
                    totalCessAmount += taxResult.cessAmount
                } else {
                    return Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
                }
            }

            val totalTaxAmount = totalCgstAmount + totalSgstAmount + totalIgstAmount + totalCessAmount
            val totalAmount = totalBaseAmount + totalTaxAmount

            val bulkResult = BulkTaxCalculationResult(
                items = results,
                totalBaseAmount = roundToTwoDecimals(totalBaseAmount),
                totalCgstAmount = roundToTwoDecimals(totalCgstAmount),
                totalSgstAmount = roundToTwoDecimals(totalSgstAmount),
                totalIgstAmount = roundToTwoDecimals(totalIgstAmount),
                totalCessAmount = roundToTwoDecimals(totalCessAmount),
                totalTaxAmount = roundToTwoDecimals(totalTaxAmount),
                totalAmount = roundToTwoDecimals(totalAmount)
            )

            Result.success(bulkResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun calculateIntraStateTax(
        request: TaxCalculationRequest,
        baseAmount: Double,
        totalGstAmount: Double,
        cessAmount: Double,
        taxRate: TaxRate
    ): TaxCalculationResult {
        // For intra-state transactions: CGST + SGST
        val cgstAmount = roundToTwoDecimals(totalGstAmount / 2)
        val sgstAmount = roundToTwoDecimals(totalGstAmount - cgstAmount) // Handle odd paise

        val taxBreakdown = buildList {
            add(TaxBreakdownItem(
                taxType = TaxType.CGST,
                ratePercentage = taxRate.ratePercentage / 2,
                taxableAmount = baseAmount,
                taxAmount = cgstAmount,
                description = "Central Goods and Services Tax"
            ))
            add(TaxBreakdownItem(
                taxType = TaxType.SGST,
                ratePercentage = taxRate.ratePercentage / 2,
                taxableAmount = baseAmount,
                taxAmount = sgstAmount,
                description = "State Goods and Services Tax"
            ))
            if (cessAmount > 0) {
                add(TaxBreakdownItem(
                    taxType = TaxType.CESS,
                    ratePercentage = taxRate.cessRate ?: 0.0,
                    taxableAmount = baseAmount,
                    taxAmount = cessAmount,
                    description = "Compensation Cess"
                ))
            }
        }

        val totalTaxAmount = cgstAmount + sgstAmount + cessAmount

        return TaxCalculationResult(
            hsnCode = request.hsnCode,
            baseAmount = baseAmount,
            quantity = request.quantity,
            cgstAmount = cgstAmount,
            sgstAmount = sgstAmount,
            igstAmount = 0.0,
            cessAmount = cessAmount,
            totalTaxAmount = roundToTwoDecimals(totalTaxAmount),
            totalAmount = roundToTwoDecimals(baseAmount + totalTaxAmount),
            taxBreakdown = taxBreakdown,
            transactionType = request.transactionType,
            isIntraState = true
        )
    }

    private fun calculateInterStateTax(
        request: TaxCalculationRequest,
        baseAmount: Double,
        totalGstAmount: Double,
        cessAmount: Double,
        taxRate: TaxRate
    ): TaxCalculationResult {
        // For inter-state transactions: IGST
        val igstAmount = totalGstAmount

        val taxBreakdown = buildList {
            add(TaxBreakdownItem(
                taxType = TaxType.IGST,
                ratePercentage = taxRate.ratePercentage,
                taxableAmount = baseAmount,
                taxAmount = igstAmount,
                description = "Integrated Goods and Services Tax"
            ))
            if (cessAmount > 0) {
                add(TaxBreakdownItem(
                    taxType = TaxType.CESS,
                    ratePercentage = taxRate.cessRate ?: 0.0,
                    taxableAmount = baseAmount,
                    taxAmount = cessAmount,
                    description = "Compensation Cess"
                ))
            }
        }

        val totalTaxAmount = igstAmount + cessAmount

        return TaxCalculationResult(
            hsnCode = request.hsnCode,
            baseAmount = baseAmount,
            quantity = request.quantity,
            cgstAmount = 0.0,
            sgstAmount = 0.0,
            igstAmount = igstAmount,
            cessAmount = cessAmount,
            totalTaxAmount = roundToTwoDecimals(totalTaxAmount),
            totalAmount = roundToTwoDecimals(baseAmount + totalTaxAmount),
            taxBreakdown = taxBreakdown,
            transactionType = request.transactionType,
            isIntraState = false
        )
    }

    private fun calculateCessAmount(
        baseAmount: Double,
        quantity: Int,
        taxRate: TaxRate
    ): Double {
        return when {
            taxRate.cessRate != null && taxRate.cessRate > 0 -> {
                // Percentage-based CESS
                roundToTwoDecimals(baseAmount * taxRate.cessRate / 100)
            }
            taxRate.cessAmountPerUnit != null && taxRate.cessAmountPerUnit > 0 -> {
                // Fixed amount per unit CESS
                roundToTwoDecimals(taxRate.cessAmountPerUnit * quantity)
            }
            else -> 0.0
        }
    }

    private fun roundToTwoDecimals(value: Double): Double {
        return round(value * 100) / 100
    }

    // Utility methods for tax validation and optimization
    suspend fun validateTaxConfiguration(hsnCode: String): Result<TaxValidationResult> {
        return try {
            val taxRates = taxStore.getTaxRatesByHsnCode(hsnCode)

            val validationResult = TaxValidationResult(
                hsnCode = hsnCode,
                hasActiveTaxRate = taxRates.any { it.isActive && it.isCurrentlyEffective },
                conflictingRates = findConflictingRates(taxRates),
                missingBusinessTypes = findMissingBusinessTypes(taxRates),
                warnings = generateTaxWarnings(taxRates)
            )

            Result.success(validationResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun findConflictingRates(taxRates: List<TaxRate>): List<String> {
        val conflicts = mutableListOf<String>()
        val activeRates = taxRates.filter { it.isActive }

        for (businessType in BusinessType.values()) {
            val ratesForType = activeRates.filter { it.businessType == businessType }
            val currentEffectiveRates = ratesForType.filter { it.isCurrentlyEffective }

            if (currentEffectiveRates.size > 1) {
                conflicts.add("Multiple effective tax rates found for business type: $businessType")
            }
        }

        return conflicts
    }

    private fun findMissingBusinessTypes(taxRates: List<TaxRate>): List<BusinessType> {
        val configuredTypes = taxRates.filter { it.isActive && it.isCurrentlyEffective }
            .map { it.businessType }
            .toSet()

        return BusinessType.values().filter { it !in configuredTypes }
    }

    private fun generateTaxWarnings(taxRates: List<TaxRate>): List<String> {
        val warnings = mutableListOf<String>()

        // Check for rates with no end date (indefinite)
        val indefiniteRates = taxRates.filter { it.effectiveTo == null }
        if (indefiniteRates.size > 1) {
            warnings.add("Multiple tax rates with no end date found")
        }

        // Check for future effective rates
        val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
        val futureRates = taxRates.filter { it.effectiveFrom > now }
        if (futureRates.isNotEmpty()) {
            warnings.add("${futureRates.size} future tax rate(s) configured")
        }

        return warnings
    }
}

data class TaxValidationResult(
    val hsnCode: String,
    val hasActiveTaxRate: Boolean,
    val conflictingRates: List<String>,
    val missingBusinessTypes: List<BusinessType>,
    val warnings: List<String>
) {
    val isValid: Boolean
        get() = hasActiveTaxRate && conflictingRates.isEmpty()
}