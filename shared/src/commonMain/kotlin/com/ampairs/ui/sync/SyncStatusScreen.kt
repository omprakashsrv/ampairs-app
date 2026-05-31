package com.ampairs.ui.sync

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ampairs.common.model.formatDate
import com.ampairs.sync.EntitySyncState
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncLogEntry
import com.ampairs.sync.SyncStatus
import dev.zacsweers.metrox.viewmodel.metroViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncStatusScreen(
    onNavigateBack: () -> Unit,
    viewModel: SyncStatusViewModel = metroViewModel(),
) {
    val entityStates by viewModel.entityStates.collectAsStateWithLifecycle()
    val logs by viewModel.logs.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sync Status") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.triggerFullSync() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Sync all")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            item {
                Text(
                    text = "Entities",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    entityStates.forEachIndexed { index, state ->
                        if (index > 0) HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                        EntitySyncRow(state = state, onTrigger = { viewModel.triggerEntitySync(state.entity) })
                    }
                    if (entityStates.isEmpty()) {
                        Text(
                            text = "No sync data yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Sync Log (last ${logs.size})",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (logs.isEmpty()) {
                item {
                    Text(
                        text = "No sync activity yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            } else {
                items(logs.asReversed(), key = { it.id }) { entry ->
                    SyncLogRow(entry)
                }
            }
        }
    }
}

@Composable
private fun EntitySyncRow(state: EntitySyncState, onTrigger: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SyncStatusIndicator(status = state.status)

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = state.entity.displayName(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = state.status.displayLabel(),
                style = MaterialTheme.typography.bodySmall,
                color = state.status.labelColor(),
            )
            state.lastSyncedAt?.let { ts ->
                Text(
                    text = "Last synced ${ts.formatDate("dd MMM, hh:mm a")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (state.status is SyncStatus.Failed) {
                Text(
                    text = state.errorMessage ?: "Sync failed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        IconButton(onClick = onTrigger, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = "Sync ${state.entity.displayName()}",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SyncStatusIndicator(status: SyncStatus) {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing)),
    )

    val icon = when (status) {
        is SyncStatus.Syncing -> Icons.Default.Sync
        is SyncStatus.Success -> Icons.Default.CheckCircle
        is SyncStatus.Failed -> Icons.Default.Error
        is SyncStatus.PendingPush -> Icons.Default.CloudUpload
        is SyncStatus.PendingPull -> Icons.Default.HourglassEmpty
        is SyncStatus.Idle -> Icons.Default.CheckCircle
    }
    val tint: Color = when (status) {
        is SyncStatus.Syncing -> MaterialTheme.colorScheme.primary
        is SyncStatus.Success -> Color(0xFF4CAF50)
        is SyncStatus.Failed -> MaterialTheme.colorScheme.error
        is SyncStatus.PendingPush, is SyncStatus.PendingPull -> MaterialTheme.colorScheme.tertiary
        is SyncStatus.Idle -> MaterialTheme.colorScheme.outlineVariant
    }

    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = Modifier
            .size(24.dp)
            .then(if (status is SyncStatus.Syncing) Modifier.rotate(rotation) else Modifier),
    )
}

@Composable
private fun SyncLogRow(entry: SyncLogEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(entry.outcome.dotColor()),
        )

        Column(modifier = Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = entry.entity.displayName(),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                )
                Surface(
                    color = when (entry.direction) {
                        SyncLogEntry.Direction.PUSH -> MaterialTheme.colorScheme.tertiaryContainer
                        SyncLogEntry.Direction.PULL -> MaterialTheme.colorScheme.secondaryContainer
                    },
                    shape = RoundedCornerShape(4.dp),
                ) {
                    Text(
                        text = entry.direction.name,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                    )
                }
            }
            Text(
                text = entry.message,
                style = MaterialTheme.typography.bodySmall,
                color = if (entry.outcome == SyncLogEntry.Outcome.FAILURE)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Text(
            text = entry.timestamp.formatDate("HH:mm:ss"),
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// — Helpers

private fun SyncEntity.displayName(): String = name
    .split("_")
    .joinToString(" ") { it.lowercase().replaceFirstChar { c -> c.uppercase() } }

@Composable
private fun SyncStatus.displayLabel(): String = when (this) {
    is SyncStatus.Idle -> "Up to date"
    is SyncStatus.PendingPush -> "Pending push"
    is SyncStatus.PendingPull -> "Pending pull"
    is SyncStatus.Syncing -> "Syncing…"
    is SyncStatus.Success -> "Synced"
    is SyncStatus.Failed -> "Failed"
}

@Composable
private fun SyncStatus.labelColor(): Color = when (this) {
    is SyncStatus.Idle -> MaterialTheme.colorScheme.onSurfaceVariant
    is SyncStatus.PendingPush, is SyncStatus.PendingPull -> MaterialTheme.colorScheme.tertiary
    is SyncStatus.Syncing -> MaterialTheme.colorScheme.primary
    is SyncStatus.Success -> Color(0xFF4CAF50)
    is SyncStatus.Failed -> MaterialTheme.colorScheme.error
}

@Composable
private fun SyncLogEntry.Outcome.dotColor(): Color = when (this) {
    SyncLogEntry.Outcome.STARTED -> MaterialTheme.colorScheme.primary
    SyncLogEntry.Outcome.SUCCESS -> Color(0xFF4CAF50)
    SyncLogEntry.Outcome.FAILURE -> MaterialTheme.colorScheme.error
}
