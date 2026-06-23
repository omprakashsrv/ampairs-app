package com.ampairs.navigation.providers

import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.ampairs.notification.ui.NotificationListRoute
import com.ampairs.notification.ui.list.NotificationListScreen

/**
 * Entry provider for Notification Center routes in Navigation 3.
 * Returns a NavEntry for notification routes, or null if the key doesn't match.
 */
fun notificationEntryProvider(
    key: NavKey,
    backStack: MutableList<NavKey>,
): NavEntry<NavKey>? = when (key) {
    is NotificationListRoute -> NavEntry(key) {
        NotificationListScreen(
            onOpenDeepLink = { /* deep-link routing handled by the global deep-link strategy */ },
            modifier = Modifier,
        )
    }

    else -> null
}
