package com.ampairs.tax.di

import com.ampairs.tax.calculation.ITaxCalculationStrategy
import com.ampairs.tax.calculation.TaxCalculationEngine
import com.ampairs.tax.calculation.strategy.IndiaGSTStrategy
import com.ampairs.tax.calculation.strategy.USASalesTaxStrategy
import com.ampairs.tax.calculation.strategy.UKVATStrategy
import com.ampairs.tax.calculation.strategy.EUVATStrategy
import com.ampairs.tax.calculation.strategy.CanadaGSTHSTStrategy
import com.ampairs.tax.calculation.strategy.AustraliaGSTStrategy
import com.ampairs.tax.calculation.strategy.DefaultTaxStrategy
import com.ampairs.tax.data.api.TaxConfigurationApi
import com.ampairs.tax.data.api.TaxConfigurationApiImpl
import com.ampairs.tax.data.db.TaxRoomDatabase
import com.ampairs.tax.data.repository.TaxCodeRepository
import com.ampairs.tax.data.repository.TaxComponentRepository
import com.ampairs.tax.data.repository.TaxConfigurationRepository
import com.ampairs.tax.data.repository.TaxRuleRepository
import com.ampairs.tax.domain.model.TaxStrategy
import com.ampairs.tax.ui.search.TaxCodeSearchViewModel
import com.ampairs.tax.ui.detail.TaxCodeDetailViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Tax Module V2 - Common Dependency Injection
 *
 * IMPORTANT: All workspace-aware components use factory scope
 * to ensure fresh instances on workspace switch
 */
val taxModule = module {
    // API Layer - Single instance with Ktor client injection
    single<TaxConfigurationApi> {
        TaxConfigurationApiImpl(
            engine = get(),
            tokenRepository = get()
        )
    }

    // DAOs - Factory scoped for workspace isolation
    factory {
        val database = get<TaxRoomDatabase>()
        database.taxCodeDao()
    }

    factory {
        val database = get<TaxRoomDatabase>()
        database.taxComponentTypeDao()
    }

    factory {
        val database = get<TaxRoomDatabase>()
        database.taxComponentDao()
    }

    factory {
        val database = get<TaxRoomDatabase>()
        database.taxRuleDao()
    }

    factory {
        val database = get<TaxRoomDatabase>()
        database.taxConfigurationDao()
    }

    // Repositories - Factory scoped for workspace isolation
    factoryOf(::TaxCodeRepository)
    factoryOf(::TaxComponentRepository)
    factoryOf(::TaxRuleRepository)
    factoryOf(::TaxConfigurationRepository)

    // Tax Calculation Strategies - Singletons (stateless)
    single {
        IndiaGSTStrategy(
            taxRuleRepository = get(),
            taxComponentRepository = get(),
        )
    }

    single {
        USASalesTaxStrategy(
            taxRuleRepository = get(),
            taxComponentRepository = get(),
        )
    }

    single {
        UKVATStrategy(
            taxRuleRepository = get(),
            taxComponentRepository = get(),
        )
    }

    single {
        EUVATStrategy(
            taxRuleRepository = get(),
            taxComponentRepository = get(),
        )
    }

    single {
        CanadaGSTHSTStrategy(
            taxRuleRepository = get(),
            taxComponentRepository = get(),
        )
    }

    single {
        AustraliaGSTStrategy(
            taxRuleRepository = get(),
            taxComponentRepository = get(),
        )
    }

    single {
        DefaultTaxStrategy(
            taxRuleRepository = get(),
            taxComponentRepository = get(),
        )
    }

    // Strategy Map - Single instance
    single<Map<TaxStrategy, ITaxCalculationStrategy>> {
        mapOf(
            TaxStrategy.INDIA_GST to get<IndiaGSTStrategy>(),
            TaxStrategy.USA_SALES_TAX to get<USASalesTaxStrategy>(),
            TaxStrategy.UK_VAT to get<UKVATStrategy>(),
            TaxStrategy.EU_VAT to get<EUVATStrategy>(),
            TaxStrategy.CANADA_GST_HST to get<CanadaGSTHSTStrategy>(),
            TaxStrategy.AUSTRALIA_GST to get<AustraliaGSTStrategy>(),
            TaxStrategy.DEFAULT_TAX to get<DefaultTaxStrategy>()
        )
    }

    // Tax Calculation Engine - Single instance
    single {
        TaxCalculationEngine(
            taxConfigRepository = get(),
            strategies = get()
        )
    }

    // ViewModels
    viewModelOf(::TaxCodeSearchViewModel)
    viewModelOf(::TaxCodeDetailViewModel)
}
