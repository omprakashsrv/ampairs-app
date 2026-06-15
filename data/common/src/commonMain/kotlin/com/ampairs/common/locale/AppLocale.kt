package com.ampairs.common.locale

import androidx.compose.runtime.compositionLocalOf

/**
 * The active workspace's localization settings, sourced from the business profile
 * (timezone / date_format / time_format / currency). Consumed by formatters at display time.
 *
 * Stored timestamps stay UTC everywhere; these settings only affect how values are *rendered*.
 */
data class AppLocale(
    val timeZoneId: String = "UTC",
    val dateFormat: String = "DD-MM-YYYY",
    val timeFormat: String = "12H",
    val currencyCode: String = "INR",
) {
    companion object {
        val Default = AppLocale()
    }
}

/**
 * Ambient [AppLocale] for the current workspace. Provided once near the navigation root from the
 * active workspace graph; defaults to [AppLocale.Default] (INR) outside that scope (login, previews).
 */
val LocalAppLocale = compositionLocalOf { AppLocale.Default }
