package com.ampairs.cbstore.ui.form

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
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel

@Composable
fun CbStoreFormScreen(
    storeId: String?,
    onDone: () -> Unit,
    viewModel: CbStoreFormViewModel = assistedMetroViewModel<CbStoreFormViewModel, CbStoreFormViewModel.Factory>(
        key = storeId ?: "new",
    ) { create(storeId) },
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
        Text(
            if (uiState.isEdit) "Edit outlet" else "New outlet",
            style = MaterialTheme.typography.headlineSmall,
        )
        OutlinedTextField(
            value = uiState.code,
            onValueChange = viewModel::onCode,
            label = { Text("Outlet code (e.g. ARK)") },
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
        CityAutocomplete(
            value = uiState.city,
            onValueChange = viewModel::onCity,
            options = uiState.cityOptions,
        )

        ZoneDropdown(
            selectedId = uiState.zonalOfficeId,
            options = uiState.zoneOptions.map { it.uid to it.name },
            onSelected = viewModel::onZone,
        )

        uiState.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        Button(
            onClick = viewModel::save,
            enabled = uiState.isValid && !uiState.isSaving,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (uiState.isSaving) "Saving…" else "Save")
        }
    }
}

/**
 * Free-text City field with autocomplete over existing distinct cities. The typed text is always the
 * value (a brand-new city can be entered); the dropdown just offers matching known cities to pick.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CityAutocomplete(
    value: String,
    onValueChange: (String) -> Unit,
    options: List<String>,
) {
    var expanded by remember { mutableStateOf(false) }
    val matches = if (value.isBlank()) options
        else options.filter { it.contains(value, ignoreCase = true) && !it.equals(value, ignoreCase = true) }
    ExposedDropdownMenuBox(
        expanded = expanded && matches.isNotEmpty(),
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it); expanded = true },
            label = { Text("City") },
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryEditable)
                .fillMaxWidth()
                .onFocusChanged { if (it.isFocused) expanded = true },
        )
        ExposedDropdownMenu(
            expanded = expanded && matches.isNotEmpty(),
            onDismissRequest = { expanded = false },
        ) {
            matches.forEach { city ->
                DropdownMenuItem(
                    text = { Text(city) },
                    onClick = {
                        onValueChange(city)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ZoneDropdown(
    selectedId: String,
    options: List<Pair<String, String>>,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = options.firstOrNull { it.first == selectedId }?.second ?: ""
    // ExposedDropdownMenuBox — a bare Modifier.clickable on a read-only OutlinedTextField never
    // fires (the field consumes the tap); menuAnchor() is what toggles the menu on tap.
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Zonal office") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (id, name) ->
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
