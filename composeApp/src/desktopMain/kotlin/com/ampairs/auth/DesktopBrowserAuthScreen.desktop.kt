package com.ampairs.auth

import androidx.compose.runtime.Composable
import com.ampairs.auth.ui.DesktopBrowserAuthScreen as DesktopBrowserAuthScreenImpl

/**
 * Actual implementation of DesktopBrowserAuthScreen for desktop platform
 */
@Composable
actual fun DesktopBrowserAuthScreen(onAuthSuccess: () -> Unit) {
    DesktopBrowserAuthScreenImpl(onAuthSuccess = onAuthSuccess)
}
