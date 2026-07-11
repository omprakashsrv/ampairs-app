package com.ampairs.tax.di

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.tax.calculation.ITaxCalculationStrategy
import com.ampairs.tax.calculation.strategy.AustraliaGSTStrategy
import com.ampairs.tax.calculation.strategy.CanadaGSTHSTStrategy
import com.ampairs.tax.calculation.strategy.DefaultTaxStrategy
import com.ampairs.tax.calculation.strategy.EUVATStrategy
import com.ampairs.tax.calculation.strategy.IndiaGSTStrategy
import com.ampairs.tax.calculation.strategy.UKVATStrategy
import com.ampairs.tax.calculation.strategy.USASalesTaxStrategy
import com.ampairs.tax.data.repository.TaxCodeLookup
import com.ampairs.tax.data.repository.TaxCodeRepository
import com.ampairs.tax.domain.model.TaxStrategy
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

// Replaced Koin taxModule. Injectable classes are annotated with @Inject directly:
// - TaxConfigurationApiImpl: @Inject @SingleIn(AppScope) @ContributesBinding
// - TaxCodeRepository, TaxComponentRepository, TaxRuleRepository, TaxConfigurationRepository: @Inject
// - IndiaGSTStrategy, USASalesTaxStrategy, UKVATStrategy, EUVATStrategy,
//   CanadaGSTHSTStrategy, AustraliaGSTStrategy, DefaultTaxStrategy: @Inject
// - TaxCalculationEngine: @Inject
// - All ViewModels: @Inject
// Tax DAOs are provided by the consolidated workspace database module (:data:database).

@ContributesTo(WorkspaceScope::class)
interface TaxServiceModule {
    companion object {
        @Provides
        fun provideTaxCodeLookup(repo: TaxCodeRepository): TaxCodeLookup = repo
    }
}


@ContributesTo(WorkspaceScope::class)
interface TaxStrategyModule {
    companion object {
        @Provides
        @SingleIn(WorkspaceScope::class)
        fun provideTaxStrategiesMap(
            indiaGST: IndiaGSTStrategy,
            usaSalesTax: USASalesTaxStrategy,
            ukVAT: UKVATStrategy,
            euVAT: EUVATStrategy,
            canadaGSTHST: CanadaGSTHSTStrategy,
            australiaGST: AustraliaGSTStrategy,
            defaultTax: DefaultTaxStrategy
        ): Map<TaxStrategy, ITaxCalculationStrategy> = mapOf(
            TaxStrategy.INDIA_GST to indiaGST,
            TaxStrategy.USA_SALES_TAX to usaSalesTax,
            TaxStrategy.UK_VAT to ukVAT,
            TaxStrategy.EU_VAT to euVAT,
            TaxStrategy.CANADA_GST_HST to canadaGSTHST,
            TaxStrategy.AUSTRALIA_GST to australiaGST,
            TaxStrategy.DEFAULT_TAX to defaultTax
        )
    }
}

fun taxModule() = Unit
