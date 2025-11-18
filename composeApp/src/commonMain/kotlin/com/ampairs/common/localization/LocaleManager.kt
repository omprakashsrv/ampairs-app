package com.ampairs.common.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import com.ampairs.common.config.AppPreferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Manages application locale and language preferences
 */
class LocaleManager(
    private val appPreferences: AppPreferencesDataStore
) {
    /**
     * Current language code flow (e.g., "en", "hi")
     */
    val currentLanguageCode: Flow<String> = appPreferences.getLanguagePreference()

    /**
     * Current language as enum
     */
    val currentLanguage: Flow<Language> = currentLanguageCode.map { code ->
        Language.fromCode(code)
    }

    /**
     * Set the application language
     */
    suspend fun setLanguage(language: Language) {
        appPreferences.setLanguagePreference(language.code)
    }

    /**
     * Set language by code
     */
    suspend fun setLanguageCode(code: String) {
        appPreferences.setLanguagePreference(code)
    }
}

/**
 * CompositionLocal for current language code
 */
val LocalLanguageCode = compositionLocalOf { "en" }

/**
 * Provider for locale/language management in Compose
 */
@Composable
fun LocaleProvider(
    localeManager: LocaleManager,
    content: @Composable () -> Unit
) {
    val languageCode by localeManager.currentLanguageCode.collectAsState("en")

    CompositionLocalProvider(
        LocalLanguageCode provides languageCode,
        content = content
    )
}

/**
 * Get current language code from composition
 */
@Composable
fun currentLanguageCode(): String = LocalLanguageCode.current
