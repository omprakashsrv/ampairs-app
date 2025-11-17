package com.ampairs.common

import androidx.compose.runtime.Composable

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // Desktop uses window controls, no system back button
    // No-op implementation
}
