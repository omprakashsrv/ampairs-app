package com.ampairs.navigation.providers

import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.ampairs.pricing.ui.GeoZoneFormRoute
import com.ampairs.pricing.ui.GeoZoneListRoute
import com.ampairs.pricing.ui.ItemEditorRoute
import com.ampairs.pricing.ui.PriceListDetailRoute
import com.ampairs.pricing.ui.PriceListFormRoute
import com.ampairs.pricing.ui.PriceListListRoute
import com.ampairs.pricing.ui.PriceListWizardRoute
import com.ampairs.pricing.ui.PriceTesterRoute
import com.ampairs.pricing.ui.PricingHomeRoute
import com.ampairs.pricing.ui.detail.PriceListDetailScreen
import com.ampairs.pricing.ui.overview.PricingOverviewScreen
import com.ampairs.pricing.ui.form.PriceListFormScreen
import com.ampairs.pricing.ui.geozone.GeoZoneFormScreen
import com.ampairs.pricing.ui.geozone.GeoZoneListScreen
import com.ampairs.pricing.ui.itemeditor.ItemEditorScreen
import com.ampairs.pricing.ui.list.PriceListListScreen
import com.ampairs.pricing.ui.tester.PriceTesterScreen
import com.ampairs.pricing.ui.wizard.PriceListWizardScreen

/**
 * Entry provider for Pricing module routes in Navigation 3.
 * Returns a NavEntry for pricing routes or null if the route doesn't match.
 */
fun pricingEntryProvider(
    key: NavKey,
    backStack: MutableList<NavKey>,
): NavEntry<NavKey>? = when (key) {
    is PricingHomeRoute -> NavEntry(key) {
        PricingOverviewScreen(
            onOpenTester = { backStack.add(PriceTesterRoute) },
            onOpenLists = { backStack.add(PriceListListRoute) },
            onOpenZones = { backStack.add(GeoZoneListRoute) },
            modifier = Modifier,
        )
    }

    is PriceListListRoute -> NavEntry(key) {
        PriceListListScreen(
            onPriceListClick = { id -> backStack.add(PriceListDetailRoute(id)) },
            onAddPriceList = { backStack.add(PriceListWizardRoute) },
            onManageGeoZones = { backStack.add(GeoZoneListRoute) },
            onOpenTester = { backStack.add(PriceTesterRoute) },
            modifier = Modifier,
        )
    }

    is PriceListDetailRoute -> NavEntry(key) {
        PriceListDetailScreen(
            priceListId = key.priceListId,
            onEdit = { backStack.add(PriceListFormRoute(key.priceListId)) },
            onAddItem = { backStack.add(ItemEditorRoute(key.priceListId, null)) },
            onItemClick = { itemId -> backStack.add(ItemEditorRoute(key.priceListId, itemId)) },
            onTestList = { backStack.add(PriceTesterRoute) },
            modifier = Modifier,
        )
    }

    is PriceListWizardRoute -> NavEntry(key) {
        PriceListWizardScreen(
            onSaved = { backStack.removeLastOrNull() },
            onCreateZone = { backStack.add(GeoZoneFormRoute()) },
            modifier = Modifier,
        )
    }

    is ItemEditorRoute -> NavEntry(key) {
        ItemEditorScreen(
            priceListId = key.priceListId,
            itemId = key.itemId,
            onSaved = { backStack.removeLastOrNull() },
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
