package com.ampairs.tax.calculation.strategy

import com.ampairs.common.sentry.ErrorTracking
import com.ampairs.tax.calculation.ITaxCalculationStrategy
import com.ampairs.tax.calculation.model.TaxCalculationRequest
import com.ampairs.tax.calculation.model.TaxCalculationResult
import com.ampairs.tax.calculation.model.TaxComponentResult
import com.ampairs.tax.data.repository.TaxRuleRepository
import com.ampairs.tax.data.repository.TaxComponentRepository

/**
 * India GST Strategy - CGST + SGST (intra-state) / IGST (inter-state) + CESS
 */
class IndiaGSTStrategy(
    private val taxRuleRepository: TaxRuleRepository,
    private val taxComponentRepository: TaxComponentRepository,
) : ITaxCalculationStrategy {

    override val countryCode: String = "IN"
    override val strategyName: String = "INDIA_GST"

    override suspend fun calculateTax(request: TaxCalculationRequest): Result<TaxCalculationResult> {
        return try {
            // 1. Determine jurisdiction
            val jurisdiction = request.sourceLocation.state
                ?: return Result.failure(Exception("Source state is required for India GST"))

            // 2. Get effective tax rule
            val taxRule = taxRuleRepository.getEffectiveRule(
                taxCode = request.taxCode,
                jurisdiction = jurisdiction
            ) ?: return Result.failure(Exception("Tax rule not found for code: ${request.taxCode}"))

            // 3. Determine if intra-state or inter-state
            val isIntraState = request.sourceLocation.state == request.destinationLocation.state

            // 4. Get applicable scenario from composition map
            val scenarioKey = if (isIntraState) "intra_state" else "inter_state"
            val scenario = taxRule.componentComposition[scenarioKey]
                ?: return Result.failure(Exception("$scenarioKey composition not configured"))

            // 5. Calculate base amount
            val baseAmount = request.baseAmount * request.quantity

            // 6. Calculate each component
            val components = mutableListOf<TaxComponentResult>()
            var totalTaxAmount = 0.0

            // Sort components by calculation order
            val sortedComponents = scenario.components.sortedBy { it.order }

            for (componentConfig in sortedComponents) {
                // Get workspace component details
                val workspaceComponent = taxComponentRepository.getWorkspaceComponentById(componentConfig.id)
                    ?: continue

                // Get component type for display name
                val componentType = taxComponentRepository.getComponentTypeById(workspaceComponent.componentTypeId)
                    ?: continue

                // Calculate taxable amount (base amount only, no compound tax for GST)
                val taxableAmount = baseAmount

                // Calculate tax amount using rate from component reference
                val taxAmount = taxableAmount * (componentConfig.rate / 100.0)

                // Add to total
                totalTaxAmount += taxAmount

                // Create component result
                components.add(
                    TaxComponentResult(
                        componentId = componentConfig.id,
                        componentName = componentConfig.name,
                        taxType = componentType.componentCode,
                        ratePercentage = componentConfig.rate,
                        taxableAmount = taxableAmount,
                        taxAmount = taxAmount,
                        description = "${componentConfig.name} @ ${"%.2f".format(componentConfig.rate)}%",
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
                metadata = mapOf(
                    "is_intra_state" to isIntraState.toString(),
                    "source_state" to (request.sourceLocation.state ?: ""),
                    "destination_state" to (request.destinationLocation.state ?: ""),
                    "tax_rule_id" to taxRule.id
                )
            )

            Result.success(result)
        } catch (e: Exception) {
            ErrorTracking.captureException(e, "IndiaGSTStrategy.calculateTax")
            Result.failure(e)
        }
    }

    override fun validateTaxCode(code: String, codeType: String): Boolean {
        return when (codeType) {
            "HSN_CODE" -> {
                // HSN codes are 4-8 digits
                code.matches(Regex("^\\d{4,8}$"))
            }
            "SAC_CODE" -> {
                // SAC codes are 6 digits
                code.matches(Regex("^\\d{6}$"))
            }
            else -> false
        }
    }
}
