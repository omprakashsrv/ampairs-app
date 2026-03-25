package com.ampairs.common.theme

import com.ampairs.common.config.AppPreferencesDataStore
import kotlinx.coroutines.flow.Flow

/**
 * Repository for theme preferences with DataStore persistence
 */
class ThemeRepository(
    private val appPreferences: AppPreferencesDataStore
) {
    val themePreference: Flow<ThemePreference> = appPreferences.getThemePreference()

    suspend fun setThemePreference(preference: ThemePreference) {
        appPreferences.setThemePreference(preference)
        println("🎨 Theme preference saved to DataStore: $preference")
    }

    suspend fun getThemePreference(): ThemePreference {
        // This is handled by the Flow, but kept for compatibility
        return ThemePreference.SYSTEM
    }
}

