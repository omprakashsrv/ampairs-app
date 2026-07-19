package com.ampairs.navigation.providers

import AuthRoute
import EcomRoute
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.ampairs.ecom.ui.EcomStorefrontScreen
import com.ampairs.ecom.ui.account.AccountScreen
import com.ampairs.ecom.ui.account.AddressesScreen
import com.ampairs.ecom.ui.cart.CartScreen
import com.ampairs.ecom.ui.catalog.DrillDownArgs
import com.ampairs.ecom.ui.catalog.DrillDownScreen
import com.ampairs.ecom.ui.catalog.ProductDetailScreen
import com.ampairs.ecom.ui.checkout.CheckoutScreen
import com.ampairs.ecom.ui.checkout.OrderPlacedScreen
import com.ampairs.ecom.ui.order.OrderTrackingScreen
import com.ampairs.ecom.ui.order.OrdersListScreen

/**
 * Entry provider for the ecom storefront (Navigation 3). The Storefront entry hosts the
 * gate + bottom-nav shell; every other destination is a full-screen route pushed onto the
 * shared back stack. Standalone screens get their own Scaffold + SnackbarHost.
 */
fun ecomEntryProvider(
    key: NavKey,
    backStack: MutableList<NavKey>,
): NavEntry<NavKey>? = when (key) {
    is EcomRoute.Storefront -> NavEntry(key) {
        EcomStorefrontScreen(
            slug = key.slug,
            onRequireLogin = { backStack.add(AuthRoute.Phone) },
            onOpenSearch = { backStack.add(EcomRoute.DrillDown(query = "")) },
            onOpenCart = { backStack.add(EcomRoute.Cart) },
            onOpenProduct = { backStack.add(EcomRoute.ProductDetail(it)) },
            onOpenDrillDown = { args ->
                backStack.add(EcomRoute.DrillDown(args.category, args.brand, args.subcategory, args.query))
            },
            onOpenOrder = { backStack.add(EcomRoute.OrderTracking(it)) },
            onOpenAddresses = { backStack.add(EcomRoute.Addresses) },
        )
    }

    is EcomRoute.DrillDown -> NavEntry(key) {
        EcomRouteScaffold { snackbar ->
            DrillDownScreen(
                args = DrillDownArgs(key.category, key.brand, key.subcategory, key.query),
                snackbar = snackbar,
                onBack = { backStack.removeLastOrNull() },
                onOpenProduct = { backStack.add(EcomRoute.ProductDetail(it)) },
                onOpenCart = { backStack.add(EcomRoute.Cart) },
            )
        }
    }

    is EcomRoute.ProductDetail -> NavEntry(key) {
        EcomRouteScaffold { snackbar ->
            ProductDetailScreen(
                productId = key.productId,
                snackbar = snackbar,
                onBack = { backStack.removeLastOrNull() },
                onOpenCart = { backStack.add(EcomRoute.Cart) },
            )
        }
    }

    is EcomRoute.Cart -> NavEntry(key) {
        EcomRouteScaffold { snackbar ->
            CartScreen(
                snackbar = snackbar,
                onBack = { backStack.removeLastOrNull() },
                onCheckout = { backStack.add(EcomRoute.Checkout) },
            )
        }
    }

    is EcomRoute.Checkout -> NavEntry(key) {
        EcomRouteScaffold { snackbar ->
            CheckoutScreen(
                snackbar = snackbar,
                onBack = { backStack.removeLastOrNull() },
                onAddAddress = { backStack.add(EcomRoute.Addresses) },
                onOrderPlaced = { ref, orderNumber -> backStack.add(EcomRoute.OrderPlaced(ref, orderNumber)) },
            )
        }
    }

    is EcomRoute.OrderPlaced -> NavEntry(key) {
        OrderPlacedScreen(
            orderRef = key.orderRef,
            orderNumber = key.orderNumber,
            onTrack = { backStack.add(EcomRoute.OrderTracking(key.orderRef)) },
            onContinue = {
                val idx = backStack.indexOfFirst { it is EcomRoute.Storefront }
                if (idx >= 0) while (backStack.size > idx + 1) backStack.removeLastOrNull()
            },
        )
    }

    is EcomRoute.OrderTracking -> NavEntry(key) {
        OrderTrackingScreen(orderRef = key.orderRef, onBack = { backStack.removeLastOrNull() })
    }

    is EcomRoute.Orders -> NavEntry(key) {
        OrdersListScreen(onOpenOrder = { backStack.add(EcomRoute.OrderTracking(it)) })
    }

    is EcomRoute.Account -> NavEntry(key) {
        AccountScreen(
            onOpenOrders = { backStack.add(EcomRoute.Orders) },
            onOpenAddresses = { backStack.add(EcomRoute.Addresses) },
            onLogin = { backStack.add(AuthRoute.Phone) },
            onLoggedOut = { backStack.removeLastOrNull() },
        )
    }

    is EcomRoute.Addresses -> NavEntry(key) {
        AddressesScreen(onBack = { backStack.removeLastOrNull() })
    }

    is EcomRoute.Browse -> NavEntry(key) {
        // Browse is hosted inside the shell; if reached directly, redirect to the storefront shell.
        EcomRouteScaffold { }
    }

    else -> null
}

@Composable
private fun EcomRouteScaffold(content: @Composable (SnackbarHostState) -> Unit) {
    val snackbar = remember { SnackbarHostState() }
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) { content(snackbar) }
    }
}
