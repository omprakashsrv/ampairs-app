package com.ampairs.aiops.ui.activity

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ampairs.common.locale.LocalAppLocale
import com.ampairs.common.locale.formatDateTime
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import ampairsapp.feature.aiops.generated.resources.Res
import ampairsapp.feature.aiops.generated.resources.aiops_activity_cd_back
import ampairsapp.feature.aiops.generated.resources.aiops_activity_empty
import ampairsapp.feature.aiops.generated.resources.aiops_activity_pending
import ampairsapp.feature.aiops.generated.resources.aiops_activity_reverted
import ampairsapp.feature.aiops.generated.resources.aiops_activity_title
import ampairsapp.feature.aiops.generated.resources.aiops_activity_undo

@Composable
fun AiOpsActivityScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: AiOpsActivityViewModel = metroViewModel(),
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.aiops_activity_cd_back),
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    text = stringResource(Res.string.aiops_activity_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        if (state.pendingSuggestions > 0) {
            Text(
                text = pluralStringResource(
                    Res.plurals.aiops_activity_pending,
                    state.pendingSuggestions,
                    state.pendingSuggestions,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        if (state.items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(Res.string.aiops_activity_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.items, key = { it.decisionId }) { item ->
                    ActivityRow(item = item, onUndo = { viewModel.undo(item.decisionId) })
                    HorizontalDivider()
                }
            }
        }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
private fun ActivityRow(
    item: AiOpsActivityItem,
    onUndo: () -> Unit,
) {
    val locale = LocalAppLocale.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            val fieldLabel = item.field ?: item.capability
            Text(
                text = "$fieldLabel: ${item.before ?: "—"} → ${item.after ?: "—"}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "${item.capability} · ${formatDateTime(Instant.fromEpochMilliseconds(item.createdAt), locale)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(8.dp))
        if (item.reverted) {
            Text(
                text = stringResource(Res.string.aiops_activity_reverted),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else if (item.canUndo) {
            TextButton(onClick = onUndo) {
                Text(stringResource(Res.string.aiops_activity_undo))
            }
        }
    }
}
