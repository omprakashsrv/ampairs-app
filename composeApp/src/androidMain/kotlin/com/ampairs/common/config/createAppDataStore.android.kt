package com.ampairs.common.config

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

/**
 * Creates a DataStore instance for Android platform using the app's files directory
 */
fun createAppDataStore(context: Context): DataStore<Preferences> = createAppDataStore(
    producePath = { context.filesDir.resolve(appDataStoreFileName).absolutePath }
)