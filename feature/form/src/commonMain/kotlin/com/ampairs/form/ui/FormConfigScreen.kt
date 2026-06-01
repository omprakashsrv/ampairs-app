package com.ampairs.form.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass
import com.ampairs.form.domain.EntityAttributeDefinition
import com.ampairs.form.domain.EntityFieldConfig
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import org.jetbrains.compose.resources.stringResource
import ampairsapp.feature.form.generated.resources.Res
import ampairsapp.feature.form.generated.resources.form_add_attribute
import ampairsapp.feature.form.generated.resources.form_back_cd
import ampairsapp.feature.form.generated.resources.form_delete_attribute_cd
import ampairsapp.feature.form.generated.resources.form_dismiss
import ampairsapp.feature.form.generated.resources.form_hint_attribute_key
import ampairsapp.feature.form.generated.resources.form_hint_attribute_key_desc
import ampairsapp.feature.form.generated.resources.form_hint_category
import ampairsapp.feature.form.generated.resources.form_hint_default_value
import ampairsapp.feature.form.generated.resources.form_hint_display_name
import ampairsapp.feature.form.generated.resources.form_hint_help_text
import ampairsapp.feature.form.generated.resources.form_label_attribute_key
import ampairsapp.feature.form.generated.resources.form_label_category
import ampairsapp.feature.form.generated.resources.form_label_data_type
import ampairsapp.feature.form.generated.resources.form_label_default_value
import ampairsapp.feature.form.generated.resources.form_label_display_name
import ampairsapp.feature.form.generated.resources.form_label_display_order
import ampairsapp.feature.form.generated.resources.form_label_help_text
import ampairsapp.feature.form.generated.resources.form_label_placeholder
import ampairsapp.feature.form.generated.resources.form_new_attribute
import ampairsapp.feature.form.generated.resources.form_no_attributes
import ampairsapp.feature.form.generated.resources.form_no_config_desc
import ampairsapp.feature.form.generated.resources.form_no_config_title
import ampairsapp.feature.form.generated.resources.form_save_changes
import ampairsapp.feature.form.generated.resources.form_saving
import ampairsapp.feature.form.generated.resources.form_section_attributes
import ampairsapp.feature.form.generated.resources.form_section_fields
import ampairsapp.feature.form.generated.resources.form_title_configure
import ampairsapp.feature.form.generated.resources.form_toggle_enabled
import ampairsapp.feature.form.generated.resources.form_toggle_mandatory
import ampairsapp.feature.form.generated.resources.form_toggle_visible

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormConfigScreen(
    entityType: String,
    onNavigateBack: () -> Unit,
    viewModel: FormConfigViewModel = assistedMetroViewModel<FormConfigViewModel, FormConfigViewModel.Factory>(key = entityType) { create(entityType) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val isExpanded = windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)

    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage != null) {
            kotlinx.coroutines.delay(2000)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.form_title_configure, uiState.entityType)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.form_back_cd)
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
                    text = { Text(stringResource(Res.string.form_save_changes)) }
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
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.fieldConfigs.isEmpty() && uiState.attributeDefinitions.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.form_no_config_title),
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = stringResource(Res.string.form_no_config_desc, uiState.entityType),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                isExpanded -> {
                    FormConfigExpandedLayout(
                        fieldConfigs = uiState.fieldConfigs,
                        attributeDefinitions = uiState.attributeDefinitions,
                        onUpdateFieldConfig = { viewModel.updateFieldConfig(it) },
                        onUpdateAttributeDefinition = { viewModel.updateAttributeDefinition(it) },
                        onDeleteAttributeDefinition = { viewModel.deleteAttributeDefinition(it) },
                        onAddAttribute = { viewModel.addNewAttribute() }
                    )
                }
                else -> {
                    FormConfigScrollableContent(
                        fieldConfigs = uiState.fieldConfigs,
                        attributeDefinitions = uiState.attributeDefinitions,
                        onUpdateFieldConfig = { viewModel.updateFieldConfig(it) },
                        onUpdateAttributeDefinition = { viewModel.updateAttributeDefinition(it) },
                        onDeleteAttributeDefinition = { viewModel.deleteAttributeDefinition(it) },
                        onAddAttribute = { viewModel.addNewAttribute() }
                    )
                }
            }

            if (uiState.error != null) {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearMessages() }) {
                            Text(stringResource(Res.string.form_dismiss))
                        }
                    }
                ) {
                    Text(uiState.error ?: "")
                }
            }

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

            if (uiState.isSaving) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(shape = MaterialTheme.shapes.medium, tonalElevation = 4.dp) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(stringResource(Res.string.form_saving))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FormConfigExpandedLayout(
    fieldConfigs: List<EntityFieldConfig>,
    attributeDefinitions: List<EntityAttributeDefinition>,
    onUpdateFieldConfig: (EntityFieldConfig) -> Unit,
    onUpdateAttributeDefinition: (EntityAttributeDefinition) -> Unit,
    onDeleteAttributeDefinition: (EntityAttributeDefinition) -> Unit,
    onAddAttribute: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Left panel — Field Configurations
        if (fieldConfigs.isNotEmpty()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.form_section_fields),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(fieldConfigs) { fieldConfig ->
                        FieldConfigCard(fieldConfig = fieldConfig, onUpdate = onUpdateFieldConfig)
                    }
                }
            }
        }

        // Right panel — Custom Attributes
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.form_section_attributes),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedButton(onClick = onAddAttribute) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(Res.string.form_add_attribute))
                }
            }
            if (attributeDefinitions.isEmpty()) {
                Text(
                    text = stringResource(Res.string.form_no_attributes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(attributeDefinitions) { attrDef ->
                        AttributeDefinitionCard(
                            attributeDefinition = attrDef,
                            onUpdate = onUpdateAttributeDefinition,
                            onDelete = onDeleteAttributeDefinition
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FormConfigScrollableContent(
    fieldConfigs: List<EntityFieldConfig>,
    attributeDefinitions: List<EntityAttributeDefinition>,
    onUpdateFieldConfig: (EntityFieldConfig) -> Unit,
    onUpdateAttributeDefinition: (EntityAttributeDefinition) -> Unit,
    onDeleteAttributeDefinition: (EntityAttributeDefinition) -> Unit,
    onAddAttribute: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (fieldConfigs.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(Res.string.form_section_fields),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            items(fieldConfigs) { fieldConfig ->
                FieldConfigCard(fieldConfig = fieldConfig, onUpdate = onUpdateFieldConfig)
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.form_section_attributes),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
                OutlinedButton(onClick = onAddAttribute) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(Res.string.form_add_attribute))
                }
            }
        }

        if (attributeDefinitions.isEmpty()) {
            item {
                Text(
                    text = stringResource(Res.string.form_no_attributes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        items(attributeDefinitions) { attributeDefinition ->
            AttributeDefinitionCard(
                attributeDefinition = attributeDefinition,
                onUpdate = onUpdateAttributeDefinition,
                onDelete = onDeleteAttributeDefinition
            )
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun FieldConfigCard(
    fieldConfig: EntityFieldConfig,
    onUpdate: (EntityFieldConfig) -> Unit
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = fieldConfig.fieldName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            OutlinedTextField(
                value = fieldConfig.displayName,
                onValueChange = { onUpdate(fieldConfig.copy(displayName = it)) },
                label = { Text(stringResource(Res.string.form_label_display_name)) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )

            OutlinedTextField(
                value = fieldConfig.placeholder ?: "",
                onValueChange = { onUpdate(fieldConfig.copy(placeholder = it.takeIf { it.isNotBlank() })) },
                label = { Text(stringResource(Res.string.form_label_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )

            OutlinedTextField(
                value = fieldConfig.defaultValue ?: "",
                onValueChange = { onUpdate(fieldConfig.copy(defaultValue = it.takeIf { it.isNotBlank() })) },
                label = { Text(stringResource(Res.string.form_label_default_value)) },
                supportingText = { Text(stringResource(Res.string.form_hint_default_value)) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )

            OutlinedTextField(
                value = fieldConfig.helpText ?: "",
                onValueChange = { onUpdate(fieldConfig.copy(helpText = it.takeIf { it.isNotBlank() })) },
                label = { Text(stringResource(Res.string.form_label_help_text)) },
                supportingText = { Text(stringResource(Res.string.form_hint_help_text)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                shape = MaterialTheme.shapes.medium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Checkbox(checked = fieldConfig.visible, onCheckedChange = { onUpdate(fieldConfig.copy(visible = it)) })
                    Text(stringResource(Res.string.form_toggle_visible), modifier = Modifier.padding(start = 8.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Checkbox(checked = fieldConfig.mandatory, onCheckedChange = { onUpdate(fieldConfig.copy(mandatory = it)) })
                    Text(stringResource(Res.string.form_toggle_mandatory), modifier = Modifier.padding(start = 8.dp))
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Checkbox(checked = fieldConfig.enabled, onCheckedChange = { onUpdate(fieldConfig.copy(enabled = it)) })
                Text(stringResource(Res.string.form_toggle_enabled), modifier = Modifier.padding(start = 8.dp))
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
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (attributeDefinition.attributeKey.isBlank())
                        stringResource(Res.string.form_new_attribute)
                    else attributeDefinition.attributeKey,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = { onDelete(attributeDefinition) }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(Res.string.form_delete_attribute_cd),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            OutlinedTextField(
                value = attributeDefinition.attributeKey,
                onValueChange = { onUpdate(attributeDefinition.copy(attributeKey = it)) },
                label = { Text(stringResource(Res.string.form_label_attribute_key)) },
                placeholder = { Text(stringResource(Res.string.form_hint_attribute_key)) },
                supportingText = { Text(stringResource(Res.string.form_hint_attribute_key_desc)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            OutlinedTextField(
                value = attributeDefinition.displayName,
                onValueChange = { onUpdate(attributeDefinition.copy(displayName = it)) },
                label = { Text(stringResource(Res.string.form_label_display_name)) },
                placeholder = { Text(stringResource(Res.string.form_hint_display_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            var dataTypeExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = dataTypeExpanded,
                onExpandedChange = { dataTypeExpanded = !dataTypeExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = attributeDefinition.dataType,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(Res.string.form_label_data_type)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dataTypeExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
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

            OutlinedTextField(
                value = attributeDefinition.category ?: "",
                onValueChange = { onUpdate(attributeDefinition.copy(category = it.takeIf { it.isNotBlank() })) },
                label = { Text(stringResource(Res.string.form_label_category)) },
                placeholder = { Text(stringResource(Res.string.form_hint_category)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            OutlinedTextField(
                value = attributeDefinition.placeholder ?: "",
                onValueChange = { onUpdate(attributeDefinition.copy(placeholder = it.takeIf { it.isNotBlank() })) },
                label = { Text(stringResource(Res.string.form_label_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            OutlinedTextField(
                value = attributeDefinition.helpText ?: "",
                onValueChange = { onUpdate(attributeDefinition.copy(helpText = it.takeIf { it.isNotBlank() })) },
                label = { Text(stringResource(Res.string.form_label_help_text)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            OutlinedTextField(
                value = attributeDefinition.displayOrder.toString(),
                onValueChange = { value ->
                    onUpdate(attributeDefinition.copy(displayOrder = value.toIntOrNull() ?: 0))
                },
                label = { Text(stringResource(Res.string.form_label_display_order)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Checkbox(checked = attributeDefinition.visible, onCheckedChange = { onUpdate(attributeDefinition.copy(visible = it)) })
                    Text(stringResource(Res.string.form_toggle_visible), modifier = Modifier.padding(start = 8.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Checkbox(checked = attributeDefinition.mandatory, onCheckedChange = { onUpdate(attributeDefinition.copy(mandatory = it)) })
                    Text(stringResource(Res.string.form_toggle_mandatory), modifier = Modifier.padding(start = 8.dp))
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Checkbox(checked = attributeDefinition.enabled, onCheckedChange = { onUpdate(attributeDefinition.copy(enabled = it)) })
                Text(stringResource(Res.string.form_toggle_enabled), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}
