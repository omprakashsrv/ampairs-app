package com.ampairs.cbmaintenance.ui.due

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ampairs.cbemployee.domain.model.Employee
import com.ampairs.cbmaintenance.domain.model.PmEntry
import dev.zacsweers.metrox.viewmodel.metroViewModel

/** Open PM statuses shown as multi-select filter chips (value -> display label). */
private val PM_STATUSES = listOf(
    "DUE" to "Due",
    "OVERDUE" to "Overdue",
    "ASSIGNED" to "Assigned",
    "IN_PROGRESS" to "In progress",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PmDueListScreen(
    onOpenTickets: () -> Unit,
    onOpenSchedules: () -> Unit,
    onOpenReport: () -> Unit,
    viewModel: PmDueListViewModel = metroViewModel(),
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // The PM entry being completed, and whether that completion reports an issue.
    var completeFor by remember { mutableStateOf<String?>(null) }
    var issueMode by remember { mutableStateOf(false) }
    // The PM entry being (re)assigned.
    var assignFor by remember { mutableStateOf<String?>(null) }
    var filtersExpanded by remember { mutableStateOf(false) }

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
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = uiState.query,
                        onValueChange = viewModel::onSearch,
                        label = { Text("Search due PM") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    // Collapsible multi-filter panel (status / assignment / outlet / asset). All active
                    // filters combine (AND) with the search box above.
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
                            PM_STATUSES.forEach { (value, label) ->
                                FilterChip(
                                    selected = value in uiState.statusFilters,
                                    onClick = { viewModel.onToggleStatus(value) },
                                    label = { Text(label) },
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Assignment",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = uiState.showUnassigned,
                                onClick = viewModel::onToggleUnassigned,
                                label = { Text("Unassigned") },
                            )
                            FilterChip(
                                selected = uiState.showAssigned,
                                onClick = viewModel::onToggleAssigned,
                                label = { Text("Assigned") },
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        FilterDropdown(
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
            uiState.message?.let { Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) }
            uiState.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) }
            if (uiState.entries.isEmpty()) {
                val filtering = uiState.activeFilterCount > 0 || uiState.query.isNotBlank()
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (filtering) "No PM tasks match your filters" else "Nothing due right now",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(uiState.entries, key = { it.uid }) { entry ->
                        val assignee = entry.assignedToEmployeeId?.let { id ->
                            uiState.employees.firstOrNull { it.uid == id }?.let { it.name.ifBlank { it.employeeNo } } ?: id
                        }
                        PmEntryCard(
                            entry = entry,
                            storeLabel = uiState.storeLabels[entry.storeId] ?: entry.storeId,
                            ticketLabel = entry.ticketId?.let { uiState.ticketLabels[it] ?: it },
                            assigneeLabel = assignee,
                            onAssign = { assignFor = entry.uid },
                            onOk = { completeFor = entry.uid; issueMode = false },
                            onIssue = { completeFor = entry.uid; issueMode = true },
                        )
                    }
                    item { Spacer(Modifier.height(40.dp)) }
                }
            }
        }
    }

    val entryId = completeFor
    if (entryId != null) {
        CompletePmDialog(
            issueMode = issueMode,
            employees = uiState.employees,
            onDismiss = { completeFor = null },
            onConfirm = { issue, doneBy, assisted ->
                if (issueMode) viewModel.reportIssue(entryId, issue, doneBy, assisted)
                else viewModel.markAllOk(entryId, doneBy, assisted)
                completeFor = null
            },
        )
    }

    val assignEntryId = assignFor
    if (assignEntryId != null) {
        val entry = uiState.entries.firstOrNull { it.uid == assignEntryId }
        // Same-zone pool: employees whose zone matches the entry's (all, if the entry has no zone).
        val zone = entry?.zonalOfficeId.orEmpty()
        val inZone = uiState.employees.filter { zone.isBlank() || it.zonalOfficeId == zone }
        AssignPmDialog(
            employees = inZone,
            onDismiss = { assignFor = null },
            onPick = { employeeId ->
                viewModel.assign(assignEntryId, employeeId)
                assignFor = null
            },
        )
    }
}

