package com.ampairs.common.ui

import BusinessRoute
import CustomerRoute
import InventoryRoute
import InvoiceRoute
import OrderRoute
import PaymentRoute
import ProductRoute
import Route
import WorkspaceRoute
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Straighten
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
import ampairsapp.shared.generated.resources.nav_alerts
import ampairsapp.shared.generated.resources.nav_assistant
import ampairsapp.shared.generated.resources.nav_dashboard
import ampairsapp.shared.generated.resources.nav_home
import ampairsapp.shared.generated.resources.nav_inventory
import ampairsapp.shared.generated.resources.nav_more
import ampairsapp.shared.generated.resources.nav_more_business
import ampairsapp.shared.generated.resources.nav_more_tax_gst
import ampairsapp.shared.generated.resources.nav_orders
import ampairsapp.shared.generated.resources.nav_parties
import ampairsapp.shared.generated.resources.nav_payments
import ampairsapp.shared.generated.resources.nav_reports
import ampairsapp.shared.generated.resources.nav_sales
import ampairsapp.shared.generated.resources.nav_stock
import ampairsapp.shared.generated.resources.nav_storefront
import ampairsapp.shared.generated.resources.nav_tax
import ampairsapp.shared.generated.resources.nav_printing
import ampairsapp.shared.generated.resources.nav_units
import ampairsapp.shared.generated.resources.nav_users
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
import com.ampairs.unit.ui.UnitFormRoute
import com.ampairs.printing.ui.PrintQueueRoute
import com.ampairs.printing.ui.PrinterListRoute
import com.ampairs.printing.ui.TemplateEditRoute
import com.ampairs.printing.ui.TemplateListRoute
import com.ampairs.unit.ui.UnitListRoute
import com.ampairs.workspace.navigation.GlobalNavigationManager
import com.ampairs.workspace.navigation.ModuleCodes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import org.jetbrains.compose.resources.stringResource

fun moduleCodeToRoute(code: String): NavKey? = when (code) {
    ModuleCodes.CUSTOMER_MANAGEMENT -> Route.Customer
    ModuleCodes.PRODUCT_MANAGEMENT -> Route.Product
    ModuleCodes.ORDER_MANAGEMENT -> Route.Order
    ModuleCodes.INVOICE_BILLING -> Route.Invoice
    ModuleCodes.INVENTORY_MANAGEMENT -> Route.Inventory
    ModuleCodes.TAX_CODE_MANAGEMENT -> Route.Tax
    ModuleCodes.BUSINESS_PROFILE -> Route.Business
    ModuleCodes.UNIT_MANAGEMENT -> Route.Unit
    ModuleCodes.PRINTING -> Route.Printing
    ModuleCodes.PAYMENT_COLLECTION -> Route.Payment
    ModuleCodes.STOREFRONT_MANAGEMENT -> Route.Storefront
    ModuleCodes.AI_ASSISTANT -> Route.Agent
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
    currentRoute is Route.Unit
        || currentRoute is UnitListRoute
        || currentRoute is UnitFormRoute -> ModuleCodes.UNIT_MANAGEMENT
    currentRoute is Route.Printing
        || currentRoute is PrinterListRoute
        || currentRoute is TemplateListRoute
        || currentRoute is TemplateEditRoute
        || currentRoute is PrintQueueRoute -> ModuleCodes.PRINTING
    currentRoute is PaymentRoute || currentRoute is Route.Payment -> ModuleCodes.PAYMENT_COLLECTION
    currentRoute is Route.Storefront -> ModuleCodes.STOREFRONT_MANAGEMENT
    currentRoute is Route.Agent -> ModuleCodes.AI_ASSISTANT
    else -> null
}

fun navigateToModule(backStack: MutableList<NavKey>, moduleCode: String) {
    val modulesIdx = backStack.indexOfLast { it is WorkspaceRoute.Modules }
    if (modulesIdx < 0) return
    while (backStack.size > modulesIdx + 1) backStack.removeLastOrNull()
    moduleCodeToRoute(moduleCode)?.let { backStack.add(it) }
}

@Composable
fun moduleCodeToDisplayName(code: String): String = when (code) {
    ModuleCodes.CUSTOMER_MANAGEMENT -> stringResource(Res.string.nav_parties)
    ModuleCodes.PRODUCT_MANAGEMENT -> stringResource(Res.string.nav_stock)
    ModuleCodes.ORDER_MANAGEMENT -> stringResource(Res.string.nav_orders)
    ModuleCodes.INVOICE_BILLING -> stringResource(Res.string.nav_sales)
    ModuleCodes.INVENTORY_MANAGEMENT -> stringResource(Res.string.nav_inventory)
    ModuleCodes.TAX_CODE_MANAGEMENT -> stringResource(Res.string.nav_tax)
    ModuleCodes.BUSINESS_PROFILE -> stringResource(Res.string.nav_more_business)
    ModuleCodes.UNIT_MANAGEMENT -> stringResource(Res.string.nav_units)
    ModuleCodes.PRINTING -> stringResource(Res.string.nav_printing)
    ModuleCodes.PAYMENT_COLLECTION -> stringResource(Res.string.nav_payments)
    ModuleCodes.STOREFRONT_MANAGEMENT -> stringResource(Res.string.nav_storefront)
    ModuleCodes.AI_ASSISTANT -> stringResource(Res.string.nav_assistant)
    "business-reporting" -> stringResource(Res.string.nav_reports)
    "business-dashboard" -> stringResource(Res.string.nav_dashboard)
    "notification-system" -> stringResource(Res.string.nav_alerts)
    "user-management" -> stringResource(Res.string.nav_users)
    else -> code
}

fun moduleCodeToIcon(code: String): ImageVector = when (code) {
    ModuleCodes.CUSTOMER_MANAGEMENT -> Icons.Default.Group
    ModuleCodes.PRODUCT_MANAGEMENT -> Icons.Default.Inventory
    ModuleCodes.ORDER_MANAGEMENT -> Icons.Default.ShoppingCart
    ModuleCodes.INVOICE_BILLING -> Icons.Default.Receipt
    ModuleCodes.INVENTORY_MANAGEMENT -> Icons.Default.Warehouse
    ModuleCodes.TAX_CODE_MANAGEMENT -> Icons.Default.Calculate
    ModuleCodes.BUSINESS_PROFILE -> Icons.Default.Business
    ModuleCodes.UNIT_MANAGEMENT -> Icons.Default.Straighten
    ModuleCodes.PRINTING -> Icons.Default.Print
    ModuleCodes.PAYMENT_COLLECTION -> Icons.Default.Payments
    ModuleCodes.STOREFRONT_MANAGEMENT -> Icons.Default.Storefront
    ModuleCodes.AI_ASSISTANT -> Icons.Default.AutoAwesome
    "business-reporting" -> Icons.Default.Analytics
    "business-dashboard" -> Icons.Default.Dashboard
    "notification-system" -> Icons.Default.Notifications
    "user-management" -> Icons.Default.ManageAccounts
    else -> Icons.Default.Extension
}

@Composable
fun AppBottomNavigation(
    backStack: MutableList<NavKey>,
    currentRoute: NavKey?
) {
    val globalNavManager = remember { GlobalNavigationManager.getInstance() }
    @OptIn(ExperimentalCoroutinesApi::class)
    val navigationRoutes by remember {
        globalNavManager.navigationService.flatMapLatest { service ->
            service?.navigationRoutes ?: flowOf(emptyList())
        }
    }.collectAsState(initial = emptyList())

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
                label = { Text(moduleCodeToDisplayName(module.moduleCode), maxLines = 1, overflow = TextOverflow.Ellipsis) }
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
