package com.ampairs.form.di

import com.ampairs.form.data.api.ConfigApi
import com.ampairs.form.data.api.ConfigApiImpl
import com.ampairs.form.data.db.FormDatabase
import com.ampairs.form.data.repository.ConfigRepository
import com.ampairs.form.ui.FormConfigViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Platform-specific module for form database
 * Implemented in androidMain, iosMain, desktopMain
 */
expect val formPlatformModule: org.koin.core.module.Module

/**
 * Koin module for form configuration dependencies
 */
val formModule = module {
    // Include platform-specific module
    includes(formPlatformModule)

    // DAOs
    factory { get<FormDatabase>().entityFieldConfigDao() }
    factory { get<FormDatabase>().entityAttributeDefinitionDao() }

    // API
    single<ConfigApi> { ConfigApiImpl(get(), get()) }

    // Repository
    factory<ConfigRepository> { ConfigRepository(get(), get(), get(), get()) }

    // ViewModels
    viewModelOf(::FormConfigViewModel)
}
