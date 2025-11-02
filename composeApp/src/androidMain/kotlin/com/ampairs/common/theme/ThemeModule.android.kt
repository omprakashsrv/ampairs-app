package com.ampairs.common.theme

import android.content.Context
import com.ampairs.common.config.AppPreferencesDataStore
import com.ampairs.common.config.DataStoreAppPreferences
import com.ampairs.common.config.createAppDataStore
import org.koin.dsl.bind
import org.koin.dsl.module

val androidAppConfigModule = module {
    single<AppPreferencesDataStore> {
        DataStoreAppPreferences(
            dataStore = createAppDataStore(get<Context>())
        )
    } bind AppPreferencesDataStore::class
}