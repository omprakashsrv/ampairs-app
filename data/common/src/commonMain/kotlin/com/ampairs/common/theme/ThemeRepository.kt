package com.ampairs.common.theme

import com.ampairs.common.config.AppPreferencesDataStore
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow

@Inject @SingleIn(AppScope::class)
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

