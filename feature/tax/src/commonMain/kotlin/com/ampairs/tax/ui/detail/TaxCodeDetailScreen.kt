package com.ampairs.tax.ui.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.ampairs.tax.domain.model.TaxCode
import com.ampairs.tax.calculation.model.TaxCalculationResult
import com.ampairs.tax.util.formatDecimal
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel

/**
 * Tax Code Detail Screen
 *
 * Displays full information about a workspace tax code including:
 * - Code details and description
 * - Sync status
 * - Usage statistics
 * - Custom notes
 * - Actions (favorite, unsubscribe)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaxCodeDetailScreen(
    taxCodeId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TaxCodeDetailViewModel = assistedMetroViewModel<TaxCodeDetailViewModel, TaxCodeDetailViewModel.Factory> { create(taxCodeId) }
) {
    val uiState by viewModel.uiState.collectAsState()
    var showUnsubscribeDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tax Code Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        when (val state = uiState) {
            is TaxCodeDetailUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is TaxCodeDetailUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = onNavigateBack) {
                            Text("Go Back")
                        }
                    }
                }
            }

            is TaxCodeDetailUiState.Success -> {
                TaxCodeDetailContent(
                    taxCode = state.taxCode,
                    taxRules = state.taxRules,
                    isLoadingRules = state.isLoadingRules,
                    isEditing = state.isEditing,
                    isSaving = state.isSaving,
                    saveError = state.saveError,
                    calculationResult = state.calculationResult,
                    isCalculating = state.isCalculating,
                    onToggleFavorite = { viewModel.toggleFavorite() },
                    onEditNotes = { viewModel.setEditing(true) },
                    onSaveNotes = { notes -> viewModel.saveNotes(notes) },
                    onCancelEdit = { viewModel.setEditing(false) },
                    onUnsubscribe = { showUnsubscribeDialog = true },
                    onClearError = { viewModel.clearError() },
                    onCalculatePreview = { amount, quantity, sourceState, destState ->
                        viewModel.calculateTaxPreview(amount, quantity, sourceState, destState)
                    },
                    modifier = Modifier.padding(paddingValues)
                )

                // Unsubscribe Confirmation Dialog
                if (showUnsubscribeDialog) {
                    UnsubscribeConfirmationDialog(
                        taxCode = state.taxCode,
                        onConfirm = {
                            viewModel.unsubscribe(onSuccess = onNavigateBack)
                            showUnsubscribeDialog = false
                        },
                        onDismiss = { showUnsubscribeDialog = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun TaxCodeDetailContent(
    taxCode: TaxCode,
    taxRules: List<com.ampairs.tax.domain.model.TaxRule>,
    isLoadingRules: Boolean,
    isEditing: Boolean,
    isSaving: Boolean,
    saveError: String?,
    calculationResult: TaxCalculationResult?,
    isCalculating: Boolean,
    onToggleFavorite: () -> Unit,
    onEditNotes: () -> Unit,
    onSaveNotes: (String) -> Unit,
    onCancelEdit: () -> Unit,
    onUnsubscribe: () -> Unit,
    onClearError: () -> Unit,
    onCalculatePreview: (Double, Int, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var notesText by remember(taxCode.notes) { mutableStateOf(taxCode.notes ?: "") }
    var previewAmount by remember { mutableStateOf("1000.00") }
    var previewQuantity by remember { mutableStateOf("1") }
    var previewSourceState by remember { mutableStateOf("MH") }
    var previewDestState by remember { mutableStateOf("MH") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Card with Code and Actions
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = taxCode.code,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(onClick = onToggleFavorite) {
                            Icon(
                                imageVector = if (taxCode.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Toggle Favorite",
                                tint = if (taxCode.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        IconButton(onClick = onUnsubscribe) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Unsubscribe",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Text(
                    text = taxCode.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                if (taxCode.shortDescription.isNotBlank() && taxCode.shortDescription != taxCode.description) {
                    Text(
                        text = taxCode.shortDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Details Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Details",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                DetailRow(label = "Code Type", value = taxCode.codeType.name)
                DetailRow(label = "Master Code ID", value = taxCode.masterTaxCodeId)

                if (taxCode.customTaxRuleId != null) {
                    DetailRow(label = "Custom Tax Rule", value = taxCode.customTaxRuleId!!)
                }

                DetailRow(
                    label = "Status",
                    value = if (taxCode.isActive) "Active" else "Inactive"
                )

                // Sync Status with Icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sync Status",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val (icon, color) = when (taxCode.syncStatus) {
                            "SYNCED" -> Icons.Default.CloudDone to MaterialTheme.colorScheme.primary
                            "PENDING" -> Icons.Default.CloudQueue to MaterialTheme.colorScheme.tertiary
                            "DELETE_PENDING" -> Icons.Default.CloudOff to MaterialTheme.colorScheme.error
                            else -> Icons.Default.CloudQueue to MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = taxCode.syncStatus,
                            style = MaterialTheme.typography.bodyMedium,
                            color = color
                        )
                    }
                }
            }
        }

        // Tax Calculation Preview Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Tax Calculation Preview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )

                Text(
                    text = "Test this tax code with sample values",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = previewAmount,
                        onValueChange = { previewAmount = it },
                        label = { Text("Amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = previewQuantity,
                        onValueChange = { previewQuantity = it },
                        label = { Text("Qty") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(0.5f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = previewSourceState,
                        onValueChange = { previewSourceState = it },
                        label = { Text("From State") },
                        placeholder = { Text("MH") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = previewDestState,
                        onValueChange = { previewDestState = it },
                        label = { Text("To State") },
                        placeholder = { Text("MH") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Button(
                    onClick = {
                        val amount = previewAmount.toDoubleOrNull() ?: 1000.0
                        val qty = previewQuantity.toIntOrNull() ?: 1
                        onCalculatePreview(amount, qty, previewSourceState, previewDestState)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isCalculating
                ) {
                    Icon(Icons.Default.Calculate, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isCalculating) "Calculating..." else "Calculate Preview")
                }

                // Show calculation result
                if (calculationResult != null) {
                    HorizontalDivider()

                    // Base amount
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Base Amount:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "₹${calculationResult.baseAmount.formatDecimal()}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    // Tax components
                    calculationResult.taxComponents.forEach { component ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${component.componentName} (${component.ratePercentage}%):",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "₹${component.taxAmount.formatDecimal()}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    HorizontalDivider()

                    // Total tax
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Total Tax:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "₹${calculationResult.totalTaxAmount.formatDecimal()}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Grand total
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Grand Total:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "₹${calculationResult.totalAmount.formatDecimal()}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Usage Statistics Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Usage Statistics",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                DetailRow(label = "Usage Count", value = "${taxCode.usageCount}")

                if (taxCode.lastUsedAt != null) {
                    DetailRow(
                        label = "Last Used",
                        value = formatTimestamp(taxCode.lastUsedAt!!)
                    )
                }

                DetailRow(
                    label = "Added",
                    value = formatTimestamp(taxCode.addedAt)
                )

                DetailRow(
                    label = "Last Updated",
                    value = formatTimestamp(taxCode.updatedAt)
                )
            }
        }

        // Tax Rules Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Tax Rules",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                if (isLoadingRules) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (taxRules.isEmpty()) {
                    Text(
                        text = "No tax rules configured for this code",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    taxRules.forEach { rule ->
                        TaxRuleItem(rule = rule)
                        if (rule != taxRules.last()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }
                }
            }
        }

        // Notes Card
        Card(modifier = Modifier.fillMaxWidth()) {
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
                        text = "Notes",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (!isEditing) {
                        IconButton(onClick = onEditNotes) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Notes"
                            )
                        }
                    }
                }

                if (isEditing) {
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Add notes about this tax code...") },
                        minLines = 3,
                        maxLines = 6
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        TextButton(onClick = onCancelEdit, enabled = !isSaving) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = { onSaveNotes(notesText) },
                            enabled = !isSaving
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save")
                        }
                    }
                } else {
                    if (taxCode.notes.isNullOrBlank()) {
                        Text(
                            text = "No notes added yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = taxCode.notes!!,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Save Error
                if (saveError != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = saveError,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onClearError) {
                                Text("Dismiss")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun UnsubscribeConfirmationDialog(
    taxCode: TaxCode,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text("Unsubscribe from Tax Code?")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Are you sure you want to unsubscribe from this tax code?")
                Text(
                    text = "${taxCode.code} - ${taxCode.description}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                if (taxCode.usageCount > 0) {
                    Text(
                        text = "This code has been used ${taxCode.usageCount} times.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Unsubscribe")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun TaxRuleItem(
    rule: com.ampairs.tax.domain.model.TaxRule,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Jurisdiction and Type
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Jurisdiction: ${rule.jurisdiction}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = rule.jurisdictionLevel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Tax Code Info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Tax Code:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${rule.taxCode} (${rule.taxCodeType})",
                style = MaterialTheme.typography.bodySmall
            )
        }

        if (rule.taxCodeDescription != null) {
            Text(
                text = rule.taxCodeDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Tax Components
        if (rule.componentComposition.isNotEmpty()) {
            Text(
                text = "Tax Components:",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 8.dp)
            )

            // Display all scenarios in the composition map
            rule.componentComposition.forEach { (scenarioKey, composition) ->
                val scenarioLabel = when (scenarioKey) {
                    "intra_state" -> "Intra-State"
                    "inter_state" -> "Inter-State"
                    "standard" -> "Standard"
                    "b2b" -> "B2B"
                    "b2c" -> "B2C"
                    else -> scenarioKey.replace("_", " ").replaceFirstChar { it.uppercase() }
                }

                Text(
                    text = "$scenarioLabel (Total: ${composition.totalRate}%)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp)
                )

                composition.components.forEach { component ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = component.name,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "${component.rate}%",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Status badge
        if (rule.isActive) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(
                    text = "Active",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * Format timestamp for display
 * Simple formatting - converts epoch milliseconds to readable string
 */
private fun formatTimestamp(timestamp: Long): String {
    // Convert to seconds for readability
    val seconds = timestamp / 1000
    val days = seconds / 86400
    val hours = (seconds % 86400) / 3600
    val minutes = (seconds % 3600) / 60

    // Simple relative time format
    return when {
        days > 365 -> "${days / 365} year(s) ago"
        days > 30 -> "${days / 30} month(s) ago"
        days > 0 -> "$days day(s) ago"
        hours > 0 -> "$hours hour(s) ago"
        minutes > 0 -> "$minutes minute(s) ago"
        else -> "Just now"
    }
}
