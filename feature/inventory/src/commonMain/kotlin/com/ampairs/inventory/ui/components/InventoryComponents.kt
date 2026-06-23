package com.ampairs.inventory.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ampairsapp.feature.inventory.generated.resources.Res
import ampairsapp.feature.inventory.generated.resources.inv_error_desc
import ampairsapp.feature.inventory.generated.resources.inv_error_title
import ampairsapp.feature.inventory.generated.resources.inv_offline
import ampairsapp.feature.inventory.generated.resources.inv_offline_banner
import ampairsapp.feature.inventory.generated.resources.inv_pending
import ampairsapp.feature.inventory.generated.resources.inv_refresh
import ampairsapp.feature.inventory.generated.resources.inv_status_in_stock
import ampairsapp.feature.inventory.generated.resources.inv_status_inactive
import ampairsapp.feature.inventory.generated.resources.inv_status_low_stock
import ampairsapp.feature.inventory.generated.resources.inv_status_out_of_stock
import ampairsapp.feature.inventory.generated.resources.inv_synced
import ampairsapp.feature.inventory.generated.resources.retry
import com.ampairs.inventory.domain.InventoryItem
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs

// ───────────────────────── Stock status (color + icon + label — never color alone) ─────────────────────────

enum class StockStatus { IN_STOCK, LOW, OUT, INACTIVE }

fun InventoryItem.stockStatus(): StockStatus = when {
    !active -> StockStatus.INACTIVE
    isOutOfStock -> StockStatus.OUT
    isLowStock -> StockStatus.LOW
    else -> StockStatus.IN_STOCK
}

@Composable
fun StockStatus.color(): Color = when (this) {
    StockStatus.IN_STOCK -> MaterialTheme.colorScheme.primary
    StockStatus.LOW -> MaterialTheme.colorScheme.tertiary
    StockStatus.OUT -> MaterialTheme.colorScheme.error
    StockStatus.INACTIVE -> MaterialTheme.colorScheme.onSurfaceVariant
}

fun StockStatus.icon(): ImageVector = when (this) {
    StockStatus.IN_STOCK -> Icons.Filled.CheckCircle
    StockStatus.LOW -> Icons.Filled.Warning
    StockStatus.OUT -> Icons.Filled.Error
    StockStatus.INACTIVE -> Icons.Filled.Block
}

@Composable
fun StockStatus.label(): String = stringResource(
    when (this) {
        StockStatus.IN_STOCK -> Res.string.inv_status_in_stock
        StockStatus.LOW -> Res.string.inv_status_low_stock
        StockStatus.OUT -> Res.string.inv_status_out_of_stock
        StockStatus.INACTIVE -> Res.string.inv_status_inactive
    }
)

@Composable
fun StockBadge(status: StockStatus, modifier: Modifier = Modifier) {
    val c = status.color()
    Row(
        modifier = modifier
            .background(c.copy(alpha = 0.14f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(status.icon(), contentDescription = null, tint = c, modifier = Modifier.size(14.dp))
        Text(status.label(), style = MaterialTheme.typography.labelMedium, color = c)
    }
}

// ───────────────────────── Numeric helpers (tabular money / quantities) ─────────────────────────

/** Drops a trailing `.0` so whole quantities read as `24`, not `24.0`. */
fun formatQty(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()

/** Signed quantity for the ledger/timeline — `+12`, `−12`, or `0`. Uses a true minus glyph. */
fun signedQty(value: Double): String = when {
    value > 0.0 -> "+${formatQty(abs(value))}"
    value < 0.0 -> "−${formatQty(abs(value))}"
    else -> "0"
}

val MonoFamily = FontFamily.Monospace

// ───────────────────────── KPI card ─────────────────────────

@Composable
fun KpiCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    leading: ImageVector? = null,
) {
    Column(
        modifier = modifier
            .background(container, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (leading != null) Icon(leading, contentDescription = null, tint = contentColor, modifier = Modifier.size(15.dp))
            Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, color = contentColor.copy(alpha = 0.85f))
        }
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ───────────────────────── Sync chip ─────────────────────────

enum class SyncChipState { SYNCED, PENDING, OFFLINE }

@Composable
fun SyncChip(state: SyncChipState, modifier: Modifier = Modifier) {
    val (icon, labelRes, tint) = when (state) {
        SyncChipState.SYNCED -> Triple(Icons.Filled.CloudDone, Res.string.inv_synced, MaterialTheme.colorScheme.primary)
        SyncChipState.PENDING -> Triple(Icons.Filled.CloudQueue, Res.string.inv_pending, MaterialTheme.colorScheme.tertiary)
        SyncChipState.OFFLINE -> Triple(Icons.Filled.CloudOff, Res.string.inv_offline, MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Row(
        modifier = modifier
            .background(tint.copy(alpha = 0.14f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
        Text(stringResource(labelRes), style = MaterialTheme.typography.labelSmall, color = tint)
    }
}

// ───────────────────────── Banners ─────────────────────────

@Composable
fun OfflineBanner(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Filled.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        Text(stringResource(Res.string.inv_offline_banner), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Non-destructive concurrency banner: server/another device changed on-hand. */
@Composable
fun RefreshBanner(message: String, onRefresh: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Filled.Sync, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(18.dp))
        Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.weight(1f))
        OutlinedButton(onClick = onRefresh) {
            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(stringResource(Res.string.inv_refresh), modifier = Modifier.padding(start = 4.dp))
        }
    }
}

// ───────────────────────── Empty / error states ─────────────────────────

@Composable
fun InventoryEmptyState(
    icon: ImageVector,
    title: String,
    description: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(48.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 16.dp),
        )
        if (description != null) {
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        if (actionLabel != null && onAction != null) {
            Button(onClick = onAction, modifier = Modifier.padding(top = 20.dp)) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(actionLabel, modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

@Composable
fun InventoryErrorState(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Filled.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
        Text(stringResource(Res.string.inv_error_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
        Text(stringResource(Res.string.inv_error_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
        OutlinedButton(onClick = onRetry, modifier = Modifier.padding(top = 20.dp)) {
            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(stringResource(Res.string.retry), modifier = Modifier.padding(start = 6.dp))
        }
    }
}

/** Lightweight skeleton — shimmer-free placeholder rows (skeletons, not blocking spinners). */
@Composable
fun InventorySkeletonList(rows: Int = 6, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(rows) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(12.dp)),
            )
        }
    }
}

// ───────────────────────── Movement type → icon/label helpers ─────────────────────────

fun movementIcon(transactionType: String, sourceType: String?): ImageVector = when {
    sourceType == "ORDER" || sourceType == "INVOICE" || sourceType == "SALE" -> Icons.Filled.ShoppingCart
    transactionType == "STOCK_IN" -> Icons.Filled.Add
    transactionType == "STOCK_OUT" -> Icons.Filled.Remove
    transactionType == "COUNT" -> Icons.Filled.Inventory2
    else -> Icons.Filled.Sync
}

/** Best-effort human source label (e.g. "Sale · INV-1042"). */
fun sourceLabel(sourceType: String?, reference: String?): String = when (sourceType) {
    "ORDER", "INVOICE", "SALE" -> listOfNotNull("Sale", reference?.takeIf { it.isNotBlank() }).joinToString(" · ")
    "MANUAL", null -> "Manual"
    else -> sourceType.lowercase().replaceFirstChar { it.uppercase() }
}
