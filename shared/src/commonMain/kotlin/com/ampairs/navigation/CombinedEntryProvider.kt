package com.ampairs.navigation

import AuthRoute
import BusinessRoute
import InventoryRoute
import InvoiceRoute
import MoreScreen
import OrderRoute
import PaymentRoute
import ProductRoute
import Route
import SubscriptionRoute
import WorkspaceRoute
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.ampairs.customer.ui.CustomerListRoute
import com.ampairs.form.ui.FormConfigScreen
import com.ampairs.form.ui.FormConfigHubScreen
import com.ampairs.navigation.providers.agentEntryProvider
import com.ampairs.navigation.providers.authEntryProvider
import com.ampairs.navigation.providers.businessEntryProvider
import com.ampairs.navigation.providers.customerEntryProvider
import com.ampairs.navigation.providers.ecomEntryProvider
import com.ampairs.navigation.providers.inventoryEntryProvider
import com.ampairs.navigation.providers.invoiceEntryProvider
import com.ampairs.navigation.providers.notificationEntryProvider
import com.ampairs.navigation.providers.orderEntryProvider
import com.ampairs.navigation.providers.paymentEntryProvider
import com.ampairs.navigation.providers.productEntryProvider
import com.ampairs.navigation.providers.sequenceEntryProvider
import com.ampairs.navigation.providers.storeEntryProvider
import com.ampairs.navigation.providers.storefrontEntryProvider
import com.ampairs.navigation.providers.subscriptionEntryProvider
import com.ampairs.navigation.providers.taxEntryProvider
import com.ampairs.navigation.providers.unitEntryProvider
import com.ampairs.navigation.providers.printingEntryProvider
import com.ampairs.navigation.providers.workspaceEntryProvider
import com.ampairs.store.ui.StoreSettingsRoute
import com.ampairs.tax.ui.navigation.TaxListRoute
import com.ampairs.unit.ui.UnitListRoute
import com.ampairs.notification.ui.NotificationListRoute
import com.ampairs.printing.ui.PrinterListRoute
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
    onNavigationServiceReady: ((DynamicModuleNavigationService?) -> Unit)?,
    sharedViewModelStoreOwner: ViewModelStoreOwner
): NavEntry<NavKey> {
    // Try each module's entry provider in sequence
    return authEntryProvider(key, backStack, onLoginSuccess, sharedViewModelStoreOwner)
        ?: workspaceEntryProvider(key, backStack, onNavigationServiceReady)
        ?: customerEntryProvider(key, backStack)
        ?: storefrontEntryProvider(key, backStack)
        ?: ecomEntryProvider(key, backStack)
        ?: productEntryProvider(key, backStack)
        ?: taxEntryProvider(key, backStack)
        ?: businessEntryProvider(key, backStack)
        ?: subscriptionEntryProvider(key, backStack)
        ?: unitEntryProvider(key, backStack)
        ?: printingEntryProvider(key, backStack)
        ?: storeEntryProvider(key, backStack)
        ?: sequenceEntryProvider(key, backStack)
        ?: orderEntryProvider(key, backStack)
        ?: invoiceEntryProvider(key, backStack)
        ?: paymentEntryProvider(key, backStack)
        ?: notificationEntryProvider(key, backStack)
        ?: inventoryEntryProvider(key, backStack)
        ?: agentEntryProvider(key, backStack)
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
            backStack.removeLastOrNull()
            backStack.add(CustomerListRoute)
        }
    }

    // Route.Product redirects to ProductRoute.Products
    is Route.Product -> NavEntry(key) {
        LaunchedEffect(Unit) {
            backStack.removeLastOrNull()
            backStack.add(ProductRoute.Products)
        }
    }

    // Route.Tax redirects to TaxListRoute
    is Route.Tax -> NavEntry(key) {
        LaunchedEffect(Unit) {
            backStack.removeLastOrNull()
            backStack.add(TaxListRoute)
        }
    }

    // Route.Business redirects to BusinessRoute.Overview
    is Route.Business -> NavEntry(key) {
        LaunchedEffect(Unit) {
            backStack.removeLastOrNull()
            backStack.add(BusinessRoute.Overview)
        }
    }

    // Route.Subscription redirects to SubscriptionRoute.Root
    is Route.Subscription -> NavEntry(key) {
        LaunchedEffect(Unit) {
            backStack.removeLastOrNull()
            backStack.add(SubscriptionRoute.Root)
        }
    }

    // Route.FormConfig — blank entityType opens the hub (entity picker); a concrete type opens its editor.
    is Route.FormConfig -> NavEntry(key) {
        if (key.entityType.isBlank()) {
            FormConfigHubScreen(
                onSelectEntity = { entityType -> backStack.add(Route.FormConfig(entityType)) },
                onNavigateBack = { backStack.removeLastOrNull() }
            )
        } else {
            FormConfigScreen(
                entityType = key.entityType,
                onNavigateBack = { backStack.removeLastOrNull() }
            )
        }
    }

    // Route.Unit redirects to UnitListRoute
    is Route.Unit -> NavEntry(key) {
        LaunchedEffect(Unit) {
            backStack.removeLastOrNull()
            backStack.add(UnitListRoute)
        }
    }

    // Route.Settings redirects to StoreSettingsRoute
    is Route.Settings -> NavEntry(key) {
        LaunchedEffect(Unit) {
            backStack.removeLastOrNull()
            backStack.add(StoreSettingsRoute)
        }
    }

    // Route.Printing redirects to PrinterListRoute
    is Route.Printing -> NavEntry(key) {
        LaunchedEffect(Unit) {
            backStack.removeLastOrNull()
            backStack.add(PrinterListRoute)
        }
    }

    // Route.Order redirects to OrderRoute.Orders
    is Route.Order -> NavEntry(key) {
        LaunchedEffect(Unit) {
            backStack.removeLastOrNull()
            backStack.add(OrderRoute.Orders)
        }
    }

    // Route.Invoice redirects to InvoiceRoute.Invoices
    is Route.Invoice -> NavEntry(key) {
        LaunchedEffect(Unit) {
            backStack.removeLastOrNull()
            backStack.add(InvoiceRoute.Invoices)
        }
    }

    // Route.Inventory redirects to InventoryRoute.Dashboard
    is Route.Inventory -> NavEntry(key) {
        LaunchedEffect(Unit) {
            backStack.removeLastOrNull()
            backStack.add(InventoryRoute.Dashboard)
        }
    }

    // Route.Payment redirects to PaymentRoute.Dashboard
    is Route.Payment -> NavEntry(key) {
        LaunchedEffect(Unit) {
            backStack.removeLastOrNull()
            backStack.add(PaymentRoute.Dashboard)
        }
    }

    // Route.Notifications redirects to NotificationListRoute
    is Route.Notifications -> NavEntry(key) {
        LaunchedEffect(Unit) {
            backStack.removeLastOrNull()
            backStack.add(NotificationListRoute)
        }
    }

    // Route.More shows the More tab screen
    is Route.More -> NavEntry(key) {
        val globalNavManager = remember { com.ampairs.workspace.navigation.GlobalNavigationManager.getInstance() }
        val headerStateManager = remember { com.ampairs.common.state.AppHeaderStateManager.instance }
        val headerState by headerStateManager.headerState.collectAsState()
        MoreScreen(
            backStack = backStack,
            onSwitchWorkspace = {
                backStack.clear()
                backStack.add(WorkspaceRoute.Root)
            },
            onSwitchUser = { backStack.add(AuthRoute.UserSelection) },
            onEditProfile = { backStack.add(AuthRoute.UserUpdate) }
        )
    }

    else -> null
}
