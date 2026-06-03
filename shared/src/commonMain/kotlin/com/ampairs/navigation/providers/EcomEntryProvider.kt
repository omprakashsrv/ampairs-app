package com.ampairs.navigation.providers

import AuthRoute
import EcomRoute
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.ampairs.ecom.ui.EcomStorefrontScreen

/**
 * Entry provider for the ecom storefront (Navigation 3).
 *
 * Phase 1: the storefront entry resolves the access gate and renders a placeholder shop.
 * The remaining EcomRoute destinations (Browse/Cart/Checkout/Orders/Account…) are wired in
 * later phases; until then they render a placeholder so navigation never dead-ends.
 */
fun ecomEntryProvider(
    key: NavKey,
    backStack: MutableList<NavKey>,
): NavEntry<NavKey>? = when (key) {
    is EcomRoute.Storefront -> NavEntry(key) {
        EcomStorefrontScreen(
            slug = key.slug,
            onRequireLogin = { backStack.add(AuthRoute.Phone) },
            onExitStore = { backStack.removeLastOrNull() },
        )
    }

    is EcomRoute.Browse,
    is EcomRoute.DrillDown,
    is EcomRoute.ProductDetail,
    is EcomRoute.Cart,
    is EcomRoute.Checkout,
    is EcomRoute.OrderPlaced,
    is EcomRoute.Orders,
    is EcomRoute.OrderTracking,
    is EcomRoute.Account,
    is EcomRoute.Addresses -> NavEntry(key) { EcomPlaceholderScreen() }

    else -> null
}

@Composable
private fun EcomPlaceholderScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "Coming soon",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
