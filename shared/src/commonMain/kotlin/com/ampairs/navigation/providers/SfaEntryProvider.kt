package com.ampairs.navigation.providers

import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.ampairs.sfa.ui.SfaBeatListRoute
import com.ampairs.sfa.ui.beat.BeatListScreen

/**
 * Entry provider for SFA (field-sales) module routes in Navigation 3.
 * Returns a NavEntry for SFA routes, or null if the key doesn't match.
 */
fun sfaEntryProvider(
    key: NavKey,
    backStack: MutableList<NavKey>,
): NavEntry<NavKey>? = when (key) {
    is SfaBeatListRoute -> NavEntry(key) {
        BeatListScreen(
            onBeatClick = { /* beat detail — added in a later increment */ },
            onAddBeat = { /* beat form — added in a later increment */ },
            modifier = Modifier,
        )
    }

    else -> null
}
