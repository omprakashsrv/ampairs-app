package com.ampairs.common.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Get a localized string that automatically updates when language changes
 *
 * This wrapper forces recomposition by reading LocalLanguageCode.current,
 * ensuring strings update immediately when the user switches language.
 */
@Composable
fun localizedString(resource: StringResource): String {
    // Read current language code to trigger recomposition on language change
    val languageCode = LocalLanguageCode.current

    // Remember the string based on the language code
    return remember(languageCode) {
        // This will be recalculated when languageCode changes
        resource
    }.let { stringResource(it) }
}

/**
 * Get a localized string with format arguments that updates when language changes
 */
@Composable
fun localizedString(resource: StringResource, vararg formatArgs: Any): String {
    // Read current language code to trigger recomposition on language change
    val languageCode = LocalLanguageCode.current

    // Remember the string based on the language code and format args
    return remember(languageCode, *formatArgs) {
        resource
    }.let { stringResource(it, *formatArgs) }
}
