package com.ampairs.pricing.ui.offers

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ampairs.common.locale.LocalAppLocale
import com.ampairs.common.locale.formatMoney
import com.ampairs.pricing.domain.model.Offer
import com.ampairs.pricing.domain.model.OfferRewardType
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.stringResource
import ampairsapp.feature.pricing.generated.resources.Res
import ampairsapp.feature.pricing.generated.resources.pricing_offerslist_cd_delete
import ampairsapp.feature.pricing.generated.resources.pricing_offerslist_empty_create
import ampairsapp.feature.pricing.generated.resources.pricing_offerslist_empty_title
import ampairsapp.feature.pricing.generated.resources.pricing_offerslist_error_title
import ampairsapp.feature.pricing.generated.resources.pricing_offerslist_new_offer
import ampairsapp.feature.pricing.generated.resources.pricing_offerslist_reward_bogo
import ampairsapp.feature.pricing.generated.resources.pricing_offerslist_reward_flat
import ampairsapp.feature.pricing.generated.resources.pricing_offerslist_reward_free_gift
import ampairsapp.feature.pricing.generated.resources.pricing_offerslist_reward_free_shipping
import ampairsapp.feature.pricing.generated.resources.pricing_offerslist_reward_percent
import ampairsapp.feature.pricing.generated.resources.pricing_offerslist_reward_percent_cap
import ampairsapp.feature.pricing.generated.resources.pricing_offerslist_reward_tiered
import ampairsapp.feature.pricing.generated.resources.pricing_offerslist_title
import ampairsapp.feature.pricing.generated.resources.pricing_offerslist_usage
import ampairsapp.feature.pricing.generated.resources.pricing_offerslist_usage_unlimited
import ampairsapp.feature.pricing.generated.resources.pricing_offerslist_window_anytime
import ampairsapp.feature.pricing.generated.resources.pricing_offerslist_window_open_ended

@Composable
fun OffersListScreen(
    onOfferClick: (String) -> Unit,
    onAddOffer: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OffersListViewModel = metroViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddOffer,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(Res.string.pricing_offerslist_new_offer)) },
            )
        },
        modifier = modifier.fillMaxSize(),
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(Res.string.pricing_offerslist_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            HorizontalDivider()

            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                uiState.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(Res.string.pricing_offerslist_error_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                        Text(uiState.error ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                uiState.offers.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(stringResource(Res.string.pricing_offerslist_empty_title), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = onAddOffer) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(Res.string.pricing_offerslist_empty_create))
                        }
                    }
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(uiState.offers, key = { it.uid }) { offer ->
                        OfferCard(
                            offer = offer,
                            onClick = { onOfferClick(offer.uid) },
                            onDelete = { viewModel.deleteOffer(offer.uid) },
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun OfferCard(
    offer: Offer,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Name + reward summary + delete
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = offer.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = rewardSummary(offer),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(Res.string.pricing_offerslist_cd_delete),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            // Channel + status + coupon pills
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                Pill(
                    label = offer.channel.name,
                    bg = MaterialTheme.colorScheme.secondaryContainer,
                    fg = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Pill(
                    label = offer.status.name,
                    bg = MaterialTheme.colorScheme.tertiaryContainer,
                    fg = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                offer.couponCode?.takeIf { it.isNotBlank() }?.let { code ->
                    CouponBadge(code)
                }
            }

            // Time window + usage footer
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = timeWindow(offer),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = usageLabel(offer),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun rewardSummary(offer: Offer): String {
    val locale = LocalAppLocale.current
    return when (offer.rewardType) {
        OfferRewardType.PERCENT -> {
            val pct = formatPercent(offer.rewardPercent ?: 0.0)
            val cap = offer.rewardCapMinor
            if (cap != null) {
                stringResource(Res.string.pricing_offerslist_reward_percent_cap, pct, formatMoney(cap / 100.0, locale))
            } else {
                stringResource(Res.string.pricing_offerslist_reward_percent, pct)
            }
        }
        OfferRewardType.FLAT ->
            stringResource(Res.string.pricing_offerslist_reward_flat, formatMoney((offer.rewardFlatMinor ?: 0L) / 100.0, locale))
        OfferRewardType.BOGO ->
            stringResource(Res.string.pricing_offerslist_reward_bogo, offer.bogoBuyQty ?: 0, offer.bogoGetQty ?: 0)
        OfferRewardType.FREE_SHIPPING -> stringResource(Res.string.pricing_offerslist_reward_free_shipping)
        OfferRewardType.FREE_GIFT -> stringResource(Res.string.pricing_offerslist_reward_free_gift)
        OfferRewardType.TIERED -> stringResource(Res.string.pricing_offerslist_reward_tiered)
    }
}

@Composable
private fun usageLabel(offer: Offer): String {
    val total = offer.totalLimit
    return if (total != null) {
        stringResource(Res.string.pricing_offerslist_usage, offer.usedCount, total)
    } else {
        stringResource(Res.string.pricing_offerslist_usage_unlimited, offer.usedCount)
    }
}

@Composable
private fun timeWindow(offer: Offer): String {
    val start = offer.startsAt?.takeIf { it.isNotBlank() }?.let { datePart(it) }
    val end = offer.endsAt?.takeIf { it.isNotBlank() }?.let { datePart(it) }
    return when {
        start != null && end != null -> "$start – $end"
        start != null && end == null -> "$start · ${stringResource(Res.string.pricing_offerslist_window_open_ended)}"
        start == null && end != null -> "… – $end"
        else -> stringResource(Res.string.pricing_offerslist_window_anytime)
    }
}

@Composable
private fun CouponBadge(code: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.ConfirmationNumber,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(13.dp),
            )
            Text(code, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun Pill(label: String, bg: Color, fg: Color) {
    Surface(color = bg, shape = MaterialTheme.shapes.extraLarge) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = fg,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

/** Trim a trailing ".0" so 10.0 renders as "10" but 12.5 stays "12.5". */
private fun formatPercent(p: Double): String = if (p % 1.0 == 0.0) p.toLong().toString() else p.toString()

/** Take the date portion of an ISO-8601 timestamp ("2025-01-09T14:30:00" -> "2025-01-09"). */
private fun datePart(iso: String): String = iso.substringBefore('T')
