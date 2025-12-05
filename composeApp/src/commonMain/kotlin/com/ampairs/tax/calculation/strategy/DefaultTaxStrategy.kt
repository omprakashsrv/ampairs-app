package com.ampairs.tax.calculation.strategy

import com.ampairs.common.sentry.ErrorTracking
import com.ampairs.tax.calculation.ITaxCalculationStrategy
import com.ampairs.tax.calculation.model.TaxCalculationRequest
import com.ampairs.tax.calculation.model.TaxCalculationResult
import com.ampairs.tax.calculation.model.TaxComponentResult
import com.ampairs.tax.data.repository.TaxRuleRepository
import com.ampairs.tax.data.repository.TaxComponentRepository

/**
 * Default Tax Strategy - Simple percentage-based fallback for any country
 *
 * Key Features:
 * - Generic percentage-based tax calculation
 * - Single or multiple tax components
 * - No special jurisdictional logic
 * - Tax exemption support
 * - Fallback for countries without specific strategies
 */
class DefaultTaxStrategy(
    private val taxRuleRepository: TaxRuleRepository,
    private val taxComponentRepository: TaxComponentRepository,
) : ITaxCalculationStrategy {

    override val countryCode: String = "DEFAULT"
    override val strategyName: String = "DEFAULT_TAX"

    override suspend fun calculateTax(request: TaxCalculationRequest): Result<TaxCalculationResult> {
        return try {
            // 1. Determine jurisdiction (use country or state if available)
            val jurisdiction = request.sourceLocation.state
                ?: request.sourceLocation.country
                ?: "DEFAULT"

            // 2. Get effective tax rule
            val taxRule = taxRuleRepository.getEffectiveRule(
                taxCode = request.taxCode,
                jurisdiction = jurisdiction
            ) ?: return Result.failure(Exception("Tax rule not found for code: ${request.taxCode}"))

            // 3. Handle tax-exempt transactions
            if (request.transactionContext.exemptionReason != null) {
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
                            "reason" to "Tax exempt transaction"
                        )
                    )
                )
            }

            // 4. Select scenario (standard by default)
            val scenario = taxRule.componentComposition.standard
                ?: return Result.failure(Exception("Standard composition not configured"))

            // 5. Calculate base amount
            val baseAmount = request.baseAmount * request.quantity

            // 6. Calculate tax components
            val components = mutableListOf<TaxComponentResult>()
            var totalTaxAmount = 0.0
            var compoundBase = baseAmount  // For compound tax calculation

            // Sort by order for proper compound calculation
            val sortedComponents = scenario.components.sortedBy { it.order }

            for (componentConfig in sortedComponents) {
                // Get workspace component details
                val workspaceComponent = taxComponentRepository.getWorkspaceComponentById(componentConfig.id)
                    ?: continue

                // Get component type
                val componentType = taxComponentRepository.getComponentTypeById(workspaceComponent.componentTypeId)
                    ?: continue

                // Determine taxable amount (base or compound)
                val taxableAmount = if (componentConfig.isCompound) {
                    compoundBase  // Tax on (base + previous taxes)
                } else {
                    baseAmount  // Tax on base only
                }

                // Calculate tax amount
                val taxAmount = taxableAmount * (componentConfig.rate / 100.0)
                totalTaxAmount += taxAmount

                // Update compound base for next component
                if (componentConfig.isCompound) {
                    compoundBase += taxAmount
                }

                // Create component result
                components.add(
                    TaxComponentResult(
                        componentId = componentConfig.id,
                        componentName = componentType.displayName,
                        taxType = componentType.componentCode,
                        ratePercentage = componentConfig.rate,
                        taxableAmount = taxableAmount,
                        taxAmount = taxAmount,
                        description = if (componentConfig.isCompound) {
                            "${componentType.displayName} @ ${"%.2f".format(componentConfig.rate)}% (compound)"
                        } else {
                            "${componentType.displayName} @ ${"%.2f".format(componentConfig.rate)}%"
                        },
                        isCompound = componentConfig.isCompound
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
                metadata = buildMetadata(jurisdiction, components)
            )

            Result.success(result)

        } catch (e: Exception) {
            ErrorTracking.captureException(e, "DefaultTaxStrategy.calculateTax")
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
    private fun buildMetadata(
        jurisdiction: String,
        components: List<TaxComponentResult>
    ): Map<String, String> {
        val totalRate = components.sumOf { it.ratePercentage }
        val hasCompound = components.any { it.isCompound }

        return mapOf(
            "jurisdiction" to jurisdiction,
            "total_rate" to "${"%.2f".format(totalRate)}%",
            "component_count" to components.size.toString(),
            "has_compound" to hasCompound.toString(),
            "strategy" to "default",
            "note" to "Generic percentage-based tax calculation"
        )
    }
}
