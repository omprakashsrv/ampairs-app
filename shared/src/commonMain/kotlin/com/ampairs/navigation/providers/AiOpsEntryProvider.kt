package com.ampairs.navigation.providers

import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.ampairs.aiops.ui.activity.AiOpsActivityRoute
import com.ampairs.aiops.ui.activity.AiOpsActivityScreen
import com.ampairs.aiops.ui.settings.AiOpsSettingsRoute
import com.ampairs.aiops.ui.settings.AiOpsSettingsScreen

/**
 * Entry provider for AI Ops (data-quality assistant) routes in Navigation 3.
 * Returns a NavEntry for AI Ops routes, or null if the key doesn't match.
 */
fun aiOpsEntryProvider(
    key: NavKey,
    backStack: MutableList<NavKey>,
): NavEntry<NavKey>? = when (key) {
    is AiOpsSettingsRoute -> NavEntry(key) {
        AiOpsSettingsScreen(
            onNavigateBack = { backStack.removeLastOrNull() },
            modifier = Modifier,
        )
    }

    is AiOpsActivityRoute -> NavEntry(key) {
        AiOpsActivityScreen(
            onNavigateBack = { backStack.removeLastOrNull() },
            modifier = Modifier,
        )
    }

    else -> null
}
