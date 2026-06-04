package com.ampairs.common.config

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import java.io.File

fun createAppDataStore(): DataStore<Preferences> {
    return createAppDataStore(
        producePath = {
            val prefsDir = try {
                com.ampairs.common.desktop.DataDirectoryManager.getPreferencesDir()
            } catch (_: IllegalStateException) {
                File(System.getProperty("user.home"), ".ampairs/preferences").also { it.mkdirs() }
            }
            File(prefsDir, appDataStoreFileName).absolutePath
        }
    )
}
