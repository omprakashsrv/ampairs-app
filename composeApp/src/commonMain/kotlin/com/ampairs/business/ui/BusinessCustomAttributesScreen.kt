package com.ampairs.business.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ampairs.form.domain.AttributeDataType
import com.ampairs.form.domain.EntityAttributeDefinition
import org.koin.compose.koinInject

/**
 * Business Custom Attributes Screen.
 * Displays and allows editing of custom attributes defined in business form configuration.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessCustomAttributesScreen(
    modifier: Modifier = Modifier,
    viewModel: BusinessCustomAttributesViewModel = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()
    var isRefreshing by remember { mutableStateOf(false) }

    // Show success snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            snackbarHostState.showSnackbar("Custom attributes saved successfully")
            viewModel.clearSaveSuccess()
        }
    }

    // Handle pull to refresh
    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading) {
            isRefreshing = false
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (uiState.hasCustomAttributes && !uiState.isLoading) {
                FloatingActionButton(
                    onClick = { viewModel.saveCustomAttributes() },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Save Custom Attributes")
                }
            }
        }
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
            when {
                uiState.isLoading && uiState.customAttributes.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
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
                }

                !uiState.hasCustomAttributes -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "No Custom Attributes",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "Custom attributes can be configured in Form Configuration to capture additional business information.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Custom Attributes",
                                style = MaterialTheme.typography.headlineMedium
                            )
                            if (uiState.businessName.isNotBlank()) {
                                Text(
                                    text = uiState.businessName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Group by category
                        val groupedAttributes = uiState.customAttributes.groupBy { it.category ?: "General" }

                        groupedAttributes.forEach { (category, attributes) ->
                            // Category Header
                            Text(
                                text = category,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp)
                            )

                            // Render attributes in category
                            attributes.sortedBy { it.displayOrder }.forEach { attribute ->
                                CustomAttributeField(
                                    attribute = attribute,
                                    value = uiState.customAttributeValues[attribute.attributeKey],
                                    onValueChange = { newValue ->
                                        viewModel.updateAttributeValue(attribute.attributeKey, newValue)
                                    },
                                    enabled = !uiState.isSaving
                                )
                            }
                        }

                        // Bottom spacing for FAB
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

/**
 * Composable to render a single custom attribute field based on its data type.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomAttributeField(
    attribute: EntityAttributeDefinition,
    value: Any?,
    onValueChange: (Any?) -> Unit,
    enabled: Boolean = true
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        when (attribute.dataType) {
            AttributeDataType.STRING -> {
                OutlinedTextField(
                    value = (value as? String) ?: "",
                    onValueChange = onValueChange,
                    label = {
                        Text(
                            text = buildString {
                                append(attribute.displayName)
                                if (attribute.mandatory) append(" *")
                            }
                        )
                    },
                    placeholder = attribute.placeholder?.let { { Text(it) } },
                    supportingText = attribute.helpText?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled,
                    isError = attribute.mandatory && (value as? String).isNullOrBlank()
                )
            }

            AttributeDataType.NUMBER -> {
                OutlinedTextField(
                    value = value?.toString() ?: "",
                    onValueChange = { newValue ->
                        onValueChange(newValue.toDoubleOrNull())
                    },
                    label = {
                        Text(
                            text = buildString {
                                append(attribute.displayName)
                                if (attribute.mandatory) append(" *")
                            }
                        )
                    },
                    placeholder = attribute.placeholder?.let { { Text(it) } },
                    supportingText = attribute.helpText?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled,
                    isError = attribute.mandatory && value == null
                )
            }

            AttributeDataType.BOOLEAN -> {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = (value as? Boolean) ?: false,
                        onCheckedChange = onValueChange,
                        enabled = enabled
                    )
                    Column {
                        Text(
                            text = buildString {
                                append(attribute.displayName)
                                if (attribute.mandatory) append(" *")
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                        attribute.helpText?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            AttributeDataType.ENUM -> {
                var expanded by remember { mutableStateOf(false) }
                val options = attribute.enumValues ?: emptyList()

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded && enabled }
                ) {
                    OutlinedTextField(
                        value = (value as? String) ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = {
                            Text(
                                text = buildString {
                                    append(attribute.displayName)
                                    if (attribute.mandatory) append(" *")
                                }
                            )
                        },
                        supportingText = attribute.helpText?.let { { Text(it) } },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled),
                        enabled = enabled,
                        isError = attribute.mandatory && (value as? String).isNullOrBlank()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        options.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    onValueChange(option)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            AttributeDataType.DATE -> {
                OutlinedTextField(
                    value = (value as? String) ?: "",
                    onValueChange = onValueChange,
                    label = {
                        Text(
                            text = buildString {
                                append(attribute.displayName)
                                if (attribute.mandatory) append(" *")
                            }
                        )
                    },
                    placeholder = { Text("YYYY-MM-DD") },
                    supportingText = attribute.helpText?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled,
                    isError = attribute.mandatory && (value as? String).isNullOrBlank()
                )
            }

            AttributeDataType.JSON -> {
                OutlinedTextField(
                    value = (value as? String) ?: "",
                    onValueChange = onValueChange,
                    label = {
                        Text(
                            text = buildString {
                                append(attribute.displayName)
                                if (attribute.mandatory) append(" *")
                            }
                        )
                    },
                    placeholder = attribute.placeholder?.let { { Text(it) } },
                    supportingText = attribute.helpText?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6,
                    enabled = enabled,
                    isError = attribute.mandatory && (value as? String).isNullOrBlank()
                )
            }

            else -> {
                // Unknown type - fallback to text field
                OutlinedTextField(
                    value = value?.toString() ?: "",
                    onValueChange = onValueChange,
                    label = { Text(attribute.displayName) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled
                )
            }
        }
    }
}
