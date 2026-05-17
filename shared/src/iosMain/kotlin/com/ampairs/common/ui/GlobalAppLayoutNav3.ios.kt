package com.ampairs.common.ui

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey

/**
 * iOS implementation of back navigation handler for Nav3.
 * iOS uses swipe gestures and software back button (no hardware back).
 */
@Composable
actual fun BackNavigationHandlerNav3(
    backStack: MutableList<NavKey>,
    enabled: Boolean,
    fallbackRoute: NavKey
) {
    // iOS doesn't have a hardware back button
    // Navigation is handled via UI back button and swipe gestures
    // The swipe gesture handling would need to be implemented at the platform level
}
