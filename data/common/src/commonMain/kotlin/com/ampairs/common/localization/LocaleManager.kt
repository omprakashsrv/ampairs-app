package com.ampairs.common.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import com.ampairs.common.config.AppPreferencesDataStore
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Inject @SingleIn(AppScope::class)
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
        // Platform-specific locale configuration will be triggered
        configureLocale(language.code)
    }

    /**
     * Set language by code
     */
    suspend fun setLanguageCode(code: String) {
        appPreferences.setLanguagePreference(code)
        configureLocale(code)
    }
}

/**
 * Platform-specific locale configuration
 * This should be implemented using expect/actual pattern for each platform
 */
expect fun configureLocale(languageCode: String)

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

    // Apply platform-specific locale configuration
    PlatformLocaleConfiguration(languageCode) {
        CompositionLocalProvider(
            LocalLanguageCode provides languageCode,
            content = content
        )
    }
}

/**
 * Platform-specific locale configuration composable
 */
@Composable
expect fun PlatformLocaleConfiguration(
    languageCode: String,
    content: @Composable () -> Unit
)

/**
 * Get current language code from composition
 */
@Composable
fun currentLanguageCode(): String = LocalLanguageCode.current
