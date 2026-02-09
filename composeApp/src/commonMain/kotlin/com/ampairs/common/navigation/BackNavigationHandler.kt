package com.ampairs.common.navigation

import androidx.compose.runtime.Composable

/**
 * Platform-specific back navigation handler.
 *
 * - Android: Uses Activity BackHandler for hardware back button
 * - iOS: Uses gesture-based navigation (swipe)
 * - Desktop: Handles keyboard shortcuts (Escape key)
 */
@Composable
expect fun PlatformBackHandler(
    enabled: Boolean,
    onBackPressed: () -> Unit,
)

/**
 * Platform-specific function to exit the app.
 */
expect fun ExitApp()
