package com.ampairs.common.config

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ampairs.common.theme.ThemePreference
import com.ampairs.common.workspace.WorkspaceContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStore-based implementation for app preferences (theme, settings, configs)
 * Works across all platforms with proper file system persistence
 */
class DataStoreAppPreferences(
    private val dataStore: DataStore<Preferences>
) : AppPreferencesDataStore {

    companion object {
        private val THEME_PREFERENCE_KEY = stringPreferencesKey("theme_preference")

        // Workspace-aware preference keys
        // Note: These keys include workspace slug to maintain separate state per workspace
        private fun getCustomerLastSyncTimeKey(workspaceSlug: String) =
            stringPreferencesKey("customer_last_sync_time_$workspaceSlug")

        private fun getFormConfigLastSyncTimeKey(workspaceSlug: String) =
            stringPreferencesKey("form_config_last_sync_time_$workspaceSlug")

        // Future preference keys can be added here:
        // private val LANGUAGE_PREFERENCE_KEY = stringPreferencesKey("language_preference")
        // private val NOTIFICATION_SETTINGS_KEY = stringPreferencesKey("notification_settings")
    }

    override fun getThemePreference(): Flow<ThemePreference> {
        return dataStore.data.map { preferences ->
            val preferenceString = preferences[THEME_PREFERENCE_KEY] ?: DEFAULT_THEME_PREFERENCE
            try {
                ThemePreference.valueOf(preferenceString)
            } catch (e: IllegalArgumentException) {
                println("⚠️ Invalid theme preference '$preferenceString', using default")
                ThemePreference.SYSTEM
            }
        }
    }

    override suspend fun setThemePreference(preference: ThemePreference) {
        dataStore.edit { preferences ->
            preferences[THEME_PREFERENCE_KEY] = preference.name
        }
    }

    override fun getCustomerLastSyncTime(): Flow<String> {
        return dataStore.data.map { preferences ->
            val workspaceSlug = WorkspaceContext.getCurrentWorkspaceSlugOrDefault()
            val key = getCustomerLastSyncTimeKey(workspaceSlug)
            preferences[key] ?: "" // Default to empty string (sync all on first run)
        }
    }

    override suspend fun setCustomerLastSyncTime(timestamp: String) {
        val workspaceSlug = WorkspaceContext.getCurrentWorkspaceSlugOrDefault()
        val key = getCustomerLastSyncTimeKey(workspaceSlug)

        dataStore.edit { preferences ->
            preferences[key] = timestamp
        }
    }

    override fun getFormConfigLastSyncTime(): Flow<String> {
        return dataStore.data.map { preferences ->
            val workspaceSlug = WorkspaceContext.getCurrentWorkspaceSlugOrDefault()
            val key = getFormConfigLastSyncTimeKey(workspaceSlug)
            preferences[key] ?: "" // Default to empty string (sync all on first run)
        }
    }

    override suspend fun setFormConfigLastSyncTime(timestamp: String) {
        val workspaceSlug = WorkspaceContext.getCurrentWorkspaceSlugOrDefault()
        val key = getFormConfigLastSyncTimeKey(workspaceSlug)

        dataStore.edit { preferences ->
            preferences[key] = timestamp
        }
    }
}