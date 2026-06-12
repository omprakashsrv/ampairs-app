package com.ampairs.invoice.editor

import ampairsapp.feature.invoice.generated.resources.Res
import ampairsapp.feature.invoice.generated.resources.doc_head_customer
import ampairsapp.feature.invoice.generated.resources.doc_head_date
import ampairsapp.feature.invoice.generated.resources.doc_head_invoice_no
import ampairsapp.feature.invoice.generated.resources.doc_head_pricing
import ampairsapp.feature.invoice.generated.resources.doc_head_select_customer
import ampairsapp.feature.invoice.generated.resources.doc_head_walk_in
import ampairsapp.feature.invoice.generated.resources.doc_open_settings_cd
import ampairsapp.feature.invoice.generated.resources.doc_price_exclusive
import ampairsapp.feature.invoice.generated.resources.doc_price_inclusive
import ampairsapp.feature.invoice.generated.resources.doc_scenario_inter_short
import ampairsapp.feature.invoice.generated.resources.doc_scenario_intra_short
import ampairsapp.feature.invoice.generated.resources.doc_sync_failed
import ampairsapp.feature.invoice.generated.resources.doc_sync_offline
import ampairsapp.feature.invoice.generated.resources.doc_sync_retry
import ampairsapp.feature.invoice.generated.resources.doc_sync_synced
import ampairsapp.feature.invoice.generated.resources.doc_sync_syncing
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PriceCheck
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ampairs.tax.calculation.document.PriceMode
import org.jetbrains.compose.resources.stringResource

/**
 * Editor header band (spec 010 v2): customer/place-of-supply chip, date, the read-only workspace
 * price-mode indicator (navigates to business settings — there is NO toggle in the editor), and
 * the invoice-number-on-save preview.
 */
