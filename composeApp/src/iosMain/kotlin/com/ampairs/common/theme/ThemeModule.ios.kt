package com.ampairs.common.theme

import com.ampairs.common.config.AppPreferencesDataStore
import com.ampairs.common.config.DataStoreAppPreferences
import com.ampairs.common.config.createAppDataStore
import org.koin.dsl.bind
import org.koin.dsl.module

val iosAppConfigModule = module {
    single<AppPreferencesDataStore> {
        DataStoreAppPreferences(
            dataStore = createAppDataStore()
        )
    } bind AppPreferencesDataStore::class
}