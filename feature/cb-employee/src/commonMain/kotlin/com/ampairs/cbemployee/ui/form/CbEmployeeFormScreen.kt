package com.ampairs.cbemployee.ui.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ampairs.cbemployee.domain.model.Employee
import com.ampairs.cbemployee.domain.model.MaintenanceRoles
import com.ampairs.cbstore.domain.model.ZonalOffice
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel

@Composable
fun CbEmployeeFormScreen(
    employeeId: String?,
    onDone: () -> Unit,
    viewModel: CbEmployeeFormViewModel = assistedMetroViewModel<CbEmployeeFormViewModel, CbEmployeeFormViewModel.Factory>(
        key = employeeId ?: "new",
    ) { create(employeeId) },
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) onDone()
    }

    Column(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(if (uiState.isEdit) "Edit member" else "New member", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            value = uiState.employeeNo,
            onValueChange = viewModel::onEmployeeNo,
            label = { Text("Employee number") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = uiState.name,
            onValueChange = viewModel::onName,
            label = { Text("Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        RoleDropdown(selected = uiState.role, onSelected = viewModel::onRole)
        ManagerDropdown(
            selectedId = uiState.reportsToEmployeeId,
            options = uiState.managerOptions,
            onSelected = viewModel::onManager,
        )
        OutlinedTextField(
            value = uiState.mobile,
            onValueChange = viewModel::onMobile,
            label = { Text("Mobile") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
        )
        ZonalOfficeDropdown(
            selectedId = uiState.zonalOfficeId,
            options = uiState.zonalOfficeOptions,
            onSelected = viewModel::onZone,
        )
        uiState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(
            onClick = viewModel::save,
            enabled = uiState.isValid && !uiState.isSaving,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (uiState.isSaving) "Saving…" else "Save")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoleDropdown(selected: String, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    // ExposedDropdownMenuBox is the Material3 pattern for a read-only "picker" field: a bare
    // Modifier.clickable on an OutlinedTextField never fires because the text field consumes the
    // tap itself. menuAnchor() wires the field as the anchor and toggles the menu on tap.
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = selected.replace('_', ' '),
            onValueChange = {},
            readOnly = true,
            label = { Text("Role") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            MaintenanceRoles.ALL.forEach { role ->
                DropdownMenuItem(
                    text = { Text(role.replace('_', ' ')) },
                    onClick = {
                        onSelected(role)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManagerDropdown(
    selectedId: String,
    options: List<Employee>,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = options.firstOrNull { it.uid == selectedId }?.name ?: ""
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Reports to (optional)") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("— None —") },
                onClick = {
                    onSelected("")
                    expanded = false
                },
            )
            options.forEach { manager ->
                DropdownMenuItem(
                    text = { Text("${manager.name} · ${manager.role.replace('_', ' ')}") },
                    onClick = {
                        onSelected(manager.uid)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ZonalOfficeDropdown(
    selectedId: String,
    options: List<ZonalOffice>,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = options.firstOrNull { it.uid == selectedId }?.let(::zonalOfficeLabel) ?: ""
    // Query starts as the current selection's name; typing filters the list and re-shows the
    // menu. Selecting an option (or losing the typed text) snaps back to the selected name.
    var query by remember(selectedName) { mutableStateOf(selectedName) }
    val filtered = remember(query, options, selectedName) {
        if (query.isBlank() || query == selectedName) {
            options
        } else {
            options.filter { it.name.contains(query, ignoreCase = true) || it.city.contains(query, ignoreCase = true) }
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                expanded = true
            },
            label = { Text("Zonal office (optional)") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            singleLine = true,
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryEditable)
                .fillMaxWidth()
                .onFocusChanged { if (it.isFocused) expanded = true },
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (selectedId.isNotBlank()) {
                DropdownMenuItem(
                    text = { Text("None") },
                    onClick = {
                        onSelected("")
                        query = ""
                        expanded = false
                    },
                )
            }
            if (filtered.isEmpty()) {
                DropdownMenuItem(text = { Text("No matching zonal office") }, onClick = {}, enabled = false)
            }
            filtered.forEach { office ->
                DropdownMenuItem(
                    text = { Text(zonalOfficeLabel(office)) },
                    onClick = {
                        onSelected(office.uid)
                        query = zonalOfficeLabel(office)
                        expanded = false
                    },
                )
            }
        }
    }
}

// The zone's own name often already embeds the city (e.g. "Zonal Office - Delhi/NCR") —
// appending it again would read as a redundant "X · X".
private fun zonalOfficeLabel(office: ZonalOffice): String =
    if (office.name.contains(office.city, ignoreCase = true)) office.name else "${office.name} · ${office.city}"