@Composable
fun DocEditorHeader(
    customer: DocCustomerUi?,
    dateLabel: String,
    priceMode: PriceMode,
    numberPreview: String?,
    onCustomerClick: () -> Unit,
    onOpenSettings: () -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    if (compact) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f)) { CustomerChip(customer, onCustomerClick, compact = true) }
            PriceModeChip(priceMode, onOpenSettings, compact = true)
        }
    } else {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(Modifier.weight(1f)) {
                Cap(stringResource(Res.string.doc_head_customer))
                CustomerChip(customer, onCustomerClick, compact = false)
            }
            Column {
                Cap(stringResource(Res.string.doc_head_date))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, cs.outlineVariant),
                    color = cs.surface,
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp).heightIn(min = 44.dp).widthIn(min = 130.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(dateLabel, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace, maxLines = 1)
                        Icon(Icons.Filled.Event, contentDescription = null, modifier = Modifier.size(18.dp).padding(start = 4.dp), tint = cs.onSurfaceVariant)
                    }
                }
            }
            Column {
                Cap(stringResource(Res.string.doc_head_pricing))
                PriceModeChip(priceMode, onOpenSettings, compact = false)
            }
            if (numberPreview != null) {
                Column {
                    Cap(stringResource(Res.string.doc_head_invoice_no))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, cs.outlineVariant),
                        color = cs.surface,
                    ) {
                        Box(Modifier.padding(horizontal = 12.dp).heightIn(min = 44.dp).widthIn(min = 140.dp), contentAlignment = Alignment.CenterStart) {
                            Text(
                                numberPreview,
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Monospace,
                                color = cs.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Cap(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 4.dp),
        maxLines = 1, overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun CustomerChip(customer: DocCustomerUi?, onClick: () -> Unit, compact: Boolean) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = cs.surfaceContainerHigh,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 6.dp).heightIn(min = 44.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(if (customer?.walkIn == true) cs.secondaryContainer else cs.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (customer == null || customer.walkIn) {
                    Icon(Icons.Filled.Person, contentDescription = null, tint = cs.onSecondaryContainer, modifier = Modifier.size(18.dp))
                } else {
                    Text(customer.initials, style = MaterialTheme.typography.labelMedium, color = cs.onPrimaryContainer)
                }
            }
            Column(Modifier.padding(horizontal = 10.dp).let { if (compact) it.weight(1f, fill = true) else it.widthIn(max = 260.dp) }) {
                Text(
                    customer?.name ?: stringResource(Res.string.doc_head_select_customer),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (customer == null) cs.primary else cs.onSurface,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                if (customer != null) {
                    val scenarioShort = stringResource(if (customer.intra) Res.string.doc_scenario_intra_short else Res.string.doc_scenario_inter_short)
                    Text(
                        listOfNotNull(
                            customer.stateLabel,
                            scenarioShort,
                            if (customer.walkIn) stringResource(Res.string.doc_head_walk_in) else null,
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (customer.intra) cs.onSurfaceVariant else cs.tertiary,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Icon(Icons.Filled.ExpandMore, contentDescription = null, tint = cs.onSurfaceVariant)
        }
    }
}

/** Read-only workspace price-mode indicator; tapping navigates to business settings. */
@Composable
private fun PriceModeChip(priceMode: PriceMode, onOpenSettings: () -> Unit, compact: Boolean) {
    val cs = MaterialTheme.colorScheme
    val settingsCd = stringResource(Res.string.doc_open_settings_cd)
    Surface(
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, cs.outlineVariant),
        color = cs.surface,
        modifier = Modifier.clickable(onClick = onOpenSettings),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp).heightIn(min = 44.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (priceMode == PriceMode.TAX_INCLUSIVE) Icons.Filled.PriceCheck else Icons.Filled.Percent,
                contentDescription = null,
                tint = cs.onSurfaceVariant,
                modifier = Modifier.size(17.dp),
            )
            if (!compact) {
                Text(
                    stringResource(if (priceMode == PriceMode.TAX_INCLUSIVE) Res.string.doc_price_inclusive else Res.string.doc_price_exclusive),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 8.dp),
                    maxLines = 1,
                )
            }
            Icon(Icons.Filled.Settings, contentDescription = settingsCd, tint = cs.onSurfaceVariant, modifier = Modifier.size(16.dp).padding(start = if (compact) 4.dp else 0.dp))
        }
    }
}

/** Sync chip: saved-offline → syncing → synced; failed offers inline retry (spec 010 states S10–S13). */
@Composable
fun DocSyncChip(state: DocSyncUi, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    if (state == DocSyncUi.NONE) return
    val cs = MaterialTheme.colorScheme
    val (icon, label, container, content) = when (state) {
        DocSyncUi.OFFLINE -> SyncChipSpec(Icons.Filled.CloudOff, stringResource(Res.string.doc_sync_offline), cs.secondaryContainer, cs.onSecondaryContainer)
        DocSyncUi.SYNCING -> SyncChipSpec(Icons.Filled.Sync, stringResource(Res.string.doc_sync_syncing), cs.tertiaryContainer, cs.onTertiaryContainer)
        DocSyncUi.SYNCED -> SyncChipSpec(Icons.Filled.CloudDone, stringResource(Res.string.doc_sync_synced), cs.primaryContainer, cs.onPrimaryContainer)
        DocSyncUi.FAILED -> SyncChipSpec(Icons.Filled.Error, stringResource(Res.string.doc_sync_failed), cs.errorContainer, cs.onErrorContainer)
        DocSyncUi.NONE -> return
    }
    Surface(
        color = container,
        shape = RoundedCornerShape(50),
        modifier = modifier.let { if (state == DocSyncUi.FAILED) it.clickable(onClick = onRetry) else it },
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(15.dp))
            Text("  $label", style = MaterialTheme.typography.labelSmall, color = content, maxLines = 1)
            if (state == DocSyncUi.FAILED) {
                Text(
                    "  " + stringResource(Res.string.doc_sync_retry),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = content,
                )
            }
        }
    }
}

private data class SyncChipSpec(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String,
    val container: androidx.compose.ui.graphics.Color,
    val content: androidx.compose.ui.graphics.Color,
)
