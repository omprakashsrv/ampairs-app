package com.ampairs.common.config

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import java.io.File

/**
 * Creates a DataStore instance for Desktop platform using the user-selected data directory
 */
fun createAppDataStore(): DataStore<Preferences> = createAppDataStore(
    producePath = {
        try {
            // Use DataDirectoryManager for consistent data location
            val prefsDir = com.ampairs.common.desktop.DataDirectoryManager.getPreferencesDir()
            File(prefsDir, appDataStoreFileName).absolutePath
        } catch (_: IllegalStateException) {
            // Fallback to user.home if data directory not set (initialization phase)
            val fallbackDir = File(System.getProperty("user.home"), ".ampairs/preferences")
            fallbackDir.mkdirs()
            println("WARNING: Using fallback preferences directory: ${fallbackDir.absolutePath}")
            File(fallbackDir, appDataStoreFileName).absolutePath
        }
    }
)