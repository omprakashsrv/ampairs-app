package com.ampairs.navigation.providers

import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.ampairs.printing.ui.PrintQueueRoute
import com.ampairs.printing.ui.PrintQueueScreen
import com.ampairs.printing.ui.PrinterListRoute
import com.ampairs.printing.ui.PrinterListScreen

/**
 * Entry provider for Printing module routes in Navigation 3.
 * Returns a NavEntry for printing routes or null if the route doesn't match.
 */
fun printingEntryProvider(
    key: NavKey,
    backStack: MutableList<NavKey>,
): NavEntry<NavKey>? = when (key) {
    is PrinterListRoute -> NavEntry(key) {
        PrinterListScreen(
            onOpenQueue = { backStack.add(PrintQueueRoute) },
            modifier = Modifier,
        )
    }

    is PrintQueueRoute -> NavEntry(key) {
        PrintQueueScreen(modifier = Modifier)
    }

    else -> null
}
