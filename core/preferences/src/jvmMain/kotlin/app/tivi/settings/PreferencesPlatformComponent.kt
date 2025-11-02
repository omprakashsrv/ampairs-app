// Copyright 2023, Christopher Banes
// SPDX-License-Identifier: Apache-2.0

package app.tivi.settings

import androidx.datastore.core.DataStore
import app.tivi.inject.ApplicationScope
import me.tatarka.inject.annotations.Provides
import java.io.File

actual interface PreferencesPlatformComponent {

    @ApplicationScope
    @Provides
    fun provideDataStore(): DataStore<androidx.datastore.preferences.core.Preferences> =
        createDataStore(
            producePath = { getPreferencesDir().absolutePath }
        )
}

private fun getPreferencesDir(): File {
    return try {
        com.ampairs.common.desktop.DataDirectoryManager.getPreferencesDir()
    } catch (e: IllegalStateException) {
        // Fallback to user.home for early initialization
        val fallbackDir = File(System.getProperty("user.home"), ".ampairs/preferences")
        fallbackDir.mkdirs()
        println("WARNING: Using fallback preferences directory: ${fallbackDir.absolutePath}")
        fallbackDir
    }
}


internal const val dataStoreFileName = "dice.preferences_pb"
