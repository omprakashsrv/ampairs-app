package com.ampairs.business.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/**
 * Reusable Material 3 pattern language for the Business module, implementing the
 * Ampairs "Business Module" design handoff. These composables are deliberately
 * presentational and self-contained so other settings-style screens (Customers,
 * Products) can adopt the same language.
 *
 * Tokens only — every colour comes from [MaterialTheme.colorScheme]; no hardcoded hex.
 */

/** Offline-first sync status. Drives [BusinessSyncChip]. */
enum class BusinessSyncState { Synced, Syncing, Offline }

/** A single tappable settings entry on the Overview, with a current-value preview. */
data class BusinessSettingsEntry(
    val icon: ImageVector,
    val tone: BusinessRowTone,
    val title: String,
    val value: String,
    val missing: Boolean = false,
    val onClick: () -> Unit,
)

/** Lead-icon tint for a settings row, mapped to an M3 container colour role. */
enum class BusinessRowTone { Primary, Amber, Indigo }

/** One item in the Overview setup-completeness checklist. */
data class BusinessChecklistItem(
    val label: String,
    val hint: String,
    val done: Boolean,
    val onClick: () -> Unit,
)

/* ── Sync chip — offline-first status, never a blocking spinner ───────────── */

@Composable
fun BusinessSyncChip(state: BusinessSyncState, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val (container, content, icon, label) = when (state) {
        BusinessSyncState.Synced ->
            Quad(cs.surfaceContainerHigh, cs.onSurfaceVariant, Icons.Filled.CloudDone, "All changes synced")
        BusinessSyncState.Syncing ->
            Quad(cs.tertiaryContainer, cs.onTertiaryContainer, Icons.Filled.Sync, "Syncing…")
        BusinessSyncState.Offline ->
            Quad(cs.secondaryContainer, cs.onSecondaryContainer, Icons.Filled.CloudOff, "Offline · saved on device")
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(container)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = content)
    }
}

private data class Quad(
    val container: androidx.compose.ui.graphics.Color,
    val content: androidx.compose.ui.graphics.Color,
    val icon: ImageVector,
    val label: String,
)

/* ── Identity header — gradient hero with logo, name, type chips, contact ── */

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BusinessIdentityHeader(
    name: String,
    initials: String,
    logoUrl: String?,
    chips: List<Pair<ImageVector, String>>,
    contacts: List<Pair<ImageVector, String>>,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val onHero = cs.onPrimary
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cs.primary),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(cs.primary, cs.secondary)))
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(onHero.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                if (logoUrl != null) {
                    AsyncImage(
                        model = logoUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Text(
                        initials,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = onHero,
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                Text(
                    name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = onHero,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    chips.forEach { (icon, label) -> HeroChip(icon, label, onHero) }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    contacts.forEach { (icon, label) ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(icon, contentDescription = null, tint = onHero.copy(alpha = 0.92f), modifier = Modifier.size(17.dp))
                            Text(label, style = MaterialTheme.typography.bodyMedium, color = onHero.copy(alpha = 0.92f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroChip(icon: ImageVector, label: String, onHero: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(onHero.copy(alpha = 0.20f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(icon, contentDescription = null, tint = onHero, modifier = Modifier.size(15.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = onHero)
    }
}

/* ── Completeness card — progress ring + actionable checklist ──────────────── */

@Composable
fun BusinessCompletenessCard(
    items: List<BusinessChecklistItem>,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val done = items.count { it.done }
    val pct = if (items.isEmpty()) 0f else done.toFloat() / items.size
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ProgressRing(pct = pct)
                Column(modifier = Modifier.weight(1f)) {
                    Text("Finish setting up", style = MaterialTheme.typography.titleLarge, color = cs.onSurface)
                    Text(
                        "$done of ${items.size} done · complete these so invoices and tax documents look right.",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant,
                    )
                }
            }
            items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = item.onClick)
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (item.done) cs.primary else cs.surfaceContainerHighest),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            if (item.done) Icons.Filled.Check else Icons.Filled.Add,
                            contentDescription = null,
                            tint = if (item.done) cs.onPrimary else cs.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            item.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (item.done) cs.onSurfaceVariant else cs.onSurface,
                        )
                        Text(item.hint, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                    }
                    if (!item.done) {
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = cs.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressRing(pct: Float) {
    val cs = MaterialTheme.colorScheme
    val ring = cs.primary
    val track = cs.surfaceContainerHighest
    val pctText = (pct * 100).toInt()
    Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(56.dp)) {
            val stroke = 6.dp.toPx()
            drawArc(
                color = track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = ring,
                startAngle = -90f,
                sweepAngle = 360f * pct,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Text(
            "$pctText%",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = cs.onSurface,
        )
    }
}

/* ── Settings row — icon + title + current-value preview + chevron ─────────── */

@Composable
fun BusinessSettingsList(
    entries: List<BusinessSettingsEntry>,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer),
    ) {
        Column {
            entries.forEachIndexed { index, entry ->
                if (index > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(cs.outlineVariant),
                    )
                }
                BusinessSettingsRow(entry)
            }
        }
    }
}

@Composable
private fun BusinessSettingsRow(entry: BusinessSettingsEntry) {
    val cs = MaterialTheme.colorScheme
    val (leadBg, leadFg) = when (entry.tone) {
        BusinessRowTone.Primary -> cs.primaryContainer to cs.onPrimaryContainer
        BusinessRowTone.Amber -> cs.secondaryContainer to cs.onSecondaryContainer
        BusinessRowTone.Indigo -> cs.tertiaryContainer to cs.onTertiaryContainer
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = entry.onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(leadBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(entry.icon, contentDescription = null, tint = leadFg, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.title, style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
            Text(
                entry.value,
                style = MaterialTheme.typography.bodySmall,
                color = if (entry.missing) cs.secondary else cs.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = cs.onSurfaceVariant)
    }
}

/* ── Section header ───────────────────────────────────────────────────────── */

@Composable
fun BusinessSectionTitle(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, contentDescription = null, tint = cs.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Text(text, style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
    }
}
