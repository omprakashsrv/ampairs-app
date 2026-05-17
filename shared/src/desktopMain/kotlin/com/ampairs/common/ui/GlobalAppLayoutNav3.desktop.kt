package com.ampairs.common.ui

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey

/**
 * Desktop implementation of back navigation handler for Nav3.
 * Desktop uses keyboard shortcuts handled in the parent window.
 */
@Composable
actual fun BackNavigationHandlerNav3(
    backStack: MutableList<NavKey>,
    enabled: Boolean,
    fallbackRoute: NavKey
) {
    // Desktop uses keyboard shortcuts (ESC) handled at window level
    // Navigation is handled via UI back button
}
