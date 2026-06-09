package com.ampairs.navigation.providers

import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.ampairs.store.ui.StoreSettingsRoute
import com.ampairs.store.ui.StoreSettingsScreen

/**
 * Entry provider for the Store (settings) module. Returns a NavEntry for store routes or null.
 */
fun storeEntryProvider(
    key: NavKey,
    backStack: MutableList<NavKey>,
): NavEntry<NavKey>? = when (key) {
    is StoreSettingsRoute -> NavEntry(key) {
        StoreSettingsScreen(modifier = Modifier)
    }

    else -> null
}
