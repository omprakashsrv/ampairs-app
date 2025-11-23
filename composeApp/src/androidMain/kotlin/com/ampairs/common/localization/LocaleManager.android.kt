package com.ampairs.common.localization

import android.content.res.Configuration
import android.os.LocaleList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.ampairs.common.ActivityProvider
import java.util.Locale

/**
 * Android-specific locale configuration
 * Updates the default locale for immediate effect
 */
actual fun configureLocale(languageCode: String) {
    val locale = Locale.forLanguageTag(languageCode)
    Locale.setDefault(locale)
}

/**
 * Android-specific locale configuration composable
 * Creates a new Configuration with the selected locale and provides it to Compose
 */
@Composable
actual fun PlatformLocaleConfiguration(
    languageCode: String,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val currentConfig = LocalConfiguration.current

    // Create locale from language code
    val locale = remember(languageCode) {
        Locale.forLanguageTag(languageCode).also {
            Locale.setDefault(it)
        }
    }

    // Create a new configuration with the selected locale
    val config = remember(languageCode, currentConfig) {
        Configuration(currentConfig).apply {
            setLocales(LocaleList(locale))
        }
    }

    // Create a new context with the updated configuration
    val localizedContext = remember(languageCode) {
        context.createConfigurationContext(config)
    }

    // Provide both LocalContext and LocalConfiguration with updated locale
    // LocalConfiguration is what Compose Resources uses to determine the locale
    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides config,
        content = content
    )
}
