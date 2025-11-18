package com.ampairs.common.localization

import android.content.res.Configuration
import android.os.LocaleList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * Android-specific locale configuration
 */
actual fun configureLocale(languageCode: String) {
    // On Android, locale is managed through Configuration
    // This will be applied in PlatformLocaleConfiguration composable
}

/**
 * Android-specific locale configuration composable
 * Creates a new Configuration with the selected locale
 */
@Composable
actual fun PlatformLocaleConfiguration(
    languageCode: String,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val currentConfig = LocalConfiguration.current

    // Create a new configuration with the selected locale
    val locale =
        Locale.forLanguageTag(languageCode)

    val config = Configuration(currentConfig).apply {
        // Use LocaleList for API 24+
        setLocales(LocaleList(locale))
    }

    // Create a new context with the updated configuration
    val localizedContext = context.createConfigurationContext(config)

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides config,
        content = content
    )
}
