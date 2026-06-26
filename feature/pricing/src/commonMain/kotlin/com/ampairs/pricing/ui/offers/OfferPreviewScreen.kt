package com.ampairs.pricing.ui.offers

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ampairs.common.locale.AppLocale
import com.ampairs.common.locale.LocalAppLocale
import com.ampairs.common.locale.formatMoney
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import org.jetbrains.compose.resources.stringResource
import ampairsapp.feature.pricing.generated.resources.Res
import ampairsapp.feature.pricing.generated.resources.pricing_offerp_applied_offer
import ampairsapp.feature.pricing.generated.resources.pricing_offerp_cap_note
import ampairsapp.feature.pricing.generated.resources.pricing_offerp_discount
import ampairsapp.feature.pricing.generated.resources.pricing_offerp_final
import ampairsapp.feature.pricing.generated.resources.pricing_offerp_line_qty
import ampairsapp.feature.pricing.generated.resources.pricing_offerp_not_found
import ampairsapp.feature.pricing.generated.resources.pricing_offerp_reward
import ampairsapp.feature.pricing.generated.resources.pricing_offerp_sample_cart
import ampairsapp.feature.pricing.generated.resources.pricing_offerp_sample_context
import ampairsapp.feature.pricing.generated.resources.pricing_offerp_subtotal
import ampairsapp.feature.pricing.generated.resources.pricing_offerp_title

/** Format a quantity Double without a trailing ".0" when it is a whole number. */
private fun formatQty(q: Double): String = if (q % 1.0 == 0.0) q.toLong().toString() else q.toString()

@Composable
fun OfferPreviewScreen(
    offerId: String,
    modifier: Modifier = Modifier,
    viewModel: OfferPreviewViewModel = assistedMetroViewModel<OfferPreviewViewModel, OfferPreviewViewModel.Factory>(
        key = offerId,
    ) { create(offerId) },
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
                Icon(Icons.Filled.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = stringResource(Res.string.pricing_offerp_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        HorizontalDivider()

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }
        if (state.notFound) {
            Box(Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(Res.string.pricing_offerp_not_found),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { SampleCartCard(state, locale) }
            item { AppliedOfferCard(state) }
            item { MathCard(state, locale) }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SampleCartCard(state: OfferPreviewUiState, locale: AppLocale) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.ShoppingCart, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
                Text(
                    stringResource(Res.string.pricing_offerp_sample_cart),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    stringResource(Res.string.pricing_offerp_sample_context),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            state.cartLines.forEach { line ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(line.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            stringResource(Res.string.pricing_offerp_line_qty, formatQty(line.qty), formatMoney(line.unitPrice, locale = locale)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        formatMoney(line.lineTotal, locale = locale),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun AppliedOfferCard(state: OfferPreviewUiState) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            Icon(Icons.Filled.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(20.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(Res.string.pricing_offerp_applied_offer),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    state.offerName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stringResource(Res.string.pricing_offerp_reward, state.rewardSummary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

@Composable
private fun MathCard(state: OfferPreviewUiState, locale: AppLocale) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(Res.string.pricing_offerp_subtotal), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatMoney(state.subtotal, locale = locale), style = MaterialTheme.typography.bodyMedium)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(Res.string.pricing_offerp_discount), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                Text(formatMoney(state.discount, locale = locale), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
            }

            if (state.capHit) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = MaterialTheme.shapes.extraLarge,
                ) {
                    Row(
                        Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(Icons.Filled.ContentCut, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(14.dp))
                        Text(
                            stringResource(Res.string.pricing_offerp_cap_note),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                }
            }

            HorizontalDivider()

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Text(
                    stringResource(Res.string.pricing_offerp_final),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    formatMoney(state.finalTotal, locale = locale),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
