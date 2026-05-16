package com.ampairs.auth

import androidx.compose.runtime.Composable

/**
 * Platform-specific implementation for browser-based authentication.
 *
 * - Desktop: Opens system browser for OAuth flow
 * - Android/iOS: Stub implementation (not used on mobile)
 */
@Composable
expect fun DesktopBrowserAuthScreen(onAuthSuccess: () -> Unit)
