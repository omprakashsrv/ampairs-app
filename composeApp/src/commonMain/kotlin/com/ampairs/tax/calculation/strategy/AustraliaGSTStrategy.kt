package com.ampairs.tax.calculation.strategy

import com.ampairs.common.sentry.ErrorTracking
import com.ampairs.tax.calculation.ITaxCalculationStrategy
import com.ampairs.tax.calculation.model.TaxCalculationRequest
import com.ampairs.tax.calculation.model.TaxCalculationResult
import com.ampairs.tax.calculation.model.TaxComponentResult
import com.ampairs.tax.calculation.model.TransactionType
import com.ampairs.tax.data.repository.TaxRuleRepository
import com.ampairs.tax.data.repository.TaxComponentRepository

/**
 * Australia GST Strategy - Simple 10% GST on most goods and services
 *
 * Key Features:
 * - Flat 10% GST rate
 * - GST-free supplies (basic food, health, education, exports)
 * - Input-taxed supplies (financial services, residential rent)
 * - $75,000 registration threshold
 * - Reverse charge for imported services
 * - Simplified BAS (Business Activity Statement) reporting
 */
class AustraliaGSTStrategy(
    private val taxRuleRepository: TaxRuleRepository,
    private val taxComponentRepository: TaxComponentRepository,
) : ITaxCalculationStrategy {

    override val countryCode: String = "AU"
    override val strategyName: String = "AUSTRALIA_GST"

    override suspend fun calculateTax(request: TaxCalculationRequest): Result<TaxCalculationResult> {
        return try {
            // 1. Determine jurisdiction (state for record keeping, GST is federal)
            val state = request.sourceLocation.state ?: "AU"

            // 2. Get effective tax rule
            val taxRule = taxRuleRepository.getEffectiveRule(
                taxCode = request.taxCode,
                jurisdiction = state
            ) ?: return Result.failure(Exception("Tax rule not found for code: ${request.taxCode}"))

            // 3. Handle special cases
            when {
                request.transactionContext.specialConditions["gst_free"] == "true" -> {
                    // GST-free supplies (basic food, health, education, exports)
                    return Result.success(
                        TaxCalculationResult(
                            taxCode = request.taxCode,
                            codeType = request.taxCodeType,
                            baseAmount = request.baseAmount * request.quantity,
                            quantity = request.quantity,
                            taxComponents = emptyList(),
                            totalTaxAmount = 0.0,
                            totalAmount = request.baseAmount * request.quantity,
                            jurisdiction = request.sourceLocation,
                            transactionContext = request.transactionContext,
                            countryCode = countryCode,
                            metadata = mapOf(
                                "gst_free" to "true",
                                "reason" to "Basic food, health, education, or export",
                                "note" to "GST-free with input tax credit available"
                            )
                        )
                    )
                }
                request.transactionContext.specialConditions["input_taxed"] == "true" -> {
                    // Input-taxed supplies (financial services, residential rent)
                    return Result.success(
                        TaxCalculationResult(
                            taxCode = request.taxCode,
                            codeType = request.taxCodeType,
                            baseAmount = request.baseAmount * request.quantity,
                            quantity = request.quantity,
                            taxComponents = emptyList(),
                            totalTaxAmount = 0.0,
                            totalAmount = request.baseAmount * request.quantity,
                            jurisdiction = request.sourceLocation,
                            transactionContext = request.transactionContext,
                            countryCode = countryCode,
                            metadata = mapOf(
                                "input_taxed" to "true",
                                "reason" to "Financial services or residential rent",
                                "note" to "No GST charged, no input tax credit available"
                            )
                        )
                    )
                }
                request.transactionType == TransactionType.EXPORT -> {
                    // Exports are GST-free
                    return Result.success(
                        TaxCalculationResult(
                            taxCode = request.taxCode,
                            codeType = request.taxCodeType,
                            baseAmount = request.baseAmount * request.quantity,
                            quantity = request.quantity,
                            taxComponents = emptyList(),
                            totalTaxAmount = 0.0,
                            totalAmount = request.baseAmount * request.quantity,
                            jurisdiction = request.sourceLocation,
                            transactionContext = request.transactionContext,
                            countryCode = countryCode,
                            metadata = mapOf(
                                "gst_free" to "true",
                                "reason" to "Export outside Australia",
                                "note" to "GST-free with input tax credit available"
                            )
                        )
                    )
                }
                request.transactionType == TransactionType.IMPORT && request.transactionContext.isReverseCharge -> {
                    // Imported services - reverse charge mechanism
                    return Result.success(
                        TaxCalculationResult(
                            taxCode = request.taxCode,
                            codeType = request.taxCodeType,
                            baseAmount = request.baseAmount * request.quantity,
                            quantity = request.quantity,
                            taxComponents = emptyList(),
                            totalTaxAmount = 0.0,
                            totalAmount = request.baseAmount * request.quantity,
                            jurisdiction = request.sourceLocation,
                            transactionContext = request.transactionContext,
                            countryCode = countryCode,
                            metadata = mapOf(
                                "reverse_charge" to "true",
                                "reason" to "Imported services from offshore supplier",
                                "note" to "Buyer accounts for GST under reverse charge"
                            )
                        )
                    )
                }
            }

            // 4. Standard GST calculation
            val scenario = taxRule.componentComposition.standard
                ?: return Result.failure(Exception("Standard composition not configured"))

            // 5. Calculate base amount
            val baseAmount = request.baseAmount * request.quantity

            // 6. Calculate GST (always 10%)
            val components = mutableListOf<TaxComponentResult>()
            var totalTaxAmount = 0.0

            for (componentConfig in scenario.components.sortedBy { it.order }) {
                // Get workspace component details
                val workspaceComponent = taxComponentRepository.getWorkspaceComponentById(componentConfig.id)
                    ?: continue

                // Get component type
                val componentType = taxComponentRepository.getComponentTypeById(workspaceComponent.componentTypeId)
                    ?: continue

                // Calculate GST amount (always flat 10%)
                val taxAmount = baseAmount * (componentConfig.rate / 100.0)
                totalTaxAmount += taxAmount

                // Create component result
                components.add(
                    TaxComponentResult(
                        componentId = componentConfig.id,
                        componentName = "GST",
                        taxType = componentType.componentCode,
                        ratePercentage = componentConfig.rate,
                        taxableAmount = baseAmount,
                        taxAmount = taxAmount,
                        description = "Goods and Services Tax @ 10%",
                        isCompound = false
                    )
                )
            }

            // 7. Build result
            val result = TaxCalculationResult(
                taxCode = request.taxCode,
                codeType = request.taxCodeType,
                baseAmount = baseAmount,
                quantity = request.quantity,
                taxComponents = components,
                totalTaxAmount = totalTaxAmount,
                totalAmount = baseAmount + totalTaxAmount,
                jurisdiction = request.sourceLocation,
                transactionContext = request.transactionContext,
                countryCode = countryCode,
                metadata = buildMetadata(state)
            )

            Result.success(result)

        } catch (e: Exception) {
            ErrorTracking.captureException(e, "AustraliaGSTStrategy.calculateTax")
            Result.failure(e)
        }
    }

    override fun validateTaxCode(code: String, codeType: String): Boolean {
        // Basic validation - accept all codes for now
        return code.isNotBlank()
    }

    /**
     * Build metadata for result
     */
    private fun buildMetadata(state: String): Map<String, String> {
        return mapOf(
            "gst_rate" to "10%",
            "state" to state,
            "registration_threshold" to "$75,000 AUD",
            "reporting" to "Business Activity Statement (BAS)",
            "note" to "GST applies nationwide - no state variations"
        )
    }
}
