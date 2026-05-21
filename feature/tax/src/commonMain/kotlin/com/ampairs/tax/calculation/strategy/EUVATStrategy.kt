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
 * EU VAT Strategy - Harmonized VAT system across EU member states
 *
 * Key Features:
 * - Standard rate (minimum 15%, varies by country)
 * - Reduced rates (5-12%, country-specific)
 * - B2B reverse charge within EU
 * - B2C destination-based VAT
 * - OSS (One-Stop Shop) for cross-border B2C
 * - Intra-community supply rules
 * - Digital services VAT (place of consumption)
 */
@Inject
class EUVATStrategy(
    private val taxRuleRepository: TaxRuleRepository,
    private val taxComponentRepository: TaxComponentRepository,
) : ITaxCalculationStrategy {

    override val countryCode: String = "EU"
    override val strategyName: String = "EU_VAT"

    override suspend fun calculateTax(request: TaxCalculationRequest): Result<TaxCalculationResult> {
        return try {
            // 1. Determine relevant country (seller or buyer depending on rules)
            val sellerCountry = request.sourceLocation.country ?: "EU"
            val buyerCountry = request.destinationLocation.country ?: sellerCountry

            // 2. Determine if intra-EU or domestic
            val isIntraEU = sellerCountry != buyerCountry && isEUCountry(buyerCountry)

            // 3. Get effective tax rule (from buyer's country for B2C, seller's for B2B)
            val taxJurisdiction = if (request.transactionType == TransactionType.B2C && isIntraEU) {
                buyerCountry  // Destination principle for B2C
            } else {
                sellerCountry // Origin for domestic or B2B
            }

            val taxRule = taxRuleRepository.getEffectiveRule(
                taxCode = request.taxCode,
                jurisdiction = taxJurisdiction
            ) ?: return Result.failure(Exception("Tax rule not found for code: ${request.taxCode}"))

            // 4. Select scenario based on transaction type
            val scenario = when {
                // B2B Intra-EU with valid VAT number - Reverse Charge
                request.transactionType == TransactionType.B2B && isIntraEU && request.transactionContext.specialConditions["buyer_vat_number"] != null -> {
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
                                "seller_country" to sellerCountry,
                                "buyer_country" to buyerCountry,
                                "buyer_vat" to (request.transactionContext.specialConditions["buyer_vat_number"] ?: ""),
                                "note" to "Intra-community supply - buyer accounts for VAT"
                            )
                        )
                    )
                }

                // Export outside EU - Zero rated
                request.transactionType == TransactionType.EXPORT && !isEUCountry(buyerCountry) -> {
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
                                "reason" to "Export outside EU",
                                "seller_country" to sellerCountry,
                                "buyer_country" to buyerCountry
                            )
                        )
                    )
                }

                // B2C Intra-EU (destination principle) or domestic
                else -> {
                    taxRule.componentComposition["standard"]
                        ?: return Result.failure(Exception("Standard composition not configured"))
                }
            }

            // 5. Calculate base amount
            val baseAmount = request.baseAmount * request.quantity

            // 6. Calculate VAT components
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

                // Determine VAT rate category
                val vatCategory = when {
                    componentConfig.rate >= 15.0 -> "Standard Rate"
                    componentConfig.rate in 5.0..14.9 -> "Reduced Rate"
                    componentConfig.rate == 0.0 -> "Zero Rate"
                    else -> "Super-Reduced Rate"
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
                        description = "$vatCategory @ ${componentConfig.rate.formatDecimal(1)}%",
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
                metadata = buildMetadata(
                    sellerCountry,
                    buyerCountry,
                    isIntraEU,
                    components.firstOrNull()?.ratePercentage
                )
            )

            Result.success(result)

        } catch (e: Exception) {
            ErrorTracking.captureException(e, "EUVATStrategy.calculateTax")
            Result.failure(e)
        }
    }

    override fun validateTaxCode(code: String, codeType: String): Boolean {
        // Basic validation - accept all codes for now
        return code.isNotBlank()
    }

    /**
     * Check if country is EU member state
     */
    private fun isEUCountry(countryCode: String): Boolean {
        val euCountries = setOf(
            "AT", "BE", "BG", "HR", "CY", "CZ", "DK", "EE", "FI", "FR",
            "DE", "GR", "HU", "IE", "IT", "LV", "LT", "LU", "MT", "NL",
            "PL", "PT", "RO", "SK", "SI", "ES", "SE"
        )
        return euCountries.contains(countryCode)
    }

    /**
     * Build metadata for result
     */
    private fun buildMetadata(
        sellerCountry: String,
        buyerCountry: String,
        isIntraEU: Boolean,
        vatRate: Double?
    ): Map<String, String> {
        val vatCategory = when {
            vatRate == null -> "Not Applicable"
            vatRate == 0.0 -> "Zero Rate"
            vatRate in 15.0..27.0 -> "Standard Rate"
            vatRate in 5.0..14.9 -> "Reduced Rate"
            else -> "Special Rate"
        }

        return mutableMapOf(
            "vat_category" to vatCategory,
            "seller_country" to sellerCountry,
            "buyer_country" to buyerCountry,
            "intra_eu" to isIntraEU.toString(),
            "vat_directive" to "2006/112/EC"
        ).apply {
            if (isIntraEU) {
                put("sourcing", "destination_principle")
                put("oss_eligible", "true")
            } else {
                put("sourcing", "origin_principle")
            }
        }
    }
}
