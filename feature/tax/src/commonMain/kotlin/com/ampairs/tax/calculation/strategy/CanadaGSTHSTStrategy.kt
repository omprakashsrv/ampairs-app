package com.ampairs.tax.calculation.strategy

import com.ampairs.common.sentry.ErrorTracking
import com.ampairs.tax.calculation.ITaxCalculationStrategy
import com.ampairs.tax.calculation.model.TaxCalculationRequest
import com.ampairs.tax.calculation.model.TaxCalculationResult
import com.ampairs.tax.calculation.model.TaxComponentResult
import com.ampairs.tax.data.repository.TaxRuleRepository
import com.ampairs.tax.data.repository.TaxComponentRepository
import com.ampairs.tax.util.formatDecimal

/**
 * Canada GST/HST/PST/QST Strategy - Complex multi-layered tax system
 *
 * Key Features:
 * - Federal GST: 5% (applies nationwide)
 * - HST (Harmonized): GST + PST combined (ON, NB, NS, NL, PE: 15%, BC: 12%)
 * - PST (Provincial): Separate from GST (BC: 7%, SK: 6%, MB: 7%)
 * - QST (Quebec): 9.975% (compound on GST base)
 * - Zero-rated supplies (groceries, medical)
 * - Exempt supplies (residential rent, financial services)
 * - Place of supply rules determine applicable tax
 */
