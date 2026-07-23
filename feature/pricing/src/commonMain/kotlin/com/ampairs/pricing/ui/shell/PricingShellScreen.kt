package com.ampairs.pricing.ui.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.ampairs.pricing.ui.geozone.GeoZoneListScreen
import com.ampairs.pricing.ui.list.PriceListListScreen
import com.ampairs.pricing.ui.offers.OffersListScreen
import com.ampairs.pricing.ui.overview.PricingOverviewScreen
import com.ampairs.pricing.ui.tester.PriceTesterScreen
import org.jetbrains.compose.resources.stringResource
import ampairsapp.feature.pricing.generated.resources.Res
import ampairsapp.feature.pricing.generated.resources.pricing_shell_tab_lists
import ampairsapp.feature.pricing.generated.resources.pricing_shell_tab_offers
import ampairsapp.feature.pricing.generated.resources.pricing_shell_tab_overview
import ampairsapp.feature.pricing.generated.resources.pricing_shell_tab_tester
import ampairsapp.feature.pricing.generated.resources.pricing_shell_tab_zones

private const val TAB_OVERVIEW = 0
private const val TAB_LISTS = 1
private const val TAB_OFFERS = 2
private const val TAB_ZONES = 3
private const val TAB_TESTER = 4

/**
 * The Pricing & Offers home shell — a persistent 5-tab bottom navigation hosting Overview, Price
 * Lists, Offers, Geo Zones, and the Price Tester. Each section's drill-downs (detail, wizard,
 * builder, editor, preview…) are pushed onto the app back stack via the callbacks, so they open
 * full-screen above the shell.
 */
@Composable
fun PricingShellScreen(
    onPriceListClick: (String) -> Unit,
    onAddPriceList: () -> Unit,
    onZoneClick: (String) -> Unit,
    onAddZone: () -> Unit,
    onOfferClick: (String) -> Unit,
    onAddOffer: () -> Unit,
    onOfferPreview: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var tab by rememberSaveable { mutableIntStateOf(TAB_OVERVIEW) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == TAB_OVERVIEW,
                    onClick = { tab = TAB_OVERVIEW },
                    icon = { Icon(Icons.Filled.Dashboard, contentDescription = null) },
                    label = { Text(stringResource(Res.string.pricing_shell_tab_overview)) },
                )
                NavigationBarItem(
                    selected = tab == TAB_LISTS,
                    onClick = { tab = TAB_LISTS },
                    icon = { Icon(Icons.Filled.Sell, contentDescription = null) },
                    label = { Text(stringResource(Res.string.pricing_shell_tab_lists)) },
                )
                NavigationBarItem(
                    selected = tab == TAB_OFFERS,
                    onClick = { tab = TAB_OFFERS },
                    icon = { Icon(Icons.Filled.LocalOffer, contentDescription = null) },
                    label = { Text(stringResource(Res.string.pricing_shell_tab_offers)) },
                )
                NavigationBarItem(
                    selected = tab == TAB_ZONES,
                    onClick = { tab = TAB_ZONES },
                    icon = { Icon(Icons.Filled.Map, contentDescription = null) },
                    label = { Text(stringResource(Res.string.pricing_shell_tab_zones)) },
                )
                NavigationBarItem(
                    selected = tab == TAB_TESTER,
                    onClick = { tab = TAB_TESTER },
                    icon = { Icon(Icons.Filled.Science, contentDescription = null) },
                    label = { Text(stringResource(Res.string.pricing_shell_tab_tester)) },
                )
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                TAB_OVERVIEW -> PricingOverviewScreen(
                    onOpenTester = { tab = TAB_TESTER },
                    onOpenLists = { tab = TAB_LISTS },
                    onOpenZones = { tab = TAB_ZONES },
                    onOpenOffers = { tab = TAB_OFFERS },
                )
                TAB_LISTS -> PriceListListScreen(
                    onPriceListClick = onPriceListClick,
                    onAddPriceList = onAddPriceList,
                    onManageGeoZones = { tab = TAB_ZONES },
                    onOpenTester = { tab = TAB_TESTER },
                )
                TAB_OFFERS -> OffersListScreen(
                    onOfferClick = onOfferClick,
                    onAddOffer = onAddOffer,
                    onPreview = onOfferPreview,
                )
                TAB_ZONES -> GeoZoneListScreen(
                    onZoneClick = onZoneClick,
                    onAddZone = onAddZone,
                )
                TAB_TESTER -> PriceTesterScreen()
            }
        }
    }
}
