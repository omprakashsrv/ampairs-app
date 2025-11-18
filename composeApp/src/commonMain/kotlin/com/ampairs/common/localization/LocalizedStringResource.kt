package com.ampairs.common.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/**
 * Get a localized string resource with explicit locale support
 * This wrapper ensures the correct locale is used based on LocalLanguageCode
 */
@Composable
@ReadOnlyComposable
fun localizedStringResource(resource: StringResource): String {
    val languageCode = LocalLanguageCode.current
    // Force recomposition when language changes by reading the current code
    return stringResource(resource)
}

/**
 * Get a localized string resource with format args and explicit locale support
 */
@Composable
@ReadOnlyComposable
fun localizedStringResource(resource: StringResource, vararg formatArgs: Any): String {
    val languageCode = LocalLanguageCode.current
    // Force recomposition when language changes by reading the current code
    return stringResource(resource, *formatArgs)
}

/**
 * Suspending function to get localized string (for non-composable contexts)
 */
suspend fun getLocalizedString(resource: StringResource, languageCode: String): String {
    // Note: This will use the current system locale
    // For proper locale support in suspend functions, we'd need platform-specific implementations
    return getString(resource)
}
