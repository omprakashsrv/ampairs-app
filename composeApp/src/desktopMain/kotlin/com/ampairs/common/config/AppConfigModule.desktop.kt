package com.ampairs.common.config

import com.ampairs.common.config.AppPreferencesDataStore
import com.ampairs.common.config.DataStoreAppPreferences
import com.ampairs.common.config.createAppDataStore
import org.koin.dsl.bind
import org.koin.dsl.module

val desktopAppConfigModule = module {
    // Use factory instead of single to ensure each workspace gets its own preferences
    factory<AppPreferencesDataStore> {
        DataStoreAppPreferences(
            dataStore = createAppDataStore()
        )
    } bind AppPreferencesDataStore::class
}