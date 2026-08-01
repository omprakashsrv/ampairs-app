package com.ampairs.notification.permission

import androidx.compose.runtime.Composable

/** Desktop has no notification permission model — no-op. */
@Composable
actual fun RequestNotificationPermissionEffect() {
    // No-op.
}
