package com.ampairs.cbmaintenance.ui.ticket

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ampairs.cbmaintenance.domain.model.PmEntry
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel

@Composable
fun TicketDetailScreen(
    ticketId: String,
    onDeleted: () -> Unit,
    viewModel: TicketDetailViewModel = assistedMetroViewModel<TicketDetailViewModel, TicketDetailViewModel.Factory>(
        key = ticketId,
    ) { create(ticketId) },
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val ticket = uiState.ticket
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.deleted) {
        if (uiState.deleted) onDeleted()
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ticket?") },
            text = { Text("This removes the ticket for everyone. Use this only for a ticket raised by mistake.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.deleteTicket()
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Ticket", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        if (ticket == null) {
            item { Text("Loading…", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            item {
                Surface(shape = MaterialTheme.shapes.medium, tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                        Text(
                            "${ticket.assetCategory} · ${ticket.subCategory}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Status: ${ticket.status}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        ticket.description?.let {
                            Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
            item {
                Button(
                    onClick = viewModel::createPmTask,
                    enabled = !uiState.isCreating && !uiState.isClosed,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (uiState.isCreating) "Creating…" else "Create PM task for this ticket")
                }
            }
            if (!uiState.isClosed) {
                item {
                    Button(
                        onClick = viewModel::closeTicket,
                        enabled = !uiState.isClosing,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (uiState.isClosing) "Closing…" else "Close ticket")
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = { confirmDelete = true },
                    enabled = !uiState.isDeleting,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (uiState.isDeleting) "Deleting…" else "Delete ticket (raised by mistake)",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            uiState.message?.let { item { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall) } }
            uiState.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) } }

            item {
                Text(
                    "PM tasks (${uiState.pmEntries.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (uiState.pmEntries.isEmpty()) {
                item { Text("No PM tasks yet", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(uiState.pmEntries, key = { it.uid }) { entry -> PmEntryRow(entry) }
            }
            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

@Composable
private fun PmEntryRow(entry: PmEntry) {
    Surface(shape = MaterialTheme.shapes.small, tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Text(entry.assetCategory, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text(
                "${entry.status} · ${entry.source.lowercase()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
