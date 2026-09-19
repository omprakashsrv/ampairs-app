package com.ampairs.aiops.ui.activity

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import org.jetbrains.compose.resources.stringResource
import ampairsapp.feature.aiops.generated.resources.Res
import ampairsapp.feature.aiops.generated.resources.aiops_activity_cd_back
import ampairsapp.feature.aiops.generated.resources.aiops_activity_empty
import ampairsapp.feature.aiops.generated.resources.aiops_activity_scan_cd
import ampairsapp.feature.aiops.generated.resources.aiops_activity_reverted
import ampairsapp.feature.aiops.generated.resources.aiops_activity_section_history
import ampairsapp.feature.aiops.generated.resources.aiops_activity_title
import ampairsapp.feature.aiops.generated.resources.aiops_activity_undo
import ampairsapp.feature.aiops.generated.resources.aiops_suggestion_accept
import ampairsapp.feature.aiops.generated.resources.aiops_suggestion_dismiss
import ampairsapp.feature.aiops.generated.resources.aiops_suggestions_header

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
                Spacer(Modifier.weight(1f))
                if (state.isScanning) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                } else {
                    IconButton(onClick = { viewModel.scan() }) {
                        Icon(
                            Icons.Default.TravelExplore,
                            contentDescription = stringResource(Res.string.aiops_activity_scan_cd),
                        )
                    }
                }
            }
        }

        if (state.items.isEmpty() && state.suggestions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(Res.string.aiops_activity_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (state.suggestions.isNotEmpty()) {
                    item(key = "header-suggestions") {
                        SectionHeader(stringResource(Res.string.aiops_suggestions_header))
                    }
                    items(state.suggestions, key = { "s-${it.findingId}" }) { suggestion ->
                        SuggestionRow(
                            suggestion = suggestion,
                            onAccept = { viewModel.acceptSuggestion(suggestion.findingId) },
                            onDismiss = { viewModel.dismissSuggestion(suggestion.findingId) },
                        )
                        HorizontalDivider()
                    }
                }
                if (state.items.isNotEmpty()) {
                    item(key = "header-history") {
                        SectionHeader(stringResource(Res.string.aiops_activity_section_history))
                    }
                    items(state.items, key = { "d-${it.decisionId}" }) { item ->
                        ActivityRow(item = item, onUndo = { viewModel.undo(item.decisionId) })
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun SuggestionRow(
    suggestion: AiOpsSuggestionItem,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
            text = suggestion.summary,
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = suggestion.capability,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.padding(top = 4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = onAccept) {
                Text(stringResource(Res.string.aiops_suggestion_accept))
            }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.aiops_suggestion_dismiss))
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