class CanadaGSTHSTStrategy(
    private val taxRuleRepository: TaxRuleRepository,
    private val taxComponentRepository: TaxComponentRepository,
) : ITaxCalculationStrategy {

    override val countryCode: String = "CA"
    override val strategyName: String = "CANADA_GST_HST"

    override suspend fun calculateTax(request: TaxCalculationRequest): Result<TaxCalculationResult> {
        return try {
            // 1. Determine province (place of supply)
            val province = request.destinationLocation.state
                ?: return Result.failure(Exception("Province is required for Canada tax"))

            // 2. Get effective tax rule
            val taxRule = taxRuleRepository.getEffectiveRule(
                taxCode = request.taxCode,
                jurisdiction = province
            ) ?: return Result.failure(Exception("Tax rule not found for code: ${request.taxCode}"))

            // 3. Determine tax regime for province
            val taxRegime = getTaxRegime(province)

            // 4. Handle special cases
            when {
                request.transactionContext.specialConditions["zero_rated"] == "true" -> {
                    return Result.success(
                        TaxCalculationResult(
                            taxCode = request.taxCode,
                            codeType = request.taxCodeType,
                            baseAmount = request.baseAmount * request.quantity,
                            quantity = request.quantity,
                            taxComponents = emptyList(),
                            totalTaxAmount = 0.0,
                            totalAmount = request.baseAmount * request.quantity,
                            jurisdiction = request.destinationLocation,
                            transactionContext = request.transactionContext,
                            countryCode = countryCode,
                            metadata = mapOf(
                                "zero_rated" to "true",
                                "reason" to "Basic groceries, prescription drugs, or medical devices",
                                "province" to province,
                                "tax_regime" to taxRegime
                            )
                        )
                    )
                }
                request.transactionContext.exemptionReason != null -> {
                    return Result.success(
                        TaxCalculationResult(
                            taxCode = request.taxCode,
                            codeType = request.taxCodeType,
                            baseAmount = request.baseAmount * request.quantity,
                            quantity = request.quantity,
                            taxComponents = emptyList(),
                            totalTaxAmount = 0.0,
                            totalAmount = request.baseAmount * request.quantity,
                            jurisdiction = request.destinationLocation,
                            transactionContext = request.transactionContext,
                            countryCode = countryCode,
                            metadata = mapOf(
                                "exempt" to "true",
                                "reason" to "Residential rent, financial services, or child care",
                                "province" to province,
                                "tax_regime" to taxRegime
                            )
                        )
                    )
                }
            }

            // 5. Select appropriate scenario
            val scenario = taxRule.componentComposition["standard"]
                ?: return Result.failure(Exception("Standard composition not configured"))

            // 6. Calculate base amount
            val baseAmount = request.baseAmount * request.quantity

            // 7. Calculate tax components
            val components = mutableListOf<TaxComponentResult>()
            var totalTaxAmount = 0.0
            var gstBase = baseAmount  // Used for QST compound calculation

            // Sort by order - important for QST compound calculation
            val sortedComponents = scenario.components.sortedBy { it.order }

            for (componentConfig in sortedComponents) {
                // Get workspace component details
                val workspaceComponent = taxComponentRepository.getWorkspaceComponentById(componentConfig.id)
                    ?: continue

                // Get component type
                val componentType = taxComponentRepository.getComponentTypeById(workspaceComponent.componentTypeId)
                    ?: continue

                // Determine taxable amount
                val taxableAmount = if (false) {
                    // QST compounds on (base + GST)
                    gstBase
                } else {
                    baseAmount
                }

                // Calculate tax amount
                val taxAmount = taxableAmount * (componentConfig.rate / 100.0)
                totalTaxAmount += taxAmount

                // Update GST base for QST compounding
                if (componentType.componentCode == "GST") {
                    gstBase = baseAmount + taxAmount
                }

                // Determine component display name
                val taxName = when (componentType.componentCode) {
                    "GST" -> "GST (Federal)"
                    "HST" -> "HST (Harmonized)"
                    "PST" -> "PST (Provincial)"
                    "QST" -> "QST (Quebec)"
                    else -> componentConfig.name
                }

                // Create component result
                components.add(
                    TaxComponentResult(
                        componentId = componentConfig.id,
                        componentName = taxName,
                        taxType = componentType.componentCode,
                        ratePercentage = componentConfig.rate,
                        taxableAmount = taxableAmount,
                        taxAmount = taxAmount,
                        description = "$taxName @ ${componentConfig.rate.formatDecimal(1)}%",
                        isCompound = false
                    )
                )
            }

            // 8. Build result
            val result = TaxCalculationResult(
                taxCode = request.taxCode,
                codeType = request.taxCodeType,
                baseAmount = baseAmount,
                quantity = request.quantity,
                taxComponents = components,
                totalTaxAmount = totalTaxAmount,
                totalAmount = baseAmount + totalTaxAmount,
                jurisdiction = request.destinationLocation,
                transactionContext = request.transactionContext,
                countryCode = countryCode,
                metadata = buildMetadata(province, taxRegime, components)
            )

            Result.success(result)

        } catch (e: Exception) {
            ErrorTracking.captureException(e, "CanadaGSTHSTStrategy.calculateTax")
            Result.failure(e)
        }
    }

    override fun validateTaxCode(code: String, codeType: String): Boolean {
        // Basic validation - accept all codes for now
        return code.isNotBlank()
    }

    /**
     * Determine tax regime for province
     */
    private fun getTaxRegime(province: String): String {
        return when (province) {
            "ON", "NB", "NS", "NL", "PE" -> "HST"
            "QC" -> "GST+QST"
            "BC", "SK", "MB" -> "GST+PST"
            "AB", "NT", "NU", "YT" -> "GST_ONLY"
            else -> "UNKNOWN"
        }
    }

    /**
     * Build metadata for result
     */
    private fun buildMetadata(
        province: String,
        taxRegime: String,
        components: List<TaxComponentResult>
    ): Map<String, String> {
        val metadata = mutableMapOf(
            "province" to province,
            "tax_regime" to taxRegime,
            "federal_gst" to "5%"
        )

        // Add regime-specific info
        when (taxRegime) {
            "HST" -> {
                val hstRate = components.firstOrNull { it.taxType == "HST" }?.ratePercentage
                metadata["hst_rate"] = "${hstRate ?: 0}%"
                metadata["note"] = "HST combines federal GST and provincial portion"
            }
            "GST+QST" -> {
                metadata["qst_rate"] = "9.975%"
                metadata["compound"] = "true"
                metadata["note"] = "QST compounds on GST base"
            }
            "GST+PST" -> {
                val pstRate = components.firstOrNull { it.taxType == "PST" }?.ratePercentage
                metadata["pst_rate"] = "${pstRate ?: 0}%"
                metadata["note"] = "GST and PST calculated separately on base amount"
            }
            "GST_ONLY" -> {
                metadata["note"] = "Only federal GST applies"
            }
        }

        return metadata
    }
}
