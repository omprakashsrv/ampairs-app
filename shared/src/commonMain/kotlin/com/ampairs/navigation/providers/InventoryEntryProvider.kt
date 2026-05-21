package com.ampairs.navigation.providers

import InventoryRoute
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.ampairs.di.LocalAppGraph
import com.ampairs.inventory.ui.InventoryPaneScreen

fun inventoryEntryProvider(
    key: NavKey,
    backStack: MutableList<NavKey>
): NavEntry<NavKey>? = when (key) {
    is InventoryRoute.Inventory -> NavEntry(key) {
        val graph = LocalAppGraph.current
        InventoryPaneScreen(
            inventoryRepository = graph.inventoryRepository,
        )
    }

    else -> null
}
