package com.ampairs.common.ui

import BusinessRoute
import CustomerRoute
import InventoryRoute
import InvoiceRoute
import OrderRoute
import ProductRoute
import Route
import WorkspaceRoute
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation3.runtime.NavKey
import ampairsapp.shared.generated.resources.Res
import ampairsapp.shared.generated.resources.nav_home
import ampairsapp.shared.generated.resources.nav_more
import com.ampairs.customer.ui.CustomerCreateRoute
import com.ampairs.customer.ui.CustomerDetailsRoute
import com.ampairs.customer.ui.CustomerGroupCreateRoute
import com.ampairs.customer.ui.CustomerGroupListRoute
import com.ampairs.customer.ui.CustomerListRoute
import com.ampairs.customer.ui.CustomerTypeCreateRoute
import com.ampairs.customer.ui.CustomerTypeListRoute
import com.ampairs.customer.ui.StateListRoute
import com.ampairs.tax.ui.navigation.MyTaxCodesRoute
import com.ampairs.tax.ui.navigation.TaxCalculatorRoute
import com.ampairs.tax.ui.navigation.TaxCodeDetailRoute
import com.ampairs.tax.ui.navigation.TaxCodeSearchRoute
import com.ampairs.tax.ui.navigation.TaxConfigurationRoute
import com.ampairs.tax.ui.navigation.TaxListRoute
import com.ampairs.workspace.navigation.DynamicModuleRoute
import com.ampairs.workspace.navigation.GlobalNavigationManager
import com.ampairs.workspace.navigation.ModuleCodes
import kotlinx.coroutines.flow.MutableStateFlow
import org.jetbrains.compose.resources.stringResource

fun moduleCodeToRoute(code: String): NavKey? = when (code) {
    ModuleCodes.CUSTOMER_MANAGEMENT -> Route.Customer
    ModuleCodes.PRODUCT_MANAGEMENT -> Route.Product
    ModuleCodes.ORDER_MANAGEMENT -> Route.Order
    ModuleCodes.INVOICE_BILLING -> Route.Invoice
    ModuleCodes.INVENTORY_MANAGEMENT -> Route.Inventory
    ModuleCodes.TAX_CODE_MANAGEMENT -> Route.Tax
    ModuleCodes.BUSINESS_PROFILE -> Route.Business
    else -> null
}

fun resolveActiveModuleCode(currentRoute: NavKey?): String? = when {
    currentRoute == null || currentRoute is WorkspaceRoute.Modules -> null
    currentRoute is Route.More -> "more"
    currentRoute is CustomerRoute
        || currentRoute is CustomerListRoute
        || currentRoute is CustomerDetailsRoute
        || currentRoute is CustomerCreateRoute
        || currentRoute is StateListRoute
        || currentRoute is CustomerTypeListRoute
        || currentRoute is CustomerTypeCreateRoute
        || currentRoute is CustomerGroupListRoute
        || currentRoute is CustomerGroupCreateRoute -> ModuleCodes.CUSTOMER_MANAGEMENT
    currentRoute is ProductRoute -> ModuleCodes.PRODUCT_MANAGEMENT
    currentRoute is InvoiceRoute -> ModuleCodes.INVOICE_BILLING
    currentRoute is InventoryRoute -> ModuleCodes.INVENTORY_MANAGEMENT
    currentRoute is OrderRoute -> ModuleCodes.ORDER_MANAGEMENT
    currentRoute is Route.Tax
        || currentRoute is TaxListRoute
        || currentRoute is TaxConfigurationRoute
        || currentRoute is TaxCalculatorRoute
        || currentRoute is MyTaxCodesRoute
        || currentRoute is TaxCodeSearchRoute
        || currentRoute is TaxCodeDetailRoute -> ModuleCodes.TAX_CODE_MANAGEMENT
    currentRoute is BusinessRoute -> ModuleCodes.BUSINESS_PROFILE
    else -> null
}

fun navigateToModule(backStack: MutableList<NavKey>, moduleCode: String) {
    val modulesIdx = backStack.indexOfLast { it is WorkspaceRoute.Modules }
    if (modulesIdx < 0) return
    while (backStack.size > modulesIdx + 1) backStack.removeLastOrNull()
    moduleCodeToRoute(moduleCode)?.let { backStack.add(it) }
}

fun moduleCodeToIcon(code: String): ImageVector = when (code) {
    ModuleCodes.CUSTOMER_MANAGEMENT -> Icons.Default.Group
    ModuleCodes.PRODUCT_MANAGEMENT -> Icons.Default.Inventory
    ModuleCodes.ORDER_MANAGEMENT -> Icons.Default.ShoppingCart
    ModuleCodes.INVOICE_BILLING -> Icons.Default.Receipt
    ModuleCodes.INVENTORY_MANAGEMENT -> Icons.Default.Warehouse
    ModuleCodes.TAX_CODE_MANAGEMENT -> Icons.Default.Calculate
    ModuleCodes.BUSINESS_PROFILE -> Icons.Default.Business
    else -> Icons.Default.Extension
}

@Composable
fun AppBottomNavigation(
    backStack: MutableList<NavKey>,
    currentRoute: NavKey?
) {
    val globalNavManager = remember { GlobalNavigationManager.getInstance() }
    val navigationService by globalNavManager.navigationService.collectAsState()
    val navigationRoutes by remember(navigationService) {
        navigationService?.navigationRoutes ?: MutableStateFlow(emptyList<DynamicModuleRoute>())
    }.collectAsState()

    val activeModuleCode = resolveActiveModuleCode(currentRoute)
    // Bottom nav shows Home + up to 3 modules (by navigationIndex) + More
    val visibleModules = navigationRoutes.take(3)

    NavigationBar {
        NavigationBarItem(
            selected = activeModuleCode == null,
            onClick = {
                val idx = backStack.indexOfLast { it is WorkspaceRoute.Modules }
                if (idx >= 0) while (backStack.size > idx + 1) backStack.removeLastOrNull()
            },
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text(stringResource(Res.string.nav_home)) }
        )
        visibleModules.forEach { module ->
            NavigationBarItem(
                selected = activeModuleCode == module.moduleCode,
                onClick = { navigateToModule(backStack, module.moduleCode) },
                icon = { Icon(moduleCodeToIcon(module.moduleCode), contentDescription = null) },
                label = { Text(module.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            )
        }
        NavigationBarItem(
            selected = activeModuleCode == "more",
            onClick = {
                val idx = backStack.indexOfLast { it is WorkspaceRoute.Modules }
                if (idx >= 0) {
                    while (backStack.size > idx + 1) backStack.removeLastOrNull()
                    backStack.add(Route.More)
                }
            },
            icon = { Icon(Icons.Default.MoreHoriz, contentDescription = null) },
            label = { Text(stringResource(Res.string.nav_more)) }
        )
    }
}
