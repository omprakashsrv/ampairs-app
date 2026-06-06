package com.ampairs.tax.calculation.document

/**
 * Cross-module accessor that resolves a [ResolvedRate] for a tax code in the current
 * [TaxScenario] (spec 010). Lives in tax-api so order/invoice can compute GST without depending on
 * the heavy feature/tax module; the implementation in feature/tax reads the workspace tax rules.
 *
 * A blank/unknown tax code resolves to [ResolvedRate.EXEMPT] (0% — shown as exempt, never silently
 * untaxed).
 */
interface TaxRateProvider {
    suspend fun resolve(taxCode: String, scenario: TaxScenario): ResolvedRate

    /** Resolve a set of tax codes at once (deduped) for a whole document. */
    suspend fun resolveAll(taxCodes: List<String>, scenario: TaxScenario): Map<String, ResolvedRate> =
        taxCodes.filter { it.isNotBlank() }.distinct().associateWith { resolve(it, scenario) }
}
