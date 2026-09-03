package com.ampairs.cbmaintenance.ui.due

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ampairs.cbmaintenance.domain.model.PmEntry
import dev.zacsweers.metrox.viewmodel.metroViewModel

@Composable
fun PmDueListScreen(
    onOpenTickets: () -> Unit,
    onOpenSchedules: () -> Unit,
    onOpenReport: () -> Unit,
    viewModel: PmDueListViewModel = metroViewModel(),
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var issueFor by remember { mutableStateOf<String?>(null) }
    var issueText by remember { mutableStateOf("") }

    Scaffold(modifier = modifier.fillMaxSize()) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Maintenance — Due & Overdue",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = onOpenReport) {
                            Icon(Icons.Default.Assessment, contentDescription = "PM compliance report")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onOpenTickets) { Text("Tickets") }
                        OutlinedButton(onClick = onOpenSchedules) { Text("PM Schedules") }
                        OutlinedButton(onClick = viewModel::generate, enabled = !uiState.isGenerating) {
                            Text(if (uiState.isGenerating) "Generating…" else "Generate PM")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = uiState.query,
                        onValueChange = viewModel::onSearch,
                        label = { Text("Search due PM") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            uiState.message?.let { Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) }
            uiState.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) }
            if (uiState.entries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nothing due right now", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(uiState.entries, key = { it.uid }) { entry ->
                        PmEntryCard(
                            entry = entry,
                            onOk = { viewModel.markAllOk(entry.uid) },
                            onIssue = { issueFor = entry.uid; issueText = "" },
                        )
                    }
                    item { Spacer(Modifier.height(40.dp)) }
                }
            }
        }
    }

    val currentIssueFor = issueFor
    if (currentIssueFor != null) {
        AlertDialog(
            onDismissRequest = { issueFor = null },
            title = { Text("Report an issue") },
            text = {
                OutlinedTextField(
                    value = issueText,
                    onValueChange = { issueText = it },
                    label = { Text("What failed? (e.g. Gasket broken)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.reportIssue(currentIssueFor, issueText)
                        issueFor = null
                    },
                    enabled = issueText.isNotBlank(),
                ) { Text("Complete & raise ticket") }
            },
            dismissButton = { TextButton(onClick = { issueFor = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun PmEntryCard(entry: PmEntry, onOk: () -> Unit, onIssue: () -> Unit) {
    Surface(shape = MaterialTheme.shapes.medium, tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Text(entry.assetCategory, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Store ${entry.storeId} · ${entry.status}${entry.dueDate?.let { " · due ${it.take(10)}" } ?: ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!entry.ticketId.isNullOrBlank()) {
                Text(
                    "For ticket ${entry.ticketId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onOk) { Text("Mark OK") }
                OutlinedButton(onClick = onIssue) { Text("Report issue") }
            }
        }
    }
}
