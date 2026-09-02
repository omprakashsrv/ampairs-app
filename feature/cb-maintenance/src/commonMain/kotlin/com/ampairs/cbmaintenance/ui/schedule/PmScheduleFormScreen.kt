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
        OutlinedTextField(
            value = uiState.assetCategory,
            onValueChange = viewModel::onAssetCategory,
            label = { Text("Asset category (e.g. WIC, DG Set)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = uiState.taskName,
            onValueChange = viewModel::onTaskName,
            label = { Text("Task") },
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
