package com.ampairs.pricing.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ampairs.common.locale.LocalAppLocale
import com.ampairs.common.locale.formatMoney
import com.ampairs.pricing.domain.model.PriceList
import com.ampairs.pricing.domain.model.PriceListItem
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import org.jetbrains.compose.resources.stringResource
import ampairsapp.feature.pricing.generated.resources.Res
import ampairsapp.feature.pricing.generated.resources.pricing_detail_add_item
import ampairsapp.feature.pricing.generated.resources.pricing_detail_applies_everyone
import ampairsapp.feature.pricing.generated.resources.pricing_detail_base_price
import ampairsapp.feature.pricing.generated.resources.pricing_detail_edit
import ampairsapp.feature.pricing.generated.resources.pricing_detail_items
import ampairsapp.feature.pricing.generated.resources.pricing_detail_moq
import ampairsapp.feature.pricing.generated.resources.pricing_detail_no_items
import ampairsapp.feature.pricing.generated.resources.pricing_detail_not_found
import ampairsapp.feature.pricing.generated.resources.pricing_detail_priority
import ampairsapp.feature.pricing.generated.resources.pricing_detail_targets_title
import ampairsapp.feature.pricing.generated.resources.pricing_detail_test_list
import ampairsapp.feature.pricing.generated.resources.pricing_detail_test_list_sub
import ampairsapp.feature.pricing.generated.resources.pricing_detail_tier_count
import ampairsapp.feature.pricing.generated.resources.pricing_target_brand
import ampairsapp.feature.pricing.generated.resources.pricing_target_category
import ampairsapp.feature.pricing.generated.resources.pricing_target_customer
import ampairsapp.feature.pricing.generated.resources.pricing_target_customer_group
import ampairsapp.feature.pricing.generated.resources.pricing_target_customer_type
import ampairsapp.feature.pricing.generated.resources.pricing_target_geo_zone
import ampairsapp.feature.pricing.generated.resources.pricing_target_product_group

private fun formatQty(q: Double): String = if (q % 1.0 == 0.0) q.toLong().toString() else q.toString()

@Composable
fun PriceListDetailScreen(
    priceListId: String,
    onEdit: () -> Unit,
    onAddItem: () -> Unit,
    onItemClick: (String) -> Unit,
    onTestList: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PriceListDetailViewModel = assistedMetroViewModel<PriceListDetailViewModel, PriceListDetailViewModel.Factory>(
        key = priceListId,
    ) { create(priceListId) },
) {
    val state by viewModel.uiState.collectAsState()
    val locale = LocalAppLocale.current

    Column(modifier = modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = state.header?.name ?: "",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(4.dp))
                    Text(stringResource(Res.string.pricing_detail_edit))
                }
            }
        }
        HorizontalDivider()

        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            state.notFound || state.header == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(Res.string.pricing_detail_not_found), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> {
                val header = state.header!!
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Pill(header.channel.name, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                            Pill(header.status.name, MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
                            Pill(stringResource(Res.string.pricing_detail_priority, header.priority), MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    item { TargetsCard(header, state.geoZoneName) }
                    item { TestListCta(onTestList) }
                    item {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                stringResource(Res.string.pricing_detail_items) + " · ${state.items.size}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            TextButton(onClick = onAddItem) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.size(4.dp))
                                Text(stringResource(Res.string.pricing_detail_add_item))
                            }
                        }
                    }
                    if (state.items.isEmpty()) {
                        item {
                            Text(
                                stringResource(Res.string.pricing_detail_no_items),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        items(state.items, key = { it.uid }) { item ->
                            ItemRow(item, locale, onClick = { onItemClick(item.uid) })
                        }
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun TargetsCard(header: PriceList, geoZoneName: String?) {
    val lblGroup = stringResource(Res.string.pricing_target_customer_group)
    val lblType = stringResource(Res.string.pricing_target_customer_type)
    val lblCustomer = stringResource(Res.string.pricing_target_customer)
    val lblBrand = stringResource(Res.string.pricing_target_brand)
    val lblCategory = stringResource(Res.string.pricing_target_category)
    val lblProductGroup = stringResource(Res.string.pricing_target_product_group)
    val lblZone = stringResource(Res.string.pricing_target_geo_zone)
    val rows = buildList {
        header.customerGroupId?.takeIf { it.isNotBlank() }?.let { add(lblGroup to it) }
        header.customerType?.takeIf { it.isNotBlank() }?.let { add(lblType to it) }
        header.customerId?.takeIf { it.isNotBlank() }?.let { add(lblCustomer to it) }
        header.brandId?.takeIf { it.isNotBlank() }?.let { add(lblBrand to it) }
        header.categoryId?.takeIf { it.isNotBlank() }?.let { add(lblCategory to it) }
        header.productGroupId?.takeIf { it.isNotBlank() }?.let { add(lblProductGroup to it) }
        header.geoZoneId?.takeIf { it.isNotBlank() }?.let { add(lblZone to (geoZoneName ?: it)) }
    }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(Res.string.pricing_detail_targets_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            if (rows.isEmpty()) {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
                        Text(
                            stringResource(Res.string.pricing_detail_applies_everyone, header.channel.name),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            } else {
                rows.forEach { (label, value) ->
                    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TestListCta(onTestList: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onTestList),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            Icon(Icons.Filled.Science, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(stringResource(Res.string.pricing_detail_test_list), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(stringResource(Res.string.pricing_detail_test_list_sub), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ItemRow(item: PriceListItem, locale: com.ampairs.common.locale.AppLocale, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    item.variantSku?.let { "${item.productId} · $it" } ?: item.productId,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    val tierLabel = if (item.tiers.isNotEmpty())
                        stringResource(Res.string.pricing_detail_tier_count, item.tiers.size)
                    else stringResource(Res.string.pricing_detail_base_price)
                    Pill(tierLabel, MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.onSurfaceVariant)
                    item.moq?.let {
                        Text(stringResource(Res.string.pricing_detail_moq, formatQty(it)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Text(formatMoney(item.unitPriceMinor / 100.0, locale), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun Pill(label: String, bg: androidx.compose.ui.graphics.Color, fg: androidx.compose.ui.graphics.Color) {
    Surface(color = bg, shape = MaterialTheme.shapes.extraLarge) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = fg,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}
