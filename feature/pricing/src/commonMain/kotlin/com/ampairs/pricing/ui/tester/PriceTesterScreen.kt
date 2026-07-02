package com.ampairs.pricing.ui.tester

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Stairs
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.ampairs.pricing.domain.model.PriceCandidate
import com.ampairs.pricing.domain.model.PriceSource
import com.ampairs.pricing.domain.model.SalesChannel
import com.ampairs.product.domain.ProductSummary
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.stringResource
import ampairsapp.feature.pricing.generated.resources.Res
import ampairsapp.feature.pricing.generated.resources.pricing_tester_channel_retail
import ampairsapp.feature.pricing.generated.resources.pricing_tester_channel_wholesale
import ampairsapp.feature.pricing.generated.resources.pricing_tester_context
import ampairsapp.feature.pricing.generated.resources.pricing_tester_change
import ampairsapp.feature.pricing.generated.resources.pricing_tester_effective
import ampairsapp.feature.pricing.generated.resources.pricing_tester_explain
import ampairsapp.feature.pricing.generated.resources.pricing_tester_explain_subtitle
import ampairsapp.feature.pricing.generated.resources.pricing_tester_line_total
import ampairsapp.feature.pricing.generated.resources.pricing_tester_moq_below
import ampairsapp.feature.pricing.generated.resources.pricing_tester_moq_ok
import ampairsapp.feature.pricing.generated.resources.pricing_tester_no_product
import ampairsapp.feature.pricing.generated.resources.pricing_tester_pincode
import ampairsapp.feature.pricing.generated.resources.pricing_tester_quantity
import ampairsapp.feature.pricing.generated.resources.pricing_tester_search_product
import ampairsapp.feature.pricing.generated.resources.pricing_tester_source_fallback
import ampairsapp.feature.pricing.generated.resources.pricing_tester_source_list
import ampairsapp.feature.pricing.generated.resources.pricing_tester_tier_applied
import ampairsapp.feature.pricing.generated.resources.pricing_tester_tier_base
import ampairsapp.feature.pricing.generated.resources.pricing_tester_title
import ampairsapp.feature.pricing.generated.resources.pricing_tester_units
import ampairsapp.feature.pricing.generated.resources.pricing_cd_decrement
import ampairsapp.feature.pricing.generated.resources.pricing_cd_increment

/** Format a quantity Double without a trailing ".0" when it is a whole number. */
private fun formatQty(q: Double): String = if (q % 1.0 == 0.0) q.toLong().toString() else q.toString()

@Composable
fun PriceTesterScreen(
    viewModel: PriceTesterViewModel = metroViewModel(),
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()
    val locale = LocalAppLocale.current

    Column(modifier = modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Filled.Science, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = stringResource(Res.string.pricing_tester_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        HorizontalDivider()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { ContextCard(state, viewModel) }
            item {
                if (state.selectedProduct == null || state.trace == null) {
                    EmptyResult()
                } else {
                    ResultCard(state, locale, onToggleExplain = viewModel::toggleExplain)
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun ContextCard(state: PriceTesterUiState, viewModel: PriceTesterViewModel) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(Res.string.pricing_tester_context),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )

            // Channel toggle
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.channel == SalesChannel.WHOLESALE,
                    onClick = { viewModel.setChannel(SalesChannel.WHOLESALE) },
                    label = { Text(stringResource(Res.string.pricing_tester_channel_wholesale)) },
                    modifier = Modifier.weight(1f),
                )
                FilterChip(
                    selected = state.channel == SalesChannel.RETAIL,
                    onClick = { viewModel.setChannel(SalesChannel.RETAIL) },
                    label = { Text(stringResource(Res.string.pricing_tester_channel_retail)) },
                    modifier = Modifier.weight(1f),
                )
            }

            // Product picker
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
                            Text(
                                formatMoney(selected.sellingPrice, locale = LocalAppLocale.current),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(onClick = { viewModel.onQueryChange("") }) {
                            Text(stringResource(Res.string.pricing_tester_change))
                        }
                    }
                }
            }
            if (selected == null) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    label = { Text(stringResource(Res.string.pricing_tester_search_product)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                state.searchResults.take(6).forEach { product ->
                    ProductRow(product, onClick = { viewModel.selectProduct(product) })
                }
            }

            // Quantity + pincode
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(Res.string.pricing_tester_quantity), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = viewModel::decrementQuantity) {
                            Icon(Icons.Filled.Remove, contentDescription = stringResource(Res.string.pricing_cd_decrement))
                        }
                        Text(formatQty(state.quantity), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        IconButton(onClick = viewModel::incrementQuantity) {
                            Icon(Icons.Filled.Add, contentDescription = stringResource(Res.string.pricing_cd_increment))
                        }
                    }
                }
                OutlinedTextField(
                    value = state.pincode,
                    onValueChange = viewModel::onPincodeChange,
                    label = { Text(stringResource(Res.string.pricing_tester_pincode)) },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null) },
                    modifier = Modifier.weight(1.2f),
                )
            }
        }
    }
}

@Composable
private fun ProductRow(product: ProductSummary, onClick: () -> Unit) {
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
            Text(formatMoney(product.sellingPrice, locale = LocalAppLocale.current), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun EmptyResult() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
            Text(
                stringResource(Res.string.pricing_tester_no_product),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ResultCard(
    state: PriceTesterUiState,
    locale: com.ampairs.common.locale.AppLocale,
    onToggleExplain: () -> Unit,
) {
    val trace = state.trace ?: return
    val resolution = trace.resolution
    val winningName = trace.candidates.firstOrNull { it.won }?.name

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column {
                    Text(stringResource(Res.string.pricing_tester_effective), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        formatMoney(resolution.effectiveUnitPrice, locale = locale),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        stringResource(Res.string.pricing_tester_line_total, stringResource(Res.string.pricing_tester_units, formatQty(state.quantity))),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        formatMoney(state.lineTotal, locale = locale),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // Source / tier / MOQ chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val sourceLabel = if (resolution.source == PriceSource.PRICE_LIST)
                    (winningName ?: stringResource(Res.string.pricing_tester_source_list))
                else stringResource(Res.string.pricing_tester_source_fallback)
                StatusPill(Icons.Filled.Sell, sourceLabel, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)

                val tierLabel = resolution.appliedTierMinQty?.let {
                    stringResource(Res.string.pricing_tester_tier_applied, formatQty(it))
                } ?: stringResource(Res.string.pricing_tester_tier_base)
                StatusPill(Icons.Filled.Stairs, tierLabel, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)

                if (resolution.belowMoq) {
                    StatusPill(Icons.Filled.Close, stringResource(Res.string.pricing_tester_moq_below), MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
                } else {
                    StatusPill(Icons.Filled.CheckCircle, stringResource(Res.string.pricing_tester_moq_ok), MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Explain toggle
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().clickable(onClick = onToggleExplain),
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(Res.string.pricing_tester_explain),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        if (state.explainOpen) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            AnimatedVisibility(visible = state.explainOpen) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(Res.string.pricing_tester_explain_subtitle, trace.evaluatedCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    trace.candidates.forEachIndexed { index, candidate ->
                        CandidateRow(index + 1, candidate)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, bg: androidx.compose.ui.graphics.Color, fg: androidx.compose.ui.graphics.Color) {
    Surface(color = bg, shape = MaterialTheme.shapes.extraLarge) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(15.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun CandidateRow(rank: Int, candidate: PriceCandidate) {
    val containerColor = if (candidate.won) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
    val onContainer = if (candidate.won) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    Surface(color = containerColor, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            Surface(color = MaterialTheme.colorScheme.surfaceContainerHighest, shape = CircleShape, modifier = Modifier.size(24.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text("$rank", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(candidate.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = onContainer, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    Icon(
                        if (candidate.won || candidate.matched) Icons.Filled.CheckCircle else Icons.Filled.Close,
                        contentDescription = null,
                        tint = if (candidate.won) MaterialTheme.colorScheme.primary else if (candidate.matched) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Text(candidate.reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
