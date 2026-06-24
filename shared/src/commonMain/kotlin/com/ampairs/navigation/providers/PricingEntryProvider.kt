package com.ampairs.navigation.providers

import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.ampairs.pricing.ui.PriceListFormRoute
import com.ampairs.pricing.ui.PriceListListRoute
import com.ampairs.pricing.ui.form.PriceListFormScreen
import com.ampairs.pricing.ui.list.PriceListListScreen

/**
 * Entry provider for Pricing module routes in Navigation 3.
 * Returns a NavEntry for pricing routes or null if the route doesn't match.
 */
fun pricingEntryProvider(
    key: NavKey,
    backStack: MutableList<NavKey>,
): NavEntry<NavKey>? = when (key) {
    is PriceListListRoute -> NavEntry(key) {
        PriceListListScreen(
            onPriceListClick = { id -> backStack.add(PriceListFormRoute(id)) },
            onAddPriceList = { backStack.add(PriceListFormRoute()) },
            modifier = Modifier,
        )
    }

    is PriceListFormRoute -> NavEntry(key) {
        PriceListFormScreen(
            priceListId = key.priceListId,
            onSaveSuccess = { backStack.removeLastOrNull() },
            modifier = Modifier,
        )
    }

    else -> null
}
