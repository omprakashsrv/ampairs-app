package com.ampairs.printing.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ampairsapp.feature.printing.generated.resources.Res
import ampairsapp.feature.printing.generated.resources.printing_clear_all
import ampairsapp.feature.printing.generated.resources.printing_delete
import ampairsapp.feature.printing.generated.resources.printing_mark_printed
import ampairsapp.feature.printing.generated.resources.printing_no_jobs
import ampairsapp.feature.printing.generated.resources.printing_queue_title
import ampairsapp.feature.printing.generated.resources.printing_retry
import com.ampairs.printing.core.model.PrintJobState
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintQueueScreen(
    viewModel: PrintQueueViewModel = metroViewModel(),
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.printing_queue_title)) },
                actions = {
                    if (state.jobs.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearAll() }) {
                            Icon(
                                Icons.Filled.DeleteSweep,
                                contentDescription = stringResource(Res.string.printing_clear_all),
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (state.jobs.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(Res.string.printing_no_jobs), style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.jobs, key = { it.id }) { job ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth().padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "${job.documentType} · ${job.documentId}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    val attempts = if (job.attempts > 0) " · ${job.attempts} attempt(s)" else ""
                                    Text(
                                        "${job.state.name}$attempts",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    job.lastError?.let {
                                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                                AssistChip(onClick = {}, label = { Text(job.state.shortLabel()) })
                                IconButton(onClick = { viewModel.delete(job.id) }) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = stringResource(Res.string.printing_delete),
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                if (job.state == PrintJobState.UNCONFIRMED) {
                                    TextButton(onClick = { viewModel.markPrinted(job.id) }) {
                                        Text(stringResource(Res.string.printing_mark_printed))
                                    }
                                }
                                if (job.state.isRetryable()) {
                                    TextButton(onClick = { viewModel.retry(job.id) }) {
                                        Text(stringResource(Res.string.printing_retry))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun PrintJobState.isRetryable(): Boolean =
    this == PrintJobState.FAILED || this == PrintJobState.DEAD || this == PrintJobState.UNCONFIRMED

private fun PrintJobState.shortLabel(): String = when (this) {
    PrintJobState.QUEUED -> "Queued"
    PrintJobState.SENDING -> "Sending"
    PrintJobState.SENT -> "Sent"
    PrintJobState.CONFIRMED -> "Printed"
    PrintJobState.FAILED -> "Failed"
    PrintJobState.UNCONFIRMED -> "Check"
    PrintJobState.DEAD -> "Dead"
}
