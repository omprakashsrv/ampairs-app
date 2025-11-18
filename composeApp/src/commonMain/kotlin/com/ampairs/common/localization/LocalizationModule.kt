package com.ampairs.common.localization

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/**
 * Koin module for localization dependencies
 */
val localizationModule = module {
    singleOf(::LocalizationManager)
}
