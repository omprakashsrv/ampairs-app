package com.ampairs.cbmaintenance.ui.ticket

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ampairs.cbmaintenance.domain.model.Ticket
import dev.zacsweers.metrox.viewmodel.metroViewModel

@Composable
fun TicketListScreen(
    onRaiseTicket: () -> Unit,
    onTicketClick: (String) -> Unit,
    viewModel: TicketListViewModel = metroViewModel(),
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onRaiseTicket,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Raise ticket") },
            )
        },
        modifier = modifier.fillMaxSize(),
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Tickets",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = uiState.query,
                        onValueChange = viewModel::onSearch,
                        label = { Text("Search tickets") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            uiState.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) }
            if (uiState.tickets.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No tickets", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(uiState.tickets, key = { it.uid }) { ticket ->
                        TicketCard(
                            ticket = ticket,
                            storeLabel = uiState.storeNames[ticket.storeId] ?: ticket.storeId,
                            onClick = { onTicketClick(ticket.uid) },
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun TicketCard(ticket: Ticket, storeLabel: String, onClick: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Text("${ticket.assetCategory} · ${ticket.subCategory}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "$storeLabel · ${ticket.status}${if (ticket.originPmEntryId != null) " · from PM" else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ticket.description?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}