/** Assign a due PM to an employee in the same zone (self-assign = pick yourself). */
@Composable
private fun AssignPmDialog(
    employees: List<Employee>,
    onDismiss: () -> Unit,
    onPick: (employeeId: String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assign to") },
        text = {
            if (employees.isEmpty()) {
                Text("No employees in this zone yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Column(
                    modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                ) {
                    employees.forEach { emp ->
                        Text(
                            emp.name.ifBlank { emp.employeeNo },
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxWidth().clickable { onPick(emp.uid) }.padding(vertical = 12.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/**
 * Completion sheet for a PM task. In OK mode it just records who did it and who helped; in issue
 * mode it also captures the failed check (which spawns a ticket server-side). "Done by" and
 * "Assisted by" both feed the PM compliance report.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompletePmDialog(
    issueMode: Boolean,
    employees: List<Employee>,
    onDismiss: () -> Unit,
    onConfirm: (issue: String, doneBy: String?, assisted: List<String>) -> Unit,
) {
    var issueText by remember { mutableStateOf("") }
    var doneBy by remember { mutableStateOf("") }
    var assisted by remember { mutableStateOf(setOf<String>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (issueMode) "Report an issue" else "Complete PM") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (issueMode) {
                    OutlinedTextField(
                        value = issueText,
                        onValueChange = { issueText = it },
                        label = { Text("What failed? (e.g. Gasket broken)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (employees.isNotEmpty()) {
                    DoneByDropdown(
                        employees = employees,
                        selectedId = doneBy,
                        onSelected = { doneBy = it },
                    )
                    Text("Assisted by (optional)", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        employees.forEach { emp ->
                            val on = emp.uid in assisted
                            FilterChip(
                                selected = on,
                                onClick = {
                                    assisted = if (on) assisted - emp.uid else assisted + emp.uid
                                },
                                label = { Text(emp.name.ifBlank { emp.employeeNo }) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(issueText, doneBy.ifBlank { null }, assisted.toList()) },
                enabled = !issueMode || issueText.isNotBlank(),
            ) { Text(if (issueMode) "Complete & raise ticket" else "Mark done") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Single-select filter dropdown with an "all" reset option (value -> display label). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterDropdown(
    label: String,
    allLabel: String,
    options: List<Pair<String, String>>,
    selectedValue: String?,
    onSelect: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == selectedValue }?.second ?: allLabel
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(allLabel) },
                onClick = { onSelect(null); expanded = false },
            )
            options.forEach { (value, display) ->
                DropdownMenuItem(
                    text = { Text(display) },
                    onClick = { onSelect(value); expanded = false },
                )
            }
        }
    }
}

/** Single-select "Done by" employee dropdown (with a clear "—" option). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DoneByDropdown(employees: List<Employee>, selectedId: String, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = employees.firstOrNull { it.uid == selectedId }?.let { it.name.ifBlank { it.employeeNo } } ?: ""
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Done by") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("—") },
                onClick = { onSelected(""); expanded = false },
            )
            employees.forEach { emp ->
                DropdownMenuItem(
                    text = { Text(emp.name.ifBlank { emp.employeeNo }) },
                    onClick = { onSelected(emp.uid); expanded = false },
                )
            }
        }
    }
}

/** Small color-coded pill for a PM entry's status (due amber, overdue red, done green, …). */
@Composable
private fun StatusBadge(status: String) {
    val (bg, fg) = statusColors(status)
    Surface(color = bg, contentColor = fg, shape = MaterialTheme.shapes.small) {
        Text(
            statusLabel(status),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun statusColors(status: String): Pair<Color, Color> = when (status.uppercase()) {
    "OVERDUE" -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    "DUE" -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    "ASSIGNED", "IN_PROGRESS" ->
        MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    // Completed — a fixed green (no green token in the M3 scheme); translucent bg reads in both themes.
    "DONE", "COMPLETED" -> Color(0xFF2E7D32).copy(alpha = 0.18f) to Color(0xFF2E7D32)
    else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
}

private fun statusLabel(status: String): String = when (status.uppercase()) {
    "IN_PROGRESS" -> "In progress"
    else -> status.lowercase().replaceFirstChar { it.uppercase() }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PmEntryCard(
    entry: PmEntry,
    storeLabel: String,
    ticketLabel: String?,
    assigneeLabel: String?,
    onAssign: () -> Unit,
    onOk: () -> Unit,
    onIssue: () -> Unit,
) {
    Surface(shape = MaterialTheme.shapes.medium, tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    entry.assetCategory,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                StatusBadge(entry.status)
            }
            Text(
                "$storeLabel${entry.dueDate?.let { " · due ${it.take(10)}" } ?: ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                if (assigneeLabel.isNullOrBlank()) "Unassigned" else "Assigned: $assigneeLabel",
                style = MaterialTheme.typography.bodySmall,
                color = if (assigneeLabel.isNullOrBlank()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (ticketLabel != null) {
                Text(
                    "For ticket: $ticketLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onOk) { Text("Mark OK") }
                OutlinedButton(onClick = onIssue) { Text("Report issue") }
                OutlinedButton(onClick = onAssign) { Text(if (assigneeLabel.isNullOrBlank()) "Assign" else "Reassign") }
            }
        }
    }
}
