package com.ampairs.cbemployee.ui.form

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import com.ampairs.cbemployee.domain.model.MaintenanceRoles
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
        OutlinedTextField(
            value = uiState.mobile,
            onValueChange = viewModel::onMobile,
            label = { Text("Mobile (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = uiState.zonalOfficeId,
            onValueChange = viewModel::onZone,
            label = { Text("Zonal office ID (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
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

@Composable
private fun RoleDropdown(selected: String, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val source = remember { MutableInteractionSource() }
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selected.replace('_', ' '),
            onValueChange = {},
            readOnly = true,
            label = { Text("Role") },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = "Pick role") },
            modifier = Modifier.fillMaxWidth().clickable(
                interactionSource = source,
                indication = null,
            ) { expanded = true },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
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
