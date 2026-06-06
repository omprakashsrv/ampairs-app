package com.ampairs.tax.calculation.document

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.tax.data.repository.TaxRuleRepository
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.first

/**
 * [TaxRateProvider] implementation backed by the workspace [TaxRuleRepository]. Bound in
 * WorkspaceScope because it reads workspace-scoped tax rules. Picks the rule whose composition
 * actually covers the requested scenario, falling back to EXEMPT when none applies.
 */
@Inject
@ContributesBinding(WorkspaceScope::class)
class TaxRateProviderImpl(
    private val taxRuleRepository: TaxRuleRepository,
) : TaxRateProvider {

    override suspend fun resolve(taxCode: String, scenario: TaxScenario): ResolvedRate {
        if (taxCode.isBlank()) return ResolvedRate.EXEMPT
        val rules = taxRuleRepository.observeTaxRulesByCode(taxCode).first()
        if (rules.isEmpty()) return ResolvedRate.EXEMPT
        // Prefer a rule that actually resolves a non-exempt rate for this scenario.
        val rule = rules.firstOrNull { TaxRuleRateResolver.resolve(it, scenario).totalRate > 0.0 }
            ?: rules.first()
        return TaxRuleRateResolver.resolve(rule, scenario)
    }
}
