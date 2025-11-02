package com.ampairs.common.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

/**
 * Android-specific implementation using Activity BackHandler
 * Handles hardware back button and gesture navigation
 */
@Composable
actual fun PlatformBackHandler(
    enabled: Boolean,
    onBackPressed: () -> Unit,
) {
    BackHandler(
        enabled = enabled,
        onBack = onBackPressed
    )
}