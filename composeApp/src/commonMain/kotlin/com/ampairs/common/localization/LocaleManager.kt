package com.ampairs.common.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import com.ampairs.common.config.AppPreferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.LanguageQualifier
import org.jetbrains.compose.resources.RegionQualifier
import org.jetbrains.compose.resources.ResourceEnvironment

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
 * CompositionLocal for ResourceEnvironment with language configuration
 */
@OptIn(InternalResourceApi::class)
val LocalResourceEnvironment = compositionLocalOf<ResourceEnvironment> {
    ResourceEnvironment(
        language = LanguageQualifier("en"),
        region = RegionQualifier.EMPTY
    )
}

/**
 * Provider for locale/language management in Compose
 */
@OptIn(InternalResourceApi::class)
@Composable
fun LocaleProvider(
    localeManager: LocaleManager,
    content: @Composable () -> Unit
) {
    val languageCode by localeManager.currentLanguageCode.collectAsState("en")

    val resourceEnvironment = ResourceEnvironment(
        language = LanguageQualifier(languageCode),
        region = RegionQualifier.EMPTY
    )

    CompositionLocalProvider(
        LocalLanguageCode provides languageCode,
        LocalResourceEnvironment provides resourceEnvironment,
        org.jetbrains.compose.resources.LocalResourceEnvironment provides resourceEnvironment,
        content = content
    )
}

/**
 * Get current language code from composition
 */
@Composable
fun currentLanguageCode(): String = LocalLanguageCode.current
