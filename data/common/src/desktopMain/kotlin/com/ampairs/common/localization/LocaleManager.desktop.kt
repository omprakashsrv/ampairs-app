package com.ampairs.common.localization

import androidx.compose.runtime.Composable
import java.util.Locale

/**
 * Desktop-specific locale configuration
 * Sets the default JVM locale
 */
actual fun configureLocale(languageCode: String) {
    val locale = Locale(languageCode)
    Locale.setDefault(locale)
}

/**
 * Desktop-specific locale configuration composable
 * On Desktop, we set the default locale globally
 */
@Composable
actual fun PlatformLocaleConfiguration(
    languageCode: String,
    content: @Composable () -> Unit
) {
    // Apply locale
    val locale = Locale(languageCode)
    Locale.setDefault(locale)

    // Simply render the content
    content()
}
