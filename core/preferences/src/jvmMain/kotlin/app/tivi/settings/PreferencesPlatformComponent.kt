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
            producePath = { getDatabaseDir().absolutePath }
        )
}

private fun getDatabaseDir(): File {
    val ApplicationName = "ampairs"
    return when (currentOperatingSystem) {
        OperatingSystem.Windows -> File(System.getenv("AppData"), "$ApplicationName")
        OperatingSystem.Linux -> File(System.getProperty("user.home"), "$ApplicationName")
        OperatingSystem.MacOS -> File(System.getProperty("user.home"), "$ApplicationName")
        else -> throw IllegalStateException("Unsupported operating system")
    }
}


internal const val dataStoreFileName = "dice.preferences_pb"
