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
            // Fallback to temp directory if data directory not set (initialization phase)
            val tempDir = File(System.getProperty("java.io.tmpdir"), "ampairs/preferences")
            tempDir.mkdirs()
            println("WARNING: Using temporary preferences directory: ${tempDir.absolutePath}")
            File(tempDir, appDataStoreFileName).absolutePath
        }
    }
)