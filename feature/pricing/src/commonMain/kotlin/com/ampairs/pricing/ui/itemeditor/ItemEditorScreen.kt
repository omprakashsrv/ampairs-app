package com.ampairs.pricing.ui.itemeditor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Stairs
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ampairs.common.locale.AppLocale
import com.ampairs.common.locale.LocalAppLocale
import com.ampairs.common.locale.formatMoney
import com.ampairs.product.domain.ProductSummary
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import org.jetbrains.compose.resources.stringResource
import ampairsapp.feature.pricing.generated.resources.Res
import ampairsapp.feature.pricing.generated.resources.pricing_itemed_add_tier
import ampairsapp.feature.pricing.generated.resources.pricing_itemed_base_unit_price
import ampairsapp.feature.pricing.generated.resources.pricing_itemed_cancel
import ampairsapp.feature.pricing.generated.resources.pricing_itemed_change
import ampairsapp.feature.pricing.generated.resources.pricing_itemed_col_min_qty
import ampairsapp.feature.pricing.generated.resources.pricing_itemed_col_unit_price
import ampairsapp.feature.pricing.generated.resources.pricing_itemed_min_qty
import ampairsapp.feature.pricing.generated.resources.pricing_itemed_moq
import ampairsapp.feature.pricing.generated.resources.pricing_itemed_moq_hint
import ampairsapp.feature.pricing.generated.resources.pricing_itemed_no_product
import ampairsapp.feature.pricing.generated.resources.pricing_itemed_product_section
import ampairsapp.feature.pricing.generated.resources.pricing_itemed_range_plus
import ampairsapp.feature.pricing.generated.resources.pricing_itemed_remove_tier
import ampairsapp.feature.pricing.generated.resources.pricing_itemed_save
import ampairsapp.feature.pricing.generated.resources.pricing_itemed_search_product
import ampairsapp.feature.pricing.generated.resources.pricing_itemed_tester_pays
import ampairsapp.feature.pricing.generated.resources.pricing_itemed_tester_prefix
import ampairsapp.feature.pricing.generated.resources.pricing_itemed_tester_units
import ampairsapp.feature.pricing.generated.resources.pricing_itemed_tester_won_base
import ampairsapp.feature.pricing.generated.resources.pricing_itemed_tester_won_tier
import ampairsapp.feature.pricing.generated.resources.pricing_itemed_tiers_hint
import ampairsapp.feature.pricing.generated.resources.pricing_itemed_tiers_section
import ampairsapp.feature.pricing.generated.resources.pricing_itemed_title
import ampairsapp.feature.pricing.generated.resources.pricing_itemed_unit_price
import ampairsapp.feature.pricing.generated.resources.pricing_itemed_valid_ladder
import ampairsapp.feature.pricing.generated.resources.pricing_itemed_variant_sku

/** Format a quantity Double without a trailing ".0" when it is a whole number. */
private fun formatQty(q: Double): String = if (q % 1.0 == 0.0) q.toLong().toString() else q.toString()

@Composable
fun ItemEditorScreen(
    priceListId: String,
    itemId: String?,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ItemEditorViewModel = assistedMetroViewModel<ItemEditorViewModel, ItemEditorViewModel.Factory>(
        key = priceListId + "/" + (itemId ?: "new"),
    ) { create(priceListId, itemId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val locale = LocalAppLocale.current

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text(
                    text = stringResource(Res.string.pricing_itemed_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            item { ProductAndPriceCard(state, locale, viewModel) }
            item { TierEditorCard(state, locale, viewModel) }
            item { InlineTesterCard(state, locale, viewModel) }
        }

        SaveBar(
            canSave = state.selectedProduct != null && state.unitPrice.isNotBlank(),
            onCancel = onSaved,
            onSave = { viewModel.save(onSaved) },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun ProductAndPriceCard(state: ItemEditorUiState, locale: AppLocale, viewModel: ItemEditorViewModel) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(Res.string.pricing_itemed_product_section),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )

            val selected = state.selectedProduct
            if (selected != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(selected.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (selected.code.isNotBlank()) {
                                Text(selected.code, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        TextButton(onClick = viewModel::clearProduct) {
                            Text(stringResource(Res.string.pricing_itemed_change))
                        }
                    }
                }
                OutlinedTextField(
                    value = state.variantSku,
                    onValueChange = viewModel::updateVariantSku,
                    label = { Text(stringResource(Res.string.pricing_itemed_variant_sku)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    label = { Text(stringResource(Res.string.pricing_itemed_search_product)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (state.searchResults.isEmpty()) {
                    Text(
                        stringResource(Res.string.pricing_itemed_no_product),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                state.searchResults.take(6).forEach { product ->
                    ProductRow(product, locale, onClick = { viewModel.selectProduct(product) })
                }
            }

            // Base unit price + MOQ
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = state.unitPrice,
                    onValueChange = viewModel::updateUnitPrice,
                    label = { Text(stringResource(Res.string.pricing_itemed_base_unit_price)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1.3f),
                )
                OutlinedTextField(
                    value = state.moq,
                    onValueChange = viewModel::updateMoq,
                    label = { Text(stringResource(Res.string.pricing_itemed_moq)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                stringResource(Res.string.pricing_itemed_moq_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProductRow(product: ProductSummary, locale: AppLocale, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(product.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (product.code.isNotBlank()) {
                    Text(product.code, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(formatMoney(product.sellingPrice, locale = locale), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun TierEditorCard(state: ItemEditorUiState, locale: AppLocale, viewModel: ItemEditorViewModel) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Stairs, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    stringResource(Res.string.pricing_itemed_tiers_section),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                if (state.isLadderValid) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                        Text(
                            stringResource(Res.string.pricing_itemed_valid_ladder),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            VisualLadder(state, locale)

            // Column headers
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 4.dp)) {
                Text(stringResource(Res.string.pricing_itemed_col_min_qty), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Text(stringResource(Res.string.pricing_itemed_col_unit_price), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(40.dp))
            }

            state.tiers.forEachIndexed { index, tier ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = tier.minQty,
                        onValueChange = { viewModel.updateTierMinQty(index, it) },
                        label = { Text(stringResource(Res.string.pricing_itemed_min_qty)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = tier.unitPrice,
                        onValueChange = { viewModel.updateTierPrice(index, it) },
                        label = { Text(stringResource(Res.string.pricing_itemed_unit_price)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { viewModel.removeTier(index) }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(Res.string.pricing_itemed_remove_tier), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            OutlinedButton(onClick = viewModel::addTier, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(Res.string.pricing_itemed_add_tier))
            }

            Text(
                stringResource(Res.string.pricing_itemed_tiers_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * The visual quantity-tier ladder: one bar per rung (base price + each entered tier). Bar height is
 * proportional to the rung's price relative to the highest rung, so cheaper volume tiers read as
 * shorter bars. Each bar shows its price label and a "Nq+" range underneath.
 */
@Composable
private fun VisualLadder(state: ItemEditorUiState, locale: AppLocale) {
    val base = state.unitPrice.toDoubleOrNull() ?: 0.0
    // Rung 0 = base (from qty 1), then each parsed tier sorted ascending by minQty.
    val rungs = buildList {
        add(1.0 to base)
        addAll(state.parsedTiers.sortedBy { it.first })
    }
    val maxPrice = rungs.maxOfOrNull { it.second }?.takeIf { it > 0.0 } ?: 1.0

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            rungs.forEachIndexed { index, (minQty, price) ->
                val fraction = (price / maxPrice).coerceIn(0.15, 1.0)
                val barHeight = (40 + fraction * 80).dp
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.widthIn(min = 74.dp),
                ) {
                    Text(
                        formatMoney(price, locale = locale),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Box(
                        modifier = Modifier
                            .width(56.dp)
                            .height(barHeight)
                            .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 4.dp, bottomEnd = 4.dp)),
                    ) {
                        Surface(
                            color = if (index == 0) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxSize(),
                        ) {}
                    }
                    Text(
                        stringResource(Res.string.pricing_itemed_range_plus, formatQty(minQty)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun InlineTesterCard(state: ItemEditorUiState, locale: AppLocale, viewModel: ItemEditorViewModel) {
    val applied = state.testerAppliedTierMinQty
    val wonLabel = if (applied != null) {
        stringResource(Res.string.pricing_itemed_tester_won_tier, formatQty(applied))
    } else {
        stringResource(Res.string.pricing_itemed_tester_won_base)
    }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.Calculate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            IconButton(onClick = viewModel::decrementTesterQuantity) {
                Icon(Icons.Filled.Remove, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = viewModel::incrementTesterQuantity) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(Res.string.pricing_itemed_tester_prefix) + " ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        stringResource(Res.string.pricing_itemed_tester_units, formatQty(state.testerQuantity)),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        " " + stringResource(Res.string.pricing_itemed_tester_pays),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(wonLabel, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                formatMoney(state.testerUnitPrice, locale = locale),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun SaveBar(
    canSave: Boolean,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp, modifier = modifier.fillMaxWidth()) {
        Column {
            HorizontalDivider()
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(onClick = onCancel) {
                    Text(stringResource(Res.string.pricing_itemed_cancel))
                }
                Button(onClick = onSave, enabled = canSave, modifier = Modifier.weight(1f)) {
                    Text(stringResource(Res.string.pricing_itemed_save))
                }
            }
        }
    }
}
