package com.ampairs.workspace.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.MenuBarScope

/**
 * Just the dynamic modules menu - can be integrated into existing MenuBars
 */
@Composable
fun MenuBarScope.DynamicModulesMenu(
    navigationService: DynamicModuleNavigationService,
    onNavigate: (String) -> Unit
) {
    val navigationRoutes by navigationService.navigationRoutes.collectAsState()
    val isLoading by navigationService.isLoading.collectAsState()

    // Debug logging
    LaunchedEffect(navigationRoutes) {
        println("DesktopMenuBar: navigationRoutes updated with ${navigationRoutes.size} routes")
        navigationRoutes.forEach { route ->
            println("  Route: ${route.moduleCode} displayName='${route.displayName}' with ${route.menuItems.size} menu items")
        }
    }

    // Dynamic Module Menus - each module becomes its own top-level menu
    if (!isLoading && navigationRoutes.isNotEmpty()) {
        navigationRoutes.forEach { moduleRoute ->
            val mnemonic =
                if (moduleRoute.displayName.isNotEmpty()) moduleRoute.displayName.first() else 'M'
            Menu(moduleRoute.displayName, mnemonic = mnemonic) {
                if (moduleRoute.menuItems.isNotEmpty()) {
                    // Add menu items for this module
                    moduleRoute.menuItems.forEach { menuItem ->
                        Item(
                            text = if (menuItem.isDefault) "${menuItem.label} (Default)" else menuItem.label,
                            onClick = {
                                // Navigate to specific menu item using its routePath
                                onNavigate(menuItem.routePath)
                                println("DesktopMenuBar: Navigating to ${menuItem.routePath} for menu item ${menuItem.label}")
                            }
                        )
                    }
                } else {
                    // If no menu items, add a default item to navigate to the module's base path
                    Item(
                        "Open ${moduleRoute.displayName}",
                        onClick = {
                            // Use the module's basePath or fallback to legacy navigation
                            val routePath = moduleRoute.basePath.ifEmpty {
                                getModuleNavigationPath(moduleRoute.moduleCode)
                            }
                            onNavigate(routePath)
                            println("DesktopMenuBar: Navigating to $routePath for module ${moduleRoute.moduleCode}")
                        }
                    )
                }
            }
        }
    }
}

/**
 * Get navigation path for a module code
 * Maps module codes to simple navigation paths that the main app can handle
 * @param moduleCode The module code (e.g., "customer-management")
 * @return Navigation path string
 */
private fun getModuleNavigationPath(moduleCode: String): String {
    return when (moduleCode) {
        "customer-management" -> "customer"
        "product-management" -> "product"
        "order-management" -> "order"
        "invoice-management" -> "invoice"
        "inventory-management" -> "inventory"
        "tax-code-management" -> "tax"
        "tax-management" -> "tax"
        else -> "unknown"
    }
}