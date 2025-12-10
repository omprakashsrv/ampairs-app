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
 * USA Sales Tax Strategy - State + County + City + Special District taxes
 *
 * Key Features:
 * - Destination-based sourcing (tax where buyer is located)
 * - Cascading jurisdiction levels (State → County → City → Special)
 * - Each jurisdiction can have different rates
 * - No federal sales tax in USA
 * - Economic nexus thresholds determine tax obligation
 */
class USASalesTaxStrategy(
    private val taxRuleRepository: TaxRuleRepository,
    private val taxComponentRepository: TaxComponentRepository,
) : ITaxCalculationStrategy {

    override val countryCode: String = "US"
    override val strategyName: String = "USA_SALES_TAX"

    override suspend fun calculateTax(request: TaxCalculationRequest): Result<TaxCalculationResult> {
        return try {
            // 1. Determine jurisdiction (destination-based)
            val state = request.destinationLocation.state
                ?: return Result.failure(Exception("Destination state is required for USA sales tax"))

            val county = request.destinationLocation.county
            val city = request.destinationLocation.city

            // 2. Get effective tax rule for the tax category
            val taxRule = taxRuleRepository.getEffectiveRule(
                taxCode = request.taxCode,
                jurisdiction = state
            ) ?: return Result.failure(Exception("Tax rule not found for code: ${request.taxCode}"))

            // 3. Select scenario (B2B vs B2C)
            val scenario = when {
                request.transactionType == TransactionType.B2B && request.transactionContext.exemptionReason != null -> {
                    // B2B with exemption certificate
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
                            metadata = mapOf("exempt" to "true", "reason" to "B2B Tax Exempt")
                        )
                    )
                }
                else -> {
                    // Standard B2C or B2B without exemption
                    taxRule.componentComposition["standard"]
                        ?: return Result.failure(Exception("Standard composition not configured"))
                }
            }

            // 4. Calculate base amount
            val baseAmount = request.baseAmount * request.quantity

            // 5. Calculate each component (State, County, City, Special District)
            val components = mutableListOf<TaxComponentResult>()
            var totalTaxAmount = 0.0

            // Sort by order
            val sortedComponents = scenario.components.sortedBy { it.order }

            for (componentConfig in sortedComponents) {
                // Get workspace component details
                val workspaceComponent = taxComponentRepository.getWorkspaceComponentById(componentConfig.id)
                    ?: continue

                // Get component type
                val componentType = taxComponentRepository.getComponentTypeById(workspaceComponent.componentTypeId)
                    ?: continue

                // Filter by jurisdiction match (county and city must match if specified)
                if (!matchesJurisdiction(workspaceComponent.jurisdiction, state, county, city)) {
                    continue
                }

                // Calculate tax amount (USA sales tax is always on base, not compound)
                val taxAmount = baseAmount * (componentConfig.rate / 100.0)
                totalTaxAmount += taxAmount

                // Create component result
                components.add(
                    TaxComponentResult(
                        componentId = componentConfig.id,
                        componentName = componentConfig.name,
                        taxType = componentType.componentCode,
                        ratePercentage = componentConfig.rate,
                        taxableAmount = baseAmount,
                        taxAmount = taxAmount,
                        description = "${componentConfig.name} @ ${"%.3f".format(componentConfig.rate)}%",
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
                jurisdiction = request.destinationLocation,
                transactionContext = request.transactionContext,
                countryCode = countryCode,
                metadata = buildMetadata(state, county, city, components.size)
            )

            Result.success(result)

        } catch (e: Exception) {
            ErrorTracking.captureException(e, "USASalesTaxStrategy.calculateTax")
            Result.failure(e)
        }
    }

    override fun validateTaxCode(code: String, codeType: String): Boolean {
        // Basic validation - accept all codes for now
        return code.isNotBlank()
    }

    /**
     * Determine jurisdiction matching priority
     */
    private fun getJurisdictionPriority(componentId: String): Int {
        return when {
            componentId.contains("STATE", ignoreCase = true) -> 1
            componentId.contains("COUNTY", ignoreCase = true) -> 2
            componentId.contains("CITY", ignoreCase = true) -> 3
            componentId.contains("SPECIAL", ignoreCase = true) -> 4
            else -> 5
        }
    }

    /**
     * Check if jurisdiction matches the component
     */
    private fun matchesJurisdiction(
        componentJurisdiction: String,
        state: String,
        county: String?,
        city: String?
    ): Boolean {
        val parts = componentJurisdiction.split("|")

        // State must always match
        if (parts.getOrNull(0) != state) return false

        // If component specifies county, it must match
        if (parts.size > 1 && parts[1].isNotBlank() && county != null) {
            if (parts[1] != county) return false
        }

        // If component specifies city, it must match
        if (parts.size > 2 && parts[2].isNotBlank() && city != null) {
            if (parts[2] != city) return false
        }

        return true
    }

    /**
     * Build jurisdiction display string
     */
    private fun buildJurisdictionString(state: String, county: String?, city: String?): String {
        return buildList {
            add(state)
            county?.let { add(it) }
            city?.let { add(it) }
        }.joinToString(" | ")
    }

    /**
     * Build metadata for result
     */
    private fun buildMetadata(state: String, county: String?, city: String?, componentCount: Int): Map<String, String> {
        return mapOf(
            "state" to state,
            "county" to (county ?: "N/A"),
            "city" to (city ?: "N/A"),
            "jurisdiction_levels" to componentCount.toString(),
            "sourcing" to "destination_based"
        )
    }
}
