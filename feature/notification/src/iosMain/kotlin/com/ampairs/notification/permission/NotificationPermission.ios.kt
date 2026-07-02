package com.ampairs.notification.permission

import androidx.compose.runtime.Composable

/** iOS authorization is requested in `AppDelegate` at launch — no-op here. */
@Composable
actual fun RequestNotificationPermissionEffect() {
    // No-op: handled by AppDelegate.
}
