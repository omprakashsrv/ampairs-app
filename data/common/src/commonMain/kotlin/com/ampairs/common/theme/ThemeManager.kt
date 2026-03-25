package com.ampairs.common.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import com.ampairs.common.concurrency.Volatile
import com.ampairs.common.concurrency.synchronized

/**
 * Theme manager for handling app-wide theme state with DataStore persistence
 */
class ThemeManager(
    private val repository: ThemeRepository
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    val themePreference: StateFlow<ThemePreference> = repository.themePreference.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = ThemePreference.LIGHT
    )

    fun setThemePreference(preference: ThemePreference) {
        scope.launch {
            repository.setThemePreference(preference)
        }
    }

    @Composable
    fun isDarkTheme(): Boolean {
        val systemInDarkTheme = isSystemInDarkTheme()
        val preference by themePreference.collectAsState()

        return when (preference) {
            ThemePreference.SYSTEM -> systemInDarkTheme
            ThemePreference.LIGHT -> false
            ThemePreference.DARK -> true
        }
    }
}