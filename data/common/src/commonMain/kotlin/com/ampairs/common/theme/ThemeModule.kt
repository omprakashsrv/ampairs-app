package com.ampairs.common.theme

import org.koin.core.module.Module
import org.koin.dsl.module

val themeModule: Module = module {
    // AppPreferencesDataStore is provided by platform-specific modules
    single { ThemeRepository(get()) }
    single { ThemeManager(get()) }
}