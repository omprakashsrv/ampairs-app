package com.ampairs.navigation

import AuthRoute
import BusinessRoute
import ProductRoute
import Route
import SubscriptionRoute
import WorkspaceRoute
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.ampairs.customer.ui.CustomerListRoute
import com.ampairs.form.ui.FormConfigScreen
import com.ampairs.navigation.providers.authEntryProvider
import com.ampairs.navigation.providers.businessEntryProvider
import com.ampairs.navigation.providers.customerEntryProvider
import com.ampairs.navigation.providers.productEntryProvider
import com.ampairs.navigation.providers.subscriptionEntryProvider
import com.ampairs.navigation.providers.taxEntryProvider
import com.ampairs.navigation.providers.workspaceEntryProvider
import com.ampairs.tax.ui.navigation.TaxListRoute
import com.ampairs.workspace.navigation.DynamicModuleNavigationService

/**
 * Combined entry provider that chains all module entry providers together.
 * This is the main entry point for Navigation 3's NavDisplay.
 *
 * The provider tries each module's entry provider in sequence and returns
 * the first non-null NavEntry, falling back to an unknown route handler.
 *
 * Note: Uses MutableList<NavKey> for flexibility with both NavBackStack and SnapshotStateList.
 */
fun combinedEntryProvider(
    key: NavKey,
    backStack: MutableList<NavKey>,
    onLoginSuccess: () -> Unit,
    onNavigationServiceReady: ((DynamicModuleNavigationService?) -> Unit)?
): NavEntry<NavKey> {
    // Try each module's entry provider in sequence
    return authEntryProvider(key, backStack, onLoginSuccess)
        ?: workspaceEntryProvider(key, backStack, onNavigationServiceReady)
        ?: customerEntryProvider(key, backStack)
        ?: productEntryProvider(key, backStack)
        ?: taxEntryProvider(key, backStack)
        ?: businessEntryProvider(key, backStack)
        ?: subscriptionEntryProvider(key, backStack)
        ?: mainRouteEntryProvider(key, backStack)
        ?: NavEntry(key) { Text("Unknown route: $key") }
}

/**
 * Entry provider for main Route types that serve as entry points to modules.
 * These routes redirect to the actual module start destinations.
 */
private fun mainRouteEntryProvider(
    key: NavKey,
    backStack: MutableList<NavKey>
): NavEntry<NavKey>? = when (key) {
    // Route.Login is handled by authEntryProvider (redirects to AuthRoute.UserSelection)
    is Route.Login -> NavEntry(key) {
        // This will redirect to UserSelection in authEntryProvider
        // For safety, we add the actual start destination
        LaunchedEffect(Unit) {
            backStack.clear()
            backStack.add(AuthRoute.UserSelection)
        }
    }

    // Route.Workspace is handled by workspaceEntryProvider (redirects to WorkspaceRoute.Root)
    is Route.Workspace -> NavEntry(key) {
        LaunchedEffect(Unit) {
            backStack.clear()
            backStack.add(WorkspaceRoute.Root)
        }
    }

    // Route.Customer redirects to CustomerListRoute
    is Route.Customer -> NavEntry(key) {
        LaunchedEffect(Unit) {
            backStack.clear()
            backStack.add(CustomerListRoute)
        }
    }

    // Route.Product redirects to ProductRoute.Products
    is Route.Product -> NavEntry(key) {
        LaunchedEffect(Unit) {
            backStack.clear()
            backStack.add(ProductRoute.Products)
        }
    }

    // Route.Tax redirects to TaxListRoute
    is Route.Tax -> NavEntry(key) {
        LaunchedEffect(Unit) {
            backStack.clear()
            backStack.add(TaxListRoute)
        }
    }

    // Route.Business redirects to BusinessRoute.Overview
    is Route.Business -> NavEntry(key) {
        LaunchedEffect(Unit) {
            backStack.clear()
            backStack.add(BusinessRoute.Overview)
        }
    }

    // Route.Subscription redirects to SubscriptionRoute.Root
    is Route.Subscription -> NavEntry(key) {
        LaunchedEffect(Unit) {
            backStack.clear()
            backStack.add(SubscriptionRoute.Root)
        }
    }

    // Route.FormConfig - Form configuration screen
    is Route.FormConfig -> NavEntry(key) {
        FormConfigScreen(
            entityType = key.entityType,
            onNavigateBack = { backStack.removeLastOrNull() }
        )
    }

    // Placeholder routes for modules not yet implemented
    is Route.Inventory -> NavEntry(key) {
        Text("Inventory module - Coming Soon")
    }

    is Route.Order -> NavEntry(key) {
        Text("Order module - Coming Soon")
    }

    is Route.Invoice -> NavEntry(key) {
        Text("Invoice module - Coming Soon")
    }

    else -> null
}
