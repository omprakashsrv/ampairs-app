// Copyright 2023, Christopher Banes
// SPDX-License-Identifier: Apache-2.0

package app.tivi.util

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import app.tivi.appinitializers.AppInitializer
import app.tivi.inject.ApplicationCoroutineScope
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Inject

@Inject
class CrashReportingInitializer(
    private val dataStore: Lazy<DataStore<Preferences>>,
    private val action: Lazy<SetCrashReportingEnabledAction>,
    private val scope: ApplicationCoroutineScope,
) : AppInitializer {

    override fun initialize() {

        scope.launchOrThrow {
            dataStore.value.data.map { preferences ->
                preferences[booleanPreferencesKey("report_app_crash")] ?: true
            }.collect { action.value(it) }
        }
    }
}
