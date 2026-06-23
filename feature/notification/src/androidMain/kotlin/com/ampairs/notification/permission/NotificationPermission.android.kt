package com.ampairs.notification.permission

import android.Manifest
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

/**
 * Android 13+ (API 33) requires a runtime grant for `POST_NOTIFICATIONS`. Below 33 the permission is
 * granted at install time, so we skip the request entirely.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
actual fun RequestNotificationPermissionEffect() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val permissionState = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    // Request once on first entry; don't re-prompt on every recomposition or on denial.
    LaunchedEffect(Unit) {
        if (!permissionState.status.isGranted) {
            permissionState.launchPermissionRequest()
        }
    }
}
