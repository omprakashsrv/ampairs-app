package com.ampairs.common.config

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import okio.Path.Companion.toPath

/**
 * Gets the singleton DataStore instance for app preferences (theme, settings, configs), creating it if necessary.
 */
fun createAppDataStore(producePath: () -> String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(
        produceFile = { producePath().toPath() }
    )

/**
 * DataStore file name for app preferences (theme, settings, configs)
 */
internal const val appDataStoreFileName = "app_preferences.preferences_pb"