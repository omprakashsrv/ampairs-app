package com.ampairs.common.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey

/**
 * Android implementation of back navigation handler for Nav3.
 * Uses BackHandler for system back button integration.
 */
@Composable
actual fun BackNavigationHandlerNav3(
    backStack: MutableList<NavKey>,
    enabled: Boolean,
    fallbackRoute: NavKey
) {
    BackHandler(enabled = enabled && backStack.size > 1) {
        if (backStack.size > 1) {
            backStack.removeLastOrNull()
        } else {
            backStack.clear()
            backStack.add(fallbackRoute)
        }
    }
}
