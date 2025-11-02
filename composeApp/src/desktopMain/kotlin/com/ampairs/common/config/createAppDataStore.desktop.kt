package com.ampairs.common.config

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import java.io.File

/**
 * Creates a DataStore instance for Desktop platform using the user's home directory
 */
fun createAppDataStore(): DataStore<Preferences> = createAppDataStore(
    producePath = {
        // Create .ampairs directory in user home for app data
        val userHome = System.getProperty("user.home")
        val ampairsDir = File(userHome, ".ampairs")
        if (!ampairsDir.exists()) {
            ampairsDir.mkdirs()
        }
        File(ampairsDir, appDataStoreFileName).absolutePath
    }
)