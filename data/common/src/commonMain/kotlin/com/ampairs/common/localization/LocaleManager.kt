package com.ampairs.common.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import com.ampairs.common.config.AppPreferencesDataStore
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@Inject @SingleIn(AppScope::class)
class LocaleManager(
    private val appPreferences: AppPreferencesDataStore
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    val currentLanguageCode: StateFlow<String> = appPreferences.getLanguagePreference()
        .stateIn(scope, SharingStarted.Eagerly, "en")

    val currentLanguage: StateFlow<Language> = currentLanguageCode
        .map { Language.fromCode(it) }
        .stateIn(scope, SharingStarted.Eagerly, Language.ENGLISH)

    suspend fun setLanguage(language: Language) {
        appPreferences.setLanguagePreference(language.code)
        configureLocale(language.code)
    }

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

val LocalLocaleManager: ProvidableCompositionLocal<LocaleManager> =
    compositionLocalOf { error("LocalLocaleManager not provided") }

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
    val languageCode by localeManager.currentLanguageCode.collectAsStateWithLifecycle()

    PlatformLocaleConfiguration(languageCode) {
        CompositionLocalProvider(
            LocalLanguageCode provides languageCode,
        ) {
            // key() forces full subtree recreation when language changes so that
            // stringResource() re-reads Locale.getDefault() with the new locale.
            // LocalComposeEnvironment is internal in CMP — this is the official workaround.
            key(languageCode) {
                content()
            }
        }
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
