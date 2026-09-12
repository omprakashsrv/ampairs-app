package com.ampairs.cbmaintenance.ui.schedule

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel

@Composable
fun PmScheduleFormScreen(
    scheduleId: String?,
    onDone: () -> Unit,
    viewModel: PmScheduleFormViewModel = assistedMetroViewModel<PmScheduleFormViewModel, PmScheduleFormViewModel.Factory>(
        key = scheduleId ?: "new",
    ) { create(scheduleId) },
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
            if (uiState.isEdit) "Edit PM schedule" else "New PM schedule",
            style = MaterialTheme.typography.headlineSmall,
        )
        // Full taxonomy cascade (Department › Equipment › Issue [› Issue-detail]) — each level
        // appears once its parent is chosen AND that level has options. The chosen leaf is stored
        // as the schedule's ticket_bucket_id. Falls back to free text if the catalog hasn't
        // synced/seeded yet.
        if (uiState.departmentOptions.isNotEmpty()) {
            PickerDropdown(
                label = "Department",
                selected = uiState.department,
                options = uiState.departmentOptions,
                onSelected = viewModel::onDepartment,
            )
            if (uiState.department.isNotBlank() && uiState.categoryOptions.isNotEmpty()) {
                PickerDropdown(
                    label = "Equipment / category",
                    selected = uiState.assetCategory,
                    options = uiState.categoryOptions,
                    onSelected = viewModel::onCategory,
                )
            }
            if (uiState.assetCategory.isNotBlank() && uiState.subCategory1Options.isNotEmpty()) {
                PickerDropdown(
                    label = "Issue",
                    selected = uiState.subCategory1,
                    options = uiState.subCategory1Options,
                    onSelected = viewModel::onSubCategory1,
                )
            }
            if (uiState.subCategory1.isNotBlank() && uiState.subCategory2Options.isNotEmpty()) {
                PickerDropdown(
                    label = "Issue detail",
                    selected = uiState.subCategory2,
                    options = uiState.subCategory2Options,
                    onSelected = viewModel::onSubCategory2,
                )
            }
        } else {
            OutlinedTextField(
                value = uiState.assetCategory,
                onValueChange = viewModel::onCategory,
                label = { Text("Asset category (e.g. WIC, DG Set)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        OutlinedTextField(
            value = uiState.taskName,
            onValueChange = viewModel::onTaskName,
            label = { Text("Task (auto — edit to override)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = uiState.frequencyInterval,
            onValueChange = viewModel::onFrequencyInterval,
            label = { Text("Every N") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        FrequencyUnitDropdown(selected = uiState.frequencyUnit, onSelected = viewModel::onFrequencyUnit)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FrequencyUnitDropdown(selected: String, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = "${selected.lowercase()}(s)",
            onValueChange = {},
            readOnly = true,
            label = { Text("Frequency unit") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            FrequencyUnits.ALL.forEach { unit ->
                DropdownMenuItem(
                    text = { Text("${unit.lowercase()}(s)") },
                    onClick = {
                        onSelected(unit)
                        expanded = false
                    },
                )
            }
        }
    }
}
