package com.ampairs.common.navigation

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

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

/**
 * Android-specific implementation to exit the app
 */
actual fun ExitApp() {
    kotlin.system.exitProcess(0)
}

/**
 * Composable helper to get exit function with Activity context
 */
@Composable
fun rememberExitApp(): () -> Unit {
    val context = LocalContext.current
    return {
        (context as? Activity)?.finish() ?: kotlin.system.exitProcess(0)
    }
}