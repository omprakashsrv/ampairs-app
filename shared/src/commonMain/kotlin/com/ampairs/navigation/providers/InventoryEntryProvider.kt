package com.ampairs.navigation.providers

import InventoryRoute
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.ampairs.di.LocalAppGraph
import com.ampairs.inventory.ui.InventoryPaneScreen

/**
 * Entry provider for Inventory module routes in Navigation 3.
 * Returns NavEntry for inventory routes or null if route doesn't match.
 */
fun inventoryEntryProvider(
    key: NavKey,
    backStack: MutableList<NavKey>
): NavEntry<NavKey>? = when (key) {
    is InventoryRoute.Inventory -> NavEntry(key) {
        val graph = LocalAppGraph.current
        val inventoryListViewModel = viewModel { graph.createInventoryListViewModel() }
        InventoryPaneScreen(
            inventoryListViewModel = inventoryListViewModel,
            inventoryRepository = graph.inventoryRepository,
        )
    }

    else -> null
}
