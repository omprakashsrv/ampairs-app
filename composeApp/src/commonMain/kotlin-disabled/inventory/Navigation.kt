package com.ampairs.inventory

import InventoryRoute
import Route
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.ampairs.inventory.ui.InventoryPaneScreen
import com.ampairs.product.domain.Product

fun NavGraphBuilder.inventoryNavigation(
    navigator: NavController,
    onComplete: (List<Product>) -> Unit
) {
    navigation<Route.Inventory>(startDestination = InventoryRoute.Inventory) {
        composable<InventoryRoute.Inventory> {
            InventoryPaneScreen()
        }
    }
}