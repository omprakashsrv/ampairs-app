package com.ampairs.navigation.providers

import InventoryRoute
import Route
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.ampairs.inventory.ui.InventoryPaneScreen
import com.ampairs.inventory.ui.dashboard.InventoryDashboardScreen
import com.ampairs.inventory.ui.form.InventoryFormScreen
import com.ampairs.inventory.ui.count.PhysicalCountScreen
import com.ampairs.inventory.ui.ledger.MovementLedgerScreen
import com.ampairs.inventory.ui.lowstock.LowStockScreen
import com.ampairs.inventory.ui.settings.InventorySettingsScreen

/**
 * Entry provider for the Inventory module (spec — full 9-screen UX). The list+detail two-pane lives
 * inside [InventoryPaneScreen] (an adaptive `ListDetailPaneScaffold`); every other screen is a
 * full-screen destination pushed onto the back stack, mirroring the Customer/Order modules.
 */
fun inventoryEntryProvider(
    key: NavKey,
    backStack: MutableList<NavKey>,
): NavEntry<NavKey>? = when (key) {
    is InventoryRoute.Dashboard -> NavEntry(key) {
        InventoryDashboardScreen(
            onOpenItems = { backStack.add(InventoryRoute.Items) },
            onAddItem = { backStack.add(InventoryRoute.ItemForm()) },
            onOpenCount = { backStack.add(InventoryRoute.PhysicalCount) },
            onOpenLedger = { backStack.add(InventoryRoute.Ledger) },
            onOpenLowStock = { backStack.add(InventoryRoute.LowStock) },
            onOpenSettings = { backStack.add(InventoryRoute.Settings) },
        )
    }

    is InventoryRoute.Items -> NavEntry(key) {
        InventoryPaneScreen(
            onAddItem = { backStack.add(InventoryRoute.ItemForm()) },
            onEditItem = { itemId -> backStack.add(InventoryRoute.ItemForm(itemId)) },
            onNavigateBack = { backStack.removeLastOrNull() },
        )
    }

    is InventoryRoute.ItemForm -> NavEntry(key) {
        InventoryFormScreen(
            itemId = key.itemId,
            onSaveSuccess = { backStack.removeLastOrNull() },
            onNavigateBack = { backStack.removeLastOrNull() },
        )
    }

    is InventoryRoute.PhysicalCount -> NavEntry(key) {
        PhysicalCountScreen(
            onNavigateBack = { backStack.removeLastOrNull() },
        )
    }

    is InventoryRoute.Ledger -> NavEntry(key) {
        MovementLedgerScreen(
            onNavigateBack = { backStack.removeLastOrNull() },
        )
    }

    is InventoryRoute.LowStock -> NavEntry(key) {
        LowStockScreen(
            onNavigateBack = { backStack.removeLastOrNull() },
            onCreateOrder = { backStack.add(Route.Order) },
        )
    }

    is InventoryRoute.Settings -> NavEntry(key) {
        InventorySettingsScreen(
            onNavigateBack = { backStack.removeLastOrNull() },
        )
    }

    else -> null
}
