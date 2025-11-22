package com.ampairs.common

import androidx.compose.runtime.Composable

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS uses swipe gestures for navigation, no system back button
    // No-op implementation
}
