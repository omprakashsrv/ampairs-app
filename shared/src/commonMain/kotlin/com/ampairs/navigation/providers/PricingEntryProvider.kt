package com.ampairs.navigation.providers

import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.ampairs.pricing.ui.GeoZoneFormRoute
import com.ampairs.pricing.ui.GeoZoneListRoute
import com.ampairs.pricing.ui.PriceListDetailRoute
import com.ampairs.pricing.ui.PriceListFormRoute
import com.ampairs.pricing.ui.PriceListListRoute
import com.ampairs.pricing.ui.PriceTesterRoute
import com.ampairs.pricing.ui.detail.PriceListDetailScreen
import com.ampairs.pricing.ui.form.PriceListFormScreen
import com.ampairs.pricing.ui.geozone.GeoZoneFormScreen
import com.ampairs.pricing.ui.geozone.GeoZoneListScreen
import com.ampairs.pricing.ui.list.PriceListListScreen
import com.ampairs.pricing.ui.tester.PriceTesterScreen

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
            onPriceListClick = { id -> backStack.add(PriceListDetailRoute(id)) },
            onAddPriceList = { backStack.add(PriceListFormRoute()) },
            onManageGeoZones = { backStack.add(GeoZoneListRoute) },
            onOpenTester = { backStack.add(PriceTesterRoute) },
            modifier = Modifier,
        )
    }

    is PriceListDetailRoute -> NavEntry(key) {
        PriceListDetailScreen(
            priceListId = key.priceListId,
            onEdit = { backStack.add(PriceListFormRoute(key.priceListId)) },
            onAddItem = { backStack.add(PriceListFormRoute(key.priceListId)) },
            onItemClick = { _ -> backStack.add(PriceListFormRoute(key.priceListId)) },
            onTestList = { backStack.add(PriceTesterRoute) },
            modifier = Modifier,
        )
    }

    is PriceTesterRoute -> NavEntry(key) {
        PriceTesterScreen(modifier = Modifier)
    }

    is PriceListFormRoute -> NavEntry(key) {
        PriceListFormScreen(
            priceListId = key.priceListId,
            onSaveSuccess = { backStack.removeLastOrNull() },
            modifier = Modifier,
        )
    }

    is GeoZoneListRoute -> NavEntry(key) {
        GeoZoneListScreen(
            onZoneClick = { id -> backStack.add(GeoZoneFormRoute(id)) },
            onAddZone = { backStack.add(GeoZoneFormRoute()) },
            modifier = Modifier,
        )
    }

    is GeoZoneFormRoute -> NavEntry(key) {
        GeoZoneFormScreen(
            geoZoneId = key.geoZoneId,
            onSaveSuccess = { backStack.removeLastOrNull() },
            modifier = Modifier,
        )
    }

    else -> null
}
