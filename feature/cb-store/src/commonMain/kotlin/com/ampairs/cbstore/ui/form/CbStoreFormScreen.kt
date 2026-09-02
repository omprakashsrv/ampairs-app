package com.ampairs.cbstore.ui.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
        OutlinedTextField(
            value = uiState.city,
            onValueChange = viewModel::onCity,
            label = { Text("City") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
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

@Composable
private fun ZoneDropdown(
    selectedId: String,
    options: List<Pair<String, String>>,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = options.firstOrNull { it.first == selectedId }?.second ?: ""
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Zonal office") },
            trailingIcon = {
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = "Pick zone",
                    modifier = Modifier.clickableNoIndication { expanded = true },
                )
            },
            modifier = Modifier.fillMaxWidth().clickableNoIndication { expanded = true },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
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

private fun Modifier.clickableNoIndication(onClick: () -> Unit): Modifier =
    this.then(androidx.compose.foundation.clickable(
        interactionSource = androidx.compose.foundation.interaction.MutableInteractionSource(),
        indication = null,
        onClick = onClick,
    ))
