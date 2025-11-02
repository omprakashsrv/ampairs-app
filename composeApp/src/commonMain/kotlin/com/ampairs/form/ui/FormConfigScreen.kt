package com.ampairs.form.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ampairs.form.domain.EntityAttributeDefinition
import com.ampairs.form.domain.EntityFieldConfig
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormConfigScreen(
    entityType: String,
    onNavigateBack: () -> Unit,
    viewModel: FormConfigViewModel = koinViewModel { parametersOf(entityType) }
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage != null) {
            // Auto-dismiss success message after 2 seconds
            kotlinx.coroutines.delay(2000)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configure ${uiState.entityType} Form") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (!uiState.isSaving && (uiState.fieldConfigs.isNotEmpty() || uiState.attributeDefinitions.isNotEmpty())) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.saveChanges() },
                    icon = { Icon(Icons.Default.Save, contentDescription = null) },
                    text = { Text("Save Changes") }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.fieldConfigs.isEmpty() && uiState.attributeDefinitions.isEmpty() -> {
                    // Empty state - backend should seed configuration
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "No Configuration Found",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = "Please contact your administrator to seed the form configuration for ${uiState.entityType}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Field Configurations Section
                        if (uiState.fieldConfigs.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Field Configurations",
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }

                            items(uiState.fieldConfigs) { fieldConfig ->
                                FieldConfigCard(
                                    fieldConfig = fieldConfig,
                                    onUpdate = { viewModel.updateFieldConfig(it) }
                                )
                            }
                        }

                        // Attribute Definitions Section
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Custom Attributes",
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                                )
                                OutlinedButton(
                                    onClick = { viewModel.addNewAttribute() }
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Add Attribute")
                                }
                            }
                        }

                        if (uiState.attributeDefinitions.isEmpty()) {
                            item {
                                Text(
                                    text = "No custom attributes defined. Click 'Add Attribute' to create one.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }

                        items(uiState.attributeDefinitions) { attributeDefinition ->
                            AttributeDefinitionCard(
                                attributeDefinition = attributeDefinition,
                                onUpdate = { viewModel.updateAttributeDefinition(it) },
                                onDelete = { viewModel.deleteAttributeDefinition(it) }
                            )
                        }

                        // Bottom padding for FAB
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }

            // Error Snackbar
            if (uiState.error != null) {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearMessages() }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(uiState.error ?: "")
                }
            }

            // Success Snackbar
            if (uiState.successMessage != null) {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Text(uiState.successMessage ?: "")
                }
            }

            // Saving Overlay
            if (uiState.isSaving) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Card {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text("Saving changes...")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FieldConfigCard(
    fieldConfig: EntityFieldConfig,
    onUpdate: (EntityFieldConfig) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = fieldConfig.fieldName,
                style = MaterialTheme.typography.titleMedium
            )

            OutlinedTextField(
                value = fieldConfig.displayName,
                onValueChange = { onUpdate(fieldConfig.copy(displayName = it)) },
                label = { Text("Display Name") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = fieldConfig.placeholder ?: "",
                onValueChange = { onUpdate(fieldConfig.copy(placeholder = it.takeIf { it.isNotBlank() })) },
                label = { Text("Placeholder") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = fieldConfig.defaultValue ?: "",
                onValueChange = { onUpdate(fieldConfig.copy(defaultValue = it.takeIf { it.isNotBlank() })) },
                label = { Text("Default Value") },
                supportingText = { Text("Pre-filled value when creating new records") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = fieldConfig.helpText ?: "",
                onValueChange = { onUpdate(fieldConfig.copy(helpText = it.takeIf { it.isNotBlank() })) },
                label = { Text("Help Text") },
                supportingText = { Text("Additional information shown below the field") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Checkbox(
                        checked = fieldConfig.visible,
                        onCheckedChange = { onUpdate(fieldConfig.copy(visible = it)) }
                    )
                    Text("Visible", modifier = Modifier.padding(start = 8.dp))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Checkbox(
                        checked = fieldConfig.mandatory,
                        onCheckedChange = { onUpdate(fieldConfig.copy(mandatory = it)) }
                    )
                    Text("Mandatory", modifier = Modifier.padding(start = 8.dp))
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = fieldConfig.enabled,
                    onCheckedChange = { onUpdate(fieldConfig.copy(enabled = it)) }
                )
                Text("Enabled", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttributeDefinitionCard(
    attributeDefinition: EntityAttributeDefinition,
    onUpdate: (EntityAttributeDefinition) -> Unit,
    onDelete: (EntityAttributeDefinition) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with delete button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (attributeDefinition.attributeKey.isBlank()) "New Attribute" else attributeDefinition.attributeKey,
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = { onDelete(attributeDefinition) }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete Attribute",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Attribute Key (editable for new attributes)
            OutlinedTextField(
                value = attributeDefinition.attributeKey,
                onValueChange = { onUpdate(attributeDefinition.copy(attributeKey = it)) },
                label = { Text("Attribute Key") },
                placeholder = { Text("e.g., industry, companySize") },
                supportingText = { Text("Unique identifier - use camelCase") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Display Name
            OutlinedTextField(
                value = attributeDefinition.displayName,
                onValueChange = { onUpdate(attributeDefinition.copy(displayName = it)) },
                label = { Text("Display Name") },
                placeholder = { Text("e.g., Industry, Company Size") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Data Type Dropdown
            var dataTypeExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = dataTypeExpanded,
                onExpandedChange = { dataTypeExpanded = !dataTypeExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = attributeDefinition.dataType,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Data Type") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = dataTypeExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    singleLine = true
                )
                ExposedDropdownMenu(
                    expanded = dataTypeExpanded,
                    onDismissRequest = { dataTypeExpanded = false }
                ) {
                    listOf("STRING", "NUMBER", "BOOLEAN", "DATE", "ENUM").forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type) },
                            onClick = {
                                onUpdate(attributeDefinition.copy(dataType = type))
                                dataTypeExpanded = false
                            }
                        )
                    }
                }
            }

            // Category
            OutlinedTextField(
                value = attributeDefinition.category ?: "",
                onValueChange = { onUpdate(attributeDefinition.copy(category = it.takeIf { it.isNotBlank() })) },
                label = { Text("Category") },
                placeholder = { Text("e.g., Business, Financial") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Placeholder
            OutlinedTextField(
                value = attributeDefinition.placeholder ?: "",
                onValueChange = { onUpdate(attributeDefinition.copy(placeholder = it.takeIf { it.isNotBlank() })) },
                label = { Text("Placeholder") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Help Text
            OutlinedTextField(
                value = attributeDefinition.helpText ?: "",
                onValueChange = { onUpdate(attributeDefinition.copy(helpText = it.takeIf { it.isNotBlank() })) },
                label = { Text("Help Text") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Display Order
            OutlinedTextField(
                value = attributeDefinition.displayOrder.toString(),
                onValueChange = { value ->
                    val order = value.toIntOrNull() ?: 0
                    onUpdate(attributeDefinition.copy(displayOrder = order))
                },
                label = { Text("Display Order") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Checkboxes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Checkbox(
                        checked = attributeDefinition.visible,
                        onCheckedChange = { onUpdate(attributeDefinition.copy(visible = it)) }
                    )
                    Text("Visible", modifier = Modifier.padding(start = 8.dp))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Checkbox(
                        checked = attributeDefinition.mandatory,
                        onCheckedChange = { onUpdate(attributeDefinition.copy(mandatory = it)) }
                    )
                    Text("Mandatory", modifier = Modifier.padding(start = 8.dp))
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = attributeDefinition.enabled,
                    onCheckedChange = { onUpdate(attributeDefinition.copy(enabled = it)) }
                )
                Text("Enabled", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}
