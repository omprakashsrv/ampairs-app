package com.ampairs.common.navigation

import androidx.compose.runtime.Composable

/**
 * iOS-specific implementation of back navigation handling
 * iOS doesn't have a hardware back button, so this is typically a no-op
 * Navigation is usually handled by the navigation controller's built-in back button
 */
@Composable
actual fun PlatformBackHandler(
    enabled: Boolean,
    onBackPressed: () -> Unit,
) {
    // iOS doesn't have a hardware back button like Android
    // Back navigation is typically handled by the navigation bar's back button
    // This could be extended to handle swipe gestures if needed
}

/**
 * iOS-specific implementation to exit the app
 * Note: Apple discourages programmatic app exit
 */
actual fun ExitApp() {
    // iOS doesn't support programmatic app exit
    // Users should use home button/gesture
    // Apple guidelines discourage calling exit() programmatically
}