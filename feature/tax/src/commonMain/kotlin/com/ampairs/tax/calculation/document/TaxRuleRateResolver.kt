package com.ampairs.tax.calculation.document

import com.ampairs.tax.domain.model.TaxRule

/**
 * Bridges the synced [TaxRule.componentComposition] to the pure [ResolvedRate] the
 * [DocumentTotalsCalculator] consumes, picking the composition for the current [TaxScenario].
 *
 * Backend stores scenario keys in upper snake case (`INTRA_STATE` / `INTER_STATE`); a couple of
 * tolerant aliases are accepted. A null rule, missing scenario, or empty composition resolves to
 * [ResolvedRate.EXEMPT] (0% — the line is shown as exempt, never silently untaxed).
 */
object TaxRuleRateResolver {

    private val INTRA_KEYS = listOf("INTRA_STATE", "intra_state", "INTRA", "STANDARD", "standard")
    private val INTER_KEYS = listOf("INTER_STATE", "inter_state", "INTER")

    fun resolve(taxRule: TaxRule?, scenario: TaxScenario): ResolvedRate {
        if (taxRule == null) return ResolvedRate.EXEMPT
        val keys = if (scenario == TaxScenario.INTRA) INTRA_KEYS else INTER_KEYS
        val composition = keys.firstNotNullOfOrNull { taxRule.componentComposition[it] }
            ?: return ResolvedRate.EXEMPT
        if (composition.components.isEmpty()) return ResolvedRate.EXEMPT
        val components = composition.components
            .sortedBy { it.order }
            .map { TaxComponentRate(name = it.name, percentage = it.rate) }
        val totalRate = if (composition.totalRate > 0.0) composition.totalRate
        else components.sumOf { it.percentage }
        return ResolvedRate(components = components, totalRate = totalRate)
    }

    /** Build the taxCode -> ResolvedRate map the calculator needs for a set of rules. */
    fun resolveAll(rulesByTaxCode: Map<String, TaxRule?>, scenario: TaxScenario): Map<String, ResolvedRate> =
        rulesByTaxCode.mapValues { (_, rule) -> resolve(rule, scenario) }
}
