package com.ampairs.notification.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Notification center list route. */
@Serializable
data object NotificationListRoute : NavKey

/** Device-local notification preferences (master + per-type toggles). */
@Serializable
data object NotificationSettingsRoute : NavKey
