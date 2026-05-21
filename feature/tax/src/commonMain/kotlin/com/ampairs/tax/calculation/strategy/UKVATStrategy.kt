package com.ampairs.tax.calculation.strategy

import com.ampairs.common.sentry.ErrorTracking
import com.ampairs.tax.calculation.ITaxCalculationStrategy
import com.ampairs.tax.calculation.model.TaxCalculationRequest
import com.ampairs.tax.calculation.model.TaxCalculationResult
import com.ampairs.tax.calculation.model.TaxComponentResult
import com.ampairs.tax.calculation.model.TransactionType
import com.ampairs.tax.data.repository.TaxRuleRepository
import com.ampairs.tax.data.repository.TaxComponentRepository
import com.ampairs.tax.util.formatDecimal
import dev.zacsweers.metro.Inject

/**
 * UK VAT Strategy - Standard (20%), Reduced (5%), Zero-rated (0%)
 *
 * Key Features:
 * - Three VAT rates: Standard 20%, Reduced 5%, Zero 0%
 * - Exempt supplies (no VAT, no input credit)
 * - VAT registration threshold (£90,000 as of 2025)
 * - Reverse charge for B2B services from outside UK
 * - Northern Ireland special rules (Windsor Framework)
 */
@Inject
class UKVATStrategy(
    private val taxRuleRepository: TaxRuleRepository,
    private val taxComponentRepository: TaxComponentRepository,
) : ITaxCalculationStrategy {

    override val countryCode: String = "GB"
    override val strategyName: String = "UK_VAT"

    override suspend fun calculateTax(request: TaxCalculationRequest): Result<TaxCalculationResult> {
        return try {
            // 1. Determine jurisdiction (whole UK or Northern Ireland specific)
            val jurisdiction = request.sourceLocation.state ?: "UK"

            // 2. Get effective tax rule
            val taxRule = taxRuleRepository.getEffectiveRule(
                taxCode = request.taxCode,
                jurisdiction = jurisdiction
            ) ?: return Result.failure(Exception("Tax rule not found for code: ${request.taxCode}"))

            // 3. Select scenario based on transaction type
            val scenario = when {
                request.transactionContext.isReverseCharge -> {
                    // Reverse charge - buyer accounts for VAT
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
                                "note" to "Buyer accounts for VAT under reverse charge"
                            )
                        )
                    )
                }
                request.transactionType == TransactionType.EXPORT -> {
                    // Zero-rated export
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
                                "zero_rated" to "true",
                                "reason" to "Export outside UK"
                            )
                        )
                    )
                }
                request.transactionContext.exemptionReason != null -> {
                    // Exempt supply
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
                                "exempt" to "true",
                                "note" to "Exempt supply - no VAT charged, no input credit"
                            )
                        )
                    )
                }
                else -> {
                    // Standard VAT (B2C or standard B2B)
                    taxRule.componentComposition["standard"]
                        ?: return Result.failure(Exception("Standard composition not configured"))
                }
            }

            // 4. Calculate base amount
            val baseAmount = request.baseAmount * request.quantity

            // 5. Calculate VAT components
            val components = mutableListOf<TaxComponentResult>()
            var totalTaxAmount = 0.0

            for (componentConfig in scenario.components.sortedBy { it.order }) {
                // Get workspace component details
                val workspaceComponent = taxComponentRepository.getWorkspaceComponentById(componentConfig.id)
                    ?: continue

                // Get component type
                val componentType = taxComponentRepository.getComponentTypeById(workspaceComponent.componentTypeId)
                    ?: continue

                // Calculate VAT amount
                val taxAmount = baseAmount * (componentConfig.rate / 100.0)
                totalTaxAmount += taxAmount

                // Determine VAT type for display
                val vatType = when {
                    componentConfig.rate == 20.0 -> "Standard Rate"
                    componentConfig.rate == 5.0 -> "Reduced Rate"
                    componentConfig.rate == 0.0 -> "Zero Rate"
                    else -> "VAT"
                }

                // Create component result
                components.add(
                    TaxComponentResult(
                        componentId = componentConfig.id,
                        componentName = componentConfig.name,
                        taxType = componentType.componentCode,
                        ratePercentage = componentConfig.rate,
                        taxableAmount = baseAmount,
                        taxAmount = taxAmount,
                        description = "$vatType @ ${componentConfig.rate.formatDecimal(1)}%",
                        isCompound = false
                    )
                )
            }

            // 6. Build result
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
                metadata = buildMetadata(jurisdiction, components.firstOrNull()?.ratePercentage)
            )

            Result.success(result)

        } catch (e: Exception) {
            ErrorTracking.captureException(e, "UKVATStrategy.calculateTax")
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
    private fun buildMetadata(jurisdiction: String, vatRate: Double?): Map<String, String> {
        val vatCategory = when (vatRate) {
            20.0 -> "Standard Rate"
            5.0 -> "Reduced Rate"
            0.0 -> "Zero Rated"
            null -> "Not Applicable"
            else -> "Special Rate"
        }

        return mapOf(
            "vat_category" to vatCategory,
            "jurisdiction" to jurisdiction,
            "registration_threshold" to "£90,000",
            "scheme" to if (jurisdiction == "NI") "Windsor Framework applies" else "Standard UK VAT"
        )
    }
}
