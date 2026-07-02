package com.ampairs.notification.permission

import androidx.compose.runtime.Composable

/**
 * Requests the platform runtime notification permission (Phase 6 of FCM push notifications).
 *
 * Behaviour per platform:
 *  - **Android**: requests `POST_NOTIFICATIONS` on API 33+ (Android 13). Below 33 the permission is
 *    granted at install time, so this is a no-op. Uses Accompanist Permissions, mirroring
 *    `LocationPermissionHandler` in `feature/formwidgets`.
 *  - **iOS**: no-op — authorization is requested in `AppDelegate` at launch.
 *  - **Desktop**: no-op — no notification permission model.
 *
 * Call once from a `LaunchedEffect`-friendly composable (e.g. when the notification center first
 * opens). It is idempotent and non-blocking; if already granted/denied it does nothing visible.
 */
@Composable
expect fun RequestNotificationPermissionEffect()
