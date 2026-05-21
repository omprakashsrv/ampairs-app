package com.ampairs.tax.calculation.strategy

import com.ampairs.common.sentry.ErrorTracking
import com.ampairs.tax.util.formatDecimal
import com.ampairs.tax.calculation.ITaxCalculationStrategy
import com.ampairs.tax.calculation.model.TaxCalculationRequest
import com.ampairs.tax.calculation.model.TaxCalculationResult
import com.ampairs.tax.calculation.model.TaxComponentResult
import com.ampairs.tax.data.repository.TaxRuleRepository
import com.ampairs.tax.data.repository.TaxComponentRepository
import dev.zacsweers.metro.Inject

/**
 * India GST Strategy - CGST + SGST (intra-state) / IGST (inter-state) + CESS
 */
@Inject
class IndiaGSTStrategy(
    private val taxRuleRepository: TaxRuleRepository,
    private val taxComponentRepository: TaxComponentRepository,
) : ITaxCalculationStrategy {

    override val countryCode: String = "IN"
    override val strategyName: String = "INDIA_GST"

    override suspend fun calculateTax(request: TaxCalculationRequest): Result<TaxCalculationResult> {
        return try {
            // 1. Determine jurisdiction
            val sourceState = request.sourceLocation.state
                ?: return Result.failure(Exception("Source state is required for India GST"))

            // 2. Get effective tax rule - Try state first, then fall back to country
            // (Backend may store rules at country level with scenario-based composition)
            var taxRule = taxRuleRepository.getEffectiveRule(
                taxCode = request.taxCode,
                jurisdiction = sourceState
            )

            // Fall back to country code if state-level not found
            if (taxRule == null) {
                taxRule = taxRuleRepository.getEffectiveRule(
                    taxCode = request.taxCode,
                    jurisdiction = request.sourceLocation.country
                )
            }

            // Fall back to country name if country code not found
            if (taxRule == null && request.sourceLocation.country == "IN") {
                taxRule = taxRuleRepository.getEffectiveRule(
                    taxCode = request.taxCode,
                    jurisdiction = "INDIA"
                )
            }

            if (taxRule == null) {
                return Result.failure(Exception("Tax rule not found for code: ${request.taxCode}, state: $sourceState, country: ${request.sourceLocation.country}"))
            }

            // 3. Determine if intra-state or inter-state
            val isIntraState = request.sourceLocation.state == request.destinationLocation.state

            // 4. Get applicable scenario from composition map
            // Note: Backend uses UPPERCASE scenario keys
            val scenarioKey = if (isIntraState) "INTRA_STATE" else "INTER_STATE"
            val scenario = taxRule.componentComposition[scenarioKey]
                ?: return Result.failure(Exception("$scenarioKey composition not configured. Available keys: ${taxRule.componentComposition.keys}"))

            // 5. Calculate base amount
            val baseAmount = request.baseAmount * request.quantity

            // 6. Calculate each component
            val components = mutableListOf<TaxComponentResult>()
            var totalTaxAmount = 0.0

            // Sort components by calculation order
            val sortedComponents = scenario.components.sortedBy { it.order }

            println("🧮 [IndiaGST] Calculating tax for ${sortedComponents.size} components")

            for (componentConfig in sortedComponents) {
                println("🧮 [IndiaGST] Processing component: id=${componentConfig.id}, name=${componentConfig.name}, rate=${componentConfig.rate}")

                // Calculate taxable amount (base amount only, no compound tax for GST)
                val taxableAmount = baseAmount

                // Calculate tax amount using rate from component reference
                val taxAmount = taxableAmount * (componentConfig.rate / 100.0)

                // Add to total
                totalTaxAmount += taxAmount

                // Create component result using data from the tax rule's component reference
                // The component composition already has all needed info: id, name, rate
                components.add(
                    TaxComponentResult(
                        componentId = componentConfig.id,
                        componentName = componentConfig.name,
                        taxType = componentConfig.name, // Use name as type (CGST, SGST, IGST)
                        ratePercentage = componentConfig.rate,
                        taxableAmount = taxableAmount,
                        taxAmount = taxAmount,
                        description = "${componentConfig.name} @ ${componentConfig.rate.formatDecimal(2)}%",
                        isCompound = false
                    )
                )

                println("✅ [IndiaGST] Added component: ${componentConfig.name}, taxAmount=$taxAmount, runningTotal=$totalTaxAmount")
            }

            println("🧮 [IndiaGST] Final calculation: baseAmount=$baseAmount, totalTax=$totalTaxAmount, components=${components.size}")

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
