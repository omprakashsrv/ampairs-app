package com.ampairs.cbmaintenance.ui.ticket

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zacsweers.metrox.viewmodel.metroViewModel

@Composable
fun RaiseTicketScreen(
    onDone: () -> Unit,
    viewModel: RaiseTicketViewModel = metroViewModel(),
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
        Text("Raise a ticket", style = MaterialTheme.typography.headlineSmall)
        StoreDropdown(
            selectedId = uiState.storeId,
            options = uiState.storeOptions.map { it.uid to "${it.code} · ${it.name}" },
            onSelected = viewModel::onStore,
        )
        // Cascading classification pickers, sourced from the ticket-bucket catalog.
        // Progressive disclosure: each level appears only after its parent is chosen AND only when
        // that level actually has options — so empty levels (e.g. no second issue detail) never show.
        if (uiState.departmentOptions.isNotEmpty()) {
            PickerDropdown(
                label = "Department",
                selected = uiState.department,
                options = uiState.departmentOptions,
                onSelected = viewModel::onDepartment,
            )
        }
        if (uiState.department.isNotBlank() && uiState.categoryOptions.isNotEmpty()) {
            PickerDropdown(
                label = "Equipment / category",
                selected = uiState.category,
                options = uiState.categoryOptions,
                onSelected = viewModel::onCategory,
            )
        }
        if (uiState.category.isNotBlank() && uiState.subCategory1Options.isNotEmpty()) {
            PickerDropdown(
                label = "Issue",
                selected = uiState.subCategory1,
                options = uiState.subCategory1Options,
                onSelected = viewModel::onSubCategory1,
            )
        }
        if (uiState.subCategory1.isNotBlank() && uiState.subCategory2Options.isNotEmpty()) {
            PickerDropdown(
                label = "Issue detail (optional)",
                selected = uiState.subCategory2,
                options = uiState.subCategory2Options,
                onSelected = viewModel::onSubCategory2,
            )
        }
        OutlinedTextField(
            value = uiState.description,
            onValueChange = viewModel::onDescription,
            label = { Text("Description (optional)") },
            modifier = Modifier.fillMaxWidth(),
        )
        uiState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(
            onClick = viewModel::save,
            enabled = uiState.isValid && !uiState.isSaving,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (uiState.isSaving) "Saving…" else "Raise ticket")
        }
    }
}

/**
 * Searchable outlet picker. The outlet list is large (130+ stores), so the field is editable while
 * open: typing filters the options case-insensitively on the display label. Collapsed it shows the
 * selected outlet; opening it clears the field for search.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StoreDropdown(
    selectedId: String,
    options: List<Pair<String, String>>,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = options.firstOrNull { it.first == selectedId }?.second ?: ""
    // Reset the search text whenever the menu opens/closes or the selection changes.
    var searchText by remember(selectedName, expanded) {
        mutableStateOf(if (expanded) "" else selectedName)
    }
    val filtered = if (!expanded || searchText.isBlank()) options
        else options.filter { it.second.contains(searchText, ignoreCase = true) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = if (expanded) searchText else selectedName,
            onValueChange = { searchText = it; expanded = true },
            singleLine = true,
            label = { Text("Outlet") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            filtered.forEach { (id, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onSelected(id)
                        expanded = false
                    },
                )
            }
        }
    }
}

/** Generic read-only picker for a list of string options (one cascade level). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickerDropdown(
    label: String,
    selected: String,
    options: List<String>,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}
