package com.ampairs.common.localization

import androidx.compose.runtime.Composable
import platform.Foundation.NSLocale
import platform.Foundation.NSUserDefaults

/**
 * iOS-specific locale configuration
 * Sets the user defaults for AppleLanguages
 */
actual fun configureLocale(languageCode: String) {
    // On iOS, we set the preferred language in NSUserDefaults
    // This affects resource loading
    val userDefaults = NSUserDefaults.standardUserDefaults
    userDefaults.setObject(listOf(languageCode), forKey = "AppleLanguages")
    userDefaults.synchronize()
}

/**
 * iOS-specific locale configuration composable
 * On iOS, we rely on NSLocale and resource bundle loading
 */
@Composable
actual fun PlatformLocaleConfiguration(
    languageCode: String,
    content: @Composable () -> Unit
) {
    // Apply locale configuration
    configureLocale(languageCode)

    // Simply render the content - iOS will use the configured locale
    content()
}
