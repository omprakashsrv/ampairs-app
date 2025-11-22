package com.ampairs.common.localization

import android.content.res.Configuration
import android.os.LocaleList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.ampairs.common.ActivityProvider
import java.util.Locale

/**
 * Android-specific locale configuration
 * Recreates the activity to properly reload resources with the new locale
 */
actual fun configureLocale(languageCode: String) {
    val activity = ActivityProvider.getActivity()
    activity?.recreate()
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

    // Create a new configuration with the selected locale
    val locale = Locale.forLanguageTag(languageCode)

    val config = Configuration(context.resources.configuration).apply {
        setLocales(LocaleList(locale))
    }

    // Create a new context with the updated configuration
    val localizedContext = context.createConfigurationContext(config)

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        content = content
    )
}
