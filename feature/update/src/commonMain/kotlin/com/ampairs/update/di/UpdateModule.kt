package com.ampairs.update.di

import com.ampairs.update.api.UpdateApi
import com.ampairs.update.api.UpdateApiImpl
import com.ampairs.update.service.UpdateChecker
import com.ampairs.update.service.UpdateDownloader
import com.ampairs.update.service.UpdateInstaller
import org.koin.dsl.module

/**
 * Koin module for update system dependencies
 */
val updateModule = module {
    // API
    factory<UpdateApi> {
        UpdateApiImpl(
            engine = get(),
            tokenRepository = get()
        )
    }

    // Services
    factory {
        UpdateChecker(
            updateApi = get(),
            appPreferences = get()
        )
    }

    factory {
        UpdateDownloader()
    }

    factory {
        UpdateInstaller()
    }
}
