package com.ampairs.printing.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ampairsapp.feature.printing.generated.resources.Res
import ampairsapp.feature.printing.generated.resources.printing_all_caught_up
import ampairsapp.feature.printing.generated.resources.printing_attempts
import ampairsapp.feature.printing.generated.resources.printing_cancel
import ampairsapp.feature.printing.generated.resources.printing_clear_all
import ampairsapp.feature.printing.generated.resources.printing_clear_all_body
import ampairsapp.feature.printing.generated.resources.printing_clear_all_title
import ampairsapp.feature.printing.generated.resources.printing_delete
import ampairsapp.feature.printing.generated.resources.printing_mark_printed
import ampairsapp.feature.printing.generated.resources.printing_no_jobs
import ampairsapp.feature.printing.generated.resources.printing_no_jobs_body
import ampairsapp.feature.printing.generated.resources.printing_queue_awaiting
import ampairsapp.feature.printing.generated.resources.printing_queue_failed
import ampairsapp.feature.printing.generated.resources.printing_queue_in_progress
import ampairsapp.feature.printing.generated.resources.printing_queue_title
import ampairsapp.feature.printing.generated.resources.printing_retry
import com.ampairs.printing.core.model.DocumentType
import com.ampairs.printing.core.model.PrintJobState
import com.ampairs.printing.core.spool.PrintJob
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintQueueScreen(
    viewModel: PrintQueueViewModel = metroViewModel(),
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()
    var showClearAll by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.printing_queue_title)) },
                actions = {
                    if (state.jobs.isNotEmpty()) {
                        TextButton(onClick = { showClearAll = true }) {
                            Icon(Icons.Filled.ClearAll, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(6.dp))
                            Text(stringResource(Res.string.printing_clear_all), color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (state.jobs.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.TaskAlt,
                title = stringResource(Res.string.printing_all_caught_up),
                body = stringResource(Res.string.printing_no_jobs_body),
                iconContainer = successColor.copy(alpha = 0.16f),
                iconContent = successColor,
                modifier = Modifier.padding(padding),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 24.dp, top = 4.dp),
            ) {
                item { StatusFilters(state.jobs) }
                items(state.jobs, key = { it.id }) { job ->
                    JobCard(
                        job = job,
                        onRetry = { viewModel.retry(job.id) },
                        onMarkPrinted = { viewModel.markPrinted(job.id) },
                        onDelete = { viewModel.delete(job.id) },
                    )
                }
            }
        }
    }

    if (showClearAll) {
        ClearAllDialog(
            onConfirm = {
                viewModel.clearAll()
                showClearAll = false
            },
            onDismiss = { showClearAll = false },
        )
    }
}

@Composable
private fun StatusFilters(jobs: List<PrintJob>) {
    val inProgress = jobs.count { it.state == PrintJobState.SENDING }
    val awaiting = jobs.count {
        it.state == PrintJobState.SENT || it.state == PrintJobState.UNCONFIRMED || it.state == PrintJobState.QUEUED
    }
    val failed = jobs.count { it.state == PrintJobState.FAILED || it.state == PrintJobState.DEAD }
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CountChip(stringResource(Res.string.printing_queue_in_progress, inProgress), MaterialTheme.colorScheme.primary)
        CountChip(stringResource(Res.string.printing_queue_awaiting, awaiting), warningColor)
        CountChip(stringResource(Res.string.printing_queue_failed, failed), MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun CountChip(label: String, color: Color) {
    Box(
        Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.13f)).padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = color)
    }
}

@Composable
private fun JobCard(
    job: PrintJob,
    onRetry: () -> Unit,
    onMarkPrinted: () -> Unit,
    onDelete: () -> Unit,
) {
    val visual = job.state.statusVisual
    Card(
        Modifier.fillMaxWidth().padding(bottom = 10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(Modifier.fillMaxWidth().padding(13.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.width(3.dp).height(56.dp).clip(RoundedCornerShape(2.dp)).background(visual.color),
            )
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(job.docTypeIcon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    Text(
                        "${job.docTypeLabel} · ${job.documentId}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                    StatusChip(visual.label, visual.icon, visual.color)
                }
                val attempts = if (job.attempts > 0) " · ${stringResource(Res.string.printing_attempts, job.attempts)}" else ""
                Text(
                    "${job.formattedTime}$attempts",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 28.dp, top = 6.dp),
                )
                job.lastError?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 28.dp, top = 2.dp),
                    )
                }

                val canMarkPrinted = job.state == PrintJobState.UNCONFIRMED
                val canRetry = job.state == PrintJobState.FAILED || job.state == PrintJobState.DEAD || job.state == PrintJobState.UNCONFIRMED
                val canDelete = job.state != PrintJobState.SENDING
                if (canMarkPrinted || canRetry || canDelete) {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 28.dp, top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (canMarkPrinted) {
                            FilledTonalButton(onClick = onMarkPrinted, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.size(6.dp))
                                Text(stringResource(Res.string.printing_mark_printed), style = MaterialTheme.typography.labelLarge)
                            }
                        }
                        if (canRetry) {
                            OutlinedButton(onClick = onRetry, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.size(6.dp))
                                Text(stringResource(Res.string.printing_retry), style = MaterialTheme.typography.labelLarge)
                            }
                        }
                        if (canDelete) {
                            TextButton(onClick = onDelete, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                                Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(17.dp), tint = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.size(6.dp))
                                Text(stringResource(Res.string.printing_delete), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(label: String, icon: ImageVector, color: Color) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.14f)).padding(start = 8.dp, end = 10.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = color)
    }
}

@Composable
private fun ClearAllDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.ClearAll, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text(stringResource(Res.string.printing_clear_all_title)) },
        text = { Text(stringResource(Res.string.printing_clear_all_body)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(Res.string.printing_clear_all), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.printing_cancel)) }
        },
    )
}

/** Resolve the job's document type (stored as a String key or enum name) for display. */
private fun resolveDocType(raw: String): DocumentType? =
    DocumentType.entries.firstOrNull { it.key.equals(raw, ignoreCase = true) || it.name.equals(raw, ignoreCase = true) }

private val PrintJob.docTypeIcon: ImageVector
    get() = resolveDocType(documentType)?.icon ?: Icons.Filled.Description

private val PrintJob.docTypeLabel: String
    get() = resolveDocType(documentType)?.displayName ?: documentType

private val PrintJob.formattedTime: String
    get() {
        val dt = Instant.fromEpochMilliseconds(createdAtMillis).toLocalDateTime(TimeZone.currentSystemDefault())
        val hour = dt.hour.toString().padStart(2, '0')
        val minute = dt.minute.toString().padStart(2, '0')
        return "$hour:$minute"
    }
