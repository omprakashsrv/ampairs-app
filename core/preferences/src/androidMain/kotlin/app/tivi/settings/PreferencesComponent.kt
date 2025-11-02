// Copyright 2023, Google LLC, Christopher Banes
// SPDX-License-Identifier: Apache-2.0

package app.tivi.settings

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import app.tivi.inject.ApplicationScope
import me.tatarka.inject.annotations.Provides

actual interface PreferencesPlatformComponent {

    @ApplicationScope
    @Provides
    fun provideDataStore(context: Application): DataStore<Preferences> = createDataStore(
        producePath = { context.filesDir.resolve(dataStoreFileName).absolutePath }
    )
}

internal const val dataStoreFileName = "dice.preferences_pb"
