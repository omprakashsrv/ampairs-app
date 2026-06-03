package com.ampairs.ecom.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import ampairsapp.feature.ecom.generated.resources.Res
import ampairsapp.feature.ecom.generated.resources.ecom_tab_account
import ampairsapp.feature.ecom.generated.resources.ecom_tab_orders
import ampairsapp.feature.ecom.generated.resources.ecom_tab_shop
import com.ampairs.ecom.ui.account.AccountScreen
import com.ampairs.ecom.ui.browse.BrowseScreen
import com.ampairs.ecom.ui.catalog.DrillDownArgs
import com.ampairs.ecom.ui.order.OrdersListScreen
import org.jetbrains.compose.resources.stringResource

private enum class EcomTab { Shop, Orders, Account }

/**
 * Customer-facing shop shell (Direction A): persistent bottom nav across
 * Shop / Orders / Account. Detail, cart, checkout, tracking and addresses are
 * pushed onto the outer Navigation3 back stack via the callbacks.
 */
@Composable
fun EcomShell(
    onOpenSearch: () -> Unit,
    onOpenCart: () -> Unit,
    onOpenProduct: (String) -> Unit,
    onOpenDrillDown: (DrillDownArgs) -> Unit,
    onOpenOrder: (String) -> Unit,
    onOpenAddresses: () -> Unit,
    onLogin: () -> Unit,
    onLoggedOut: () -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(EcomTab.Shop) }
    val snackbar = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == EcomTab.Shop,
                    onClick = { tab = EcomTab.Shop },
                    icon = { Icon(Icons.Filled.Storefront, contentDescription = null) },
                    label = { Text(stringResource(Res.string.ecom_tab_shop)) },
                )
                NavigationBarItem(
                    selected = tab == EcomTab.Orders,
                    onClick = { tab = EcomTab.Orders },
                    icon = { Icon(Icons.Filled.Receipt, contentDescription = null) },
                    label = { Text(stringResource(Res.string.ecom_tab_orders)) },
                )
                NavigationBarItem(
                    selected = tab == EcomTab.Account,
                    onClick = { tab = EcomTab.Account },
                    icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    label = { Text(stringResource(Res.string.ecom_tab_account)) },
                )
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                EcomTab.Shop -> BrowseScreen(
                    snackbar = snackbar,
                    onOpenSearch = onOpenSearch,
                    onOpenCart = onOpenCart,
                    onOpenCategory = { onOpenDrillDown(DrillDownArgs(category = it)) },
                    onOpenBrand = { onOpenDrillDown(DrillDownArgs(brand = it)) },
                    onOpenProduct = onOpenProduct,
                )
                EcomTab.Orders -> OrdersListScreen(onOpenOrder = onOpenOrder)
                EcomTab.Account -> AccountScreen(
                    onOpenOrders = { tab = EcomTab.Orders },
                    onOpenAddresses = onOpenAddresses,
                    onLogin = onLogin,
                    onLoggedOut = onLoggedOut,
                )
            }
        }
    }
}
