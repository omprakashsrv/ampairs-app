package com.ampairs.common.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue

/**
 * CompositionLocal for providing strings throughout the app
 */
val LocalStrings = compositionLocalOf<Strings> {
    EnglishStrings
}

/**
 * CompositionLocal for providing language throughout the app
 */
val LocalLanguage = compositionLocalOf<Language> {
    Language.ENGLISH
}

/**
 * Provider that sets up localization context for the app
 * Usage:
 * ```
 * LocalizationProvider(localizationManager) {
 *     // Your app content
 * }
 * ```
 */
@Composable
fun LocalizationProvider(
    localizationManager: LocalizationManager,
    content: @Composable () -> Unit
) {
    val currentLanguage by localizationManager.currentLanguage.collectAsState(Language.ENGLISH)
    val currentStrings by localizationManager.strings.collectAsState(EnglishStrings)

    CompositionLocalProvider(
        LocalLanguage provides currentLanguage,
        LocalStrings provides currentStrings
    ) {
        content()
    }
}

/**
 * Access localized strings in any Composable
 * Usage:
 * ```
 * val strings = localizedStrings()
 * Text(text = strings.appName)
 * ```
 */
@Composable
fun localizedStrings(): Strings = LocalStrings.current

/**
 * Access current language in any Composable
 */
@Composable
fun currentLanguage(): Language = LocalLanguage.current
