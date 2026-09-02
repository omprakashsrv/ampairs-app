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
import androidx.compose.ui.focus.onFocusChanged
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
        OutlinedTextField(
            value = uiState.assetCategory,
            onValueChange = viewModel::onAssetCategory,
            label = { Text("Asset category (e.g. ChestFreezer)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = uiState.subCategory,
            onValueChange = viewModel::onSubCategory,
            label = { Text("Issue (e.g. Gasket broken)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StoreDropdown(
    selectedId: String,
    options: List<Pair<String, String>>,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = options.firstOrNull { it.first == selectedId }?.second ?: ""
    // Query starts as the current selection's name; typing filters the list and re-shows the
    // menu. Selecting an option (or losing the typed text) snaps back to the selected name.
    var query by remember(selectedName) { mutableStateOf(selectedName) }
    val filtered = remember(query, options, selectedName) {
        if (query.isBlank() || query == selectedName) {
            options
        } else {
            options.filter { it.second.contains(query, ignoreCase = true) }
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
            label = { Text("Outlet") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            singleLine = true,
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryEditable)
                .fillMaxWidth()
                .onFocusChanged { if (it.isFocused) expanded = true },
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (filtered.isEmpty()) {
                DropdownMenuItem(text = { Text("No matching outlet") }, onClick = {}, enabled = false)
            }
            filtered.forEach { (id, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onSelected(id)
                        query = name
                        expanded = false
                    },
                )
            }
        }
    }
}
