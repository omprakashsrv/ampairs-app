package com.ampairs.pricing.ui.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.stringResource
import ampairsapp.feature.pricing.generated.resources.Res
import ampairsapp.feature.pricing.generated.resources.pricing_overview_active_lists_count
import ampairsapp.feature.pricing.generated.resources.pricing_overview_channel_split
import ampairsapp.feature.pricing.generated.resources.pricing_overview_hero_badge
import ampairsapp.feature.pricing.generated.resources.pricing_overview_hero_cta
import ampairsapp.feature.pricing.generated.resources.pricing_overview_hero_sub
import ampairsapp.feature.pricing.generated.resources.pricing_overview_hero_title
import ampairsapp.feature.pricing.generated.resources.pricing_overview_quick_lists
import ampairsapp.feature.pricing.generated.resources.pricing_overview_quick_lists_sub
import ampairsapp.feature.pricing.generated.resources.pricing_overview_quick_offers
import ampairsapp.feature.pricing.generated.resources.pricing_overview_quick_offers_sub
import ampairsapp.feature.pricing.generated.resources.pricing_overview_quick_zones
import ampairsapp.feature.pricing.generated.resources.pricing_overview_quick_zones_sub
import ampairsapp.feature.pricing.generated.resources.pricing_overview_retail
import ampairsapp.feature.pricing.generated.resources.pricing_overview_tile_active_lists
import ampairsapp.feature.pricing.generated.resources.pricing_overview_tile_geo_zones
import ampairsapp.feature.pricing.generated.resources.pricing_overview_tile_offers
import ampairsapp.feature.pricing.generated.resources.pricing_overview_title
import ampairsapp.feature.pricing.generated.resources.pricing_overview_wholesale

@Composable
fun PricingOverviewScreen(
    onOpenTester: () -> Unit,
    onOpenLists: () -> Unit,
    onOpenZones: () -> Unit,
    onOpenOffers: () -> Unit,
    viewModel: PricingOverviewViewModel = metroViewModel(),
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                stringResource(Res.string.pricing_overview_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        item { HeroCard(onOpenTester) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryTile(
                    icon = Icons.Filled.Sell,
                    value = state.activeListCount,
                    label = stringResource(Res.string.pricing_overview_tile_active_lists),
                    onClick = onOpenLists,
                    modifier = Modifier.weight(1f),
                )
                SummaryTile(
                    icon = Icons.Filled.LocalOffer,
                    value = state.activeOfferCount,
                    label = stringResource(Res.string.pricing_overview_tile_offers),
                    onClick = onOpenOffers,
                    modifier = Modifier.weight(1f),
                )
                SummaryTile(
                    icon = Icons.Filled.Map,
                    value = state.geoZoneCount,
                    label = stringResource(Res.string.pricing_overview_tile_geo_zones),
                    onClick = onOpenZones,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item { ChannelSplitCard(state.retailActiveCount, state.wholesaleActiveCount, state.activeListCount) }
        item {
            QuickNavRow(
                icon = Icons.Filled.Sell,
                title = stringResource(Res.string.pricing_overview_quick_lists),
                subtitle = stringResource(Res.string.pricing_overview_quick_lists_sub),
                onClick = onOpenLists,
            )
        }
        item {
            QuickNavRow(
                icon = Icons.Filled.LocalOffer,
                title = stringResource(Res.string.pricing_overview_quick_offers),
                subtitle = stringResource(Res.string.pricing_overview_quick_offers_sub),
                onClick = onOpenOffers,
            )
        }
        item {
            QuickNavRow(
                icon = Icons.Filled.Map,
                title = stringResource(Res.string.pricing_overview_quick_zones),
                subtitle = stringResource(Res.string.pricing_overview_quick_zones_sub),
                onClick = onOpenZones,
            )
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun HeroCard(onOpenTester: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenTester),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(16.dp))
                Text(stringResource(Res.string.pricing_overview_hero_badge), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
            }
            Text(stringResource(Res.string.pricing_overview_hero_title), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(stringResource(Res.string.pricing_overview_hero_sub), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Surface(color = MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.extraLarge) {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(stringResource(Res.string.pricing_overview_hero_cta), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary)
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun SummaryTile(icon: ImageVector, value: Int, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Text("$value", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ChannelSplitCard(retail: Int, wholesale: Int, activeTotal: Int) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(Res.string.pricing_overview_channel_split), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(stringResource(Res.string.pricing_overview_active_lists_count, activeTotal), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val total = retail + wholesale
            Row(Modifier.fillMaxWidth().height(14.dp).clip(MaterialTheme.shapes.small)) {
                if (total == 0) {
                    Box(Modifier.weight(1f).fillMaxHeight().background(MaterialTheme.colorScheme.surfaceContainerHighest))
                } else {
                    if (retail > 0) Box(Modifier.weight(retail.toFloat()).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
                    if (wholesale > 0) Box(Modifier.weight(wholesale.toFloat()).fillMaxHeight().background(MaterialTheme.colorScheme.secondary))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                LegendDot(MaterialTheme.colorScheme.primary, stringResource(Res.string.pricing_overview_retail), retail)
                LegendDot(MaterialTheme.colorScheme.secondary, stringResource(Res.string.pricing_overview_wholesale), wholesale)
            }
        }
    }
}

@Composable
private fun LegendDot(color: androidx.compose.ui.graphics.Color, label: String, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(Modifier.size(11.dp).clip(MaterialTheme.shapes.extraSmall).background(color))
        Text("$label · $count", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun QuickNavRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
