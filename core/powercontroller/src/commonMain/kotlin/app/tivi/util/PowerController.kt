// Copyright 2020, Google LLC, Christopher Banes
// SPDX-License-Identifier: Apache-2.0

package app.tivi.util

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Inject

interface PowerController {
    suspend fun shouldSaveData(): SaveData
}

sealed class SaveData {
    data object Disabled : SaveData()
    data class Enabled(val reason: SaveDataReason) : SaveData()
}

enum class SaveDataReason {
    PREFERENCE,
    SYSTEM_DATA_SAVER,
    SYSTEM_POWER_SAVER,
}

@Inject
class DefaultPowerController(
    private val dataStore: Lazy<DataStore<Preferences>>,
) : PowerController {

    override suspend fun shouldSaveData(): SaveData =
        dataStore.value.data.map { preferences ->
            preferences[booleanPreferencesKey("use_less_data")] ?: false
        }.first().let {
            if (it) {
                SaveData.Enabled(SaveDataReason.PREFERENCE)
            } else {
                SaveData.Disabled
            }
        }
}
