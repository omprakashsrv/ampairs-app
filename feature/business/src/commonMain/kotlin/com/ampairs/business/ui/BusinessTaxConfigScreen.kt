package com.ampairs.business.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject

/**
 * Business Tax Configuration Screen.
 *
 * Manages:
 * - Tax ID (GST/VAT)
 * - Registration number
 * - Tax settings and preferences
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessTaxConfigScreen(
    modifier: Modifier = Modifier,
    viewModel: BusinessTaxConfigViewModel = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()
    var isRefreshing by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current

    // Handle save success
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            snackbarHostState.showSnackbar("Tax configuration saved successfully")
            viewModel.clearSaveSuccess()
        }
    }

    // Handle refresh state
    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading) {
            isRefreshing = false
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        PullToRefreshBox(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            state = pullRefreshState,
            isRefreshing = isRefreshing,
            onRefresh = {
                if (!isRefreshing) {
                    isRefreshing = true
                    viewModel.refresh()
                }
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Tax Configuration",
                    style = MaterialTheme.typography.headlineMedium
                )

                when {
                    uiState.isLoading && uiState.taxConfig == null -> {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    uiState.error != null -> {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = uiState.error ?: "Unknown error",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }

                    else -> {
                        val formState = uiState.formState

                        // Basic Tax Information
                        SectionHeader("Tax Information")

                        OutlinedTextField(
                            value = formState.taxId,
                            onValueChange = { viewModel.updateFormState(formState.copy(taxId = it)) },
                            label = { Text("Tax ID (GST/VAT)") },
                            placeholder = { Text("29XXXXX1234X1Z5") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                        )

                        OutlinedTextField(
                            value = formState.registrationNumber,
                            onValueChange = { viewModel.updateFormState(formState.copy(registrationNumber = it)) },
                            label = { Text("Registration Number") },
                            placeholder = { Text("Enter registration number") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                        )

                        // Tax Settings Section
                        SectionHeader("Tax Settings")

                        TaxSettingsEditor(
                            settings = formState.taxSettings,
                            onSettingsChanged = { newSettings ->
                                viewModel.updateFormState(formState.copy(taxSettings = newSettings))
                            }
                        )

                        // Save Button
                        Button(
                            onClick = { viewModel.saveTaxConfig() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isSaving
                        ) {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text("Save Configuration")
                            }
                        }

                        // Add bottom spacing
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun TaxSettingsEditor(
    settings: Map<String, String>,
    onSettingsChanged: (Map<String, String>) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (settings.isEmpty()) {
            Text(
                text = "No tax settings configured. Click the button below to add settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            settings.forEach { (key, value) ->
                TaxSettingItem(
                    key = key,
                    value = value,
                    onDelete = {
                        val newSettings = settings.toMutableMap().apply { remove(key) }
                        onSettingsChanged(newSettings)
                    },
                    onValueChange = { newValue ->
                        val newSettings = settings.toMutableMap().apply { put(key, newValue) }
                        onSettingsChanged(newSettings)
                    }
                )
            }
        }

        OutlinedButton(
            onClick = { showAddDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Tax Setting")
        }
    }

    if (showAddDialog) {
        AddTaxSettingDialog(
            existingKeys = settings.keys,
            onDismiss = { showAddDialog = false },
            onAdd = { key, value ->
                val newSettings = settings.toMutableMap().apply { put(key, value) }
                onSettingsChanged(newSettings)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun TaxSettingItem(
    key: String,
    value: String,
    onDelete: () -> Unit,
    onValueChange: (String) -> Unit
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = key,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete setting",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun AddTaxSettingDialog(
    existingKeys: Set<String>,
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit
) {
    var key by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Tax Setting") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = key,
                    onValueChange = {
                        key = it
                        errorMessage = null
                    },
                    label = { Text("Setting Name") },
                    placeholder = { Text("e.g., tax_rate, reverse_charge") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = errorMessage != null
                )

                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("Setting Value") },
                    placeholder = { Text("Enter value") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when {
                        key.isBlank() -> errorMessage = "Setting name is required"
                        existingKeys.contains(key) -> errorMessage = "Setting name already exists"
                        value.isBlank() -> errorMessage = "Setting value is required"
                        else -> onAdd(key, value)
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
