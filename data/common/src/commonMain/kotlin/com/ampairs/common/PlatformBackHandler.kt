package com.ampairs.common

import androidx.compose.runtime.Composable

/**
 * Platform-specific back handler for handling system back press
 * - Android: Uses BackHandler from androidx.activity.compose
 * - iOS: No-op (iOS uses swipe gestures for navigation)
 * - Desktop: No-op (Desktop uses window controls)
 */
@Composable
expect fun PlatformBackHandler(enabled: Boolean = true, onBack: () -> Unit)
