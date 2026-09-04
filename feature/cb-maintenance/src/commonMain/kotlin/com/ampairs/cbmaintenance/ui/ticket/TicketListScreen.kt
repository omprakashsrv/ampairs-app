package com.ampairs.cbmaintenance.ui.ticket

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.ampairs.cbmaintenance.domain.model.Ticket
import com.ampairs.cbmaintenance.ui.due.FilterDropdown
import com.ampairs.cbmaintenance.ui.due.SearchableFilterDropdown
import dev.zacsweers.metrox.viewmodel.metroViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TicketListScreen(
    onRaiseTicket: () -> Unit,
    onTicketClick: (String) -> Unit,
    viewModel: TicketListViewModel = metroViewModel(),
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var filtersExpanded by remember { mutableStateOf(false) }

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
                    Spacer(Modifier.height(8.dp))
                    // Collapsible multi-filter panel (status / outlet / asset). All active filters
                    // combine (AND) with the search box above.
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { filtersExpanded = !filtersExpanded },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.FilterList, contentDescription = null)
                            Text(
                                if (uiState.activeFilterCount > 0) "Filters (${uiState.activeFilterCount})" else "Filters",
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (uiState.activeFilterCount > 0) {
                                TextButton(onClick = viewModel::clearFilters) { Text("Clear") }
                            }
                            Icon(
                                if (filtersExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (filtersExpanded) "Collapse filters" else "Expand filters",
                            )
                        }
                    }
                    if (filtersExpanded) {
                        Text(
                            "Status",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TICKET_STATUSES.forEach { status ->
                                FilterChip(
                                    selected = status in uiState.statusFilters,
                                    onClick = { viewModel.onToggleStatus(status) },
                                    label = { Text(status.replace('_', ' ')) },
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        SearchableFilterDropdown(
                            label = "Outlet",
                            allLabel = "All outlets",
                            options = uiState.availableStores,
                            selectedValue = uiState.storeFilter,
                            onSelect = viewModel::onStoreFilter,
                        )
                        Spacer(Modifier.height(6.dp))
                        FilterDropdown(
                            label = "Asset",
                            allLabel = "All assets",
                            options = uiState.availableAssets.map { it to it },
                            selectedValue = uiState.assetFilter,
                            onSelect = viewModel::onAssetFilter,
                        )
                    }
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
                            doneBy = uiState.doneByLabels[ticket.uid],
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
private fun TicketCard(ticket: Ticket, storeLabel: String, doneBy: String?, onClick: () -> Unit) {
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
            if (!doneBy.isNullOrBlank()) {
                Text(
                    "Done by: $doneBy",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            ticket.description?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}
