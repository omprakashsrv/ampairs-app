package com.ampairs.tax.ui.hsn

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ampairs.tax.domain.HsnCategory
import com.ampairs.tax.domain.HsnCode
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.time.Clock

@OptIn(ExperimentalMaterial3Api::class, kotlin.time.ExperimentalTime::class)
@Composable
fun HsnCodeDetailsScreen(
    hsnCodeId: String,
    onNavigateBack: () -> Unit,
    onEditHsnCode: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HsnCodeDetailsViewModel = koinViewModel { parametersOf(hsnCodeId) }
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(hsnCodeId) {
        viewModel.refreshHsnCode()
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("HSN Code Details") },
            actions = {
                uiState.hsnCode?.let { hsnCode ->
                    IconButton(onClick = { onEditHsnCode(hsnCode.id) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        enabled = !uiState.isDeleting
                    ) {
                        if (uiState.isDeleting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
            }
        )

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.showEmptyState -> {
                EmptyHsnCodeDetailsState(
                    onNavigateBack = onNavigateBack,
                    modifier = Modifier.fillMaxSize()
                )
            }

            uiState.showContent -> {
                HsnCodeDetailsContent(
                    hsnCode = uiState.hsnCode!!,
                    error = uiState.error,
                    onClearError = viewModel::clearError,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete HSN Code") },
            text = { Text("Are you sure you want to delete this HSN code? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteHsnCode {
                            onNavigateBack()
                        }
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun HsnCodeDetailsContent(
    hsnCode: HsnCode,
    error: String?,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Error Display
        error?.let { errorMessage ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onClearError) {
                        Text("Dismiss")
                    }
                }
            }
        }

        // Main HSN Code Information
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "HSN Code Information",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                // Formatted HSN Code Display
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "HSN Code",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = hsnCode.formattedCode,
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (hsnCode.isValidHsnCode) {
                            AssistChip(
                                onClick = { },
                                label = { Text("Valid Format") },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }
                }

                // Description
                DetailRow(
                    label = "Description",
                    value = hsnCode.description,
                    isMultiline = true
                )

                // Chapter
                DetailRow(
                    label = "Chapter",
                    value = hsnCode.chapter
                )

                // Heading
                DetailRow(
                    label = "Heading",
                    value = hsnCode.heading
                )

                // Category
                DetailRow(
                    label = "Category",
                    value = getCategoryDisplayName(hsnCode.category),
                    isChip = true
                )

                // Parent HSN
                hsnCode.parentHsnId?.let { parentId ->
                    DetailRow(
                        label = "Parent HSN ID",
                        value = parentId
                    )
                }

                // Status
                DetailRow(
                    label = "Status",
                    value = if (hsnCode.isActive) "Active" else "Inactive",
                    isStatus = true,
                    statusActive = hsnCode.isActive
                )
            }
        }

        // HSN Code Validation Info
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "HSN Code Format",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                DetailRow(
                    label = "Code Length",
                    value = "${hsnCode.hsnCode.length} digits"
                )

                DetailRow(
                    label = "Format Type",
                    value = when (hsnCode.hsnCode.length) {
                        4 -> "4-digit Chapter Level"
                        6 -> "6-digit Heading Level"
                        8 -> "8-digit Sub-heading Level"
                        else -> "Non-standard format"
                    }
                )

                DetailRow(
                    label = "Validation",
                    value = if (hsnCode.isValidHsnCode) "Valid HSN Format" else "Invalid Format",
                    isStatus = true,
                    statusActive = hsnCode.isValidHsnCode
                )
            }
        }

        // Metadata
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Metadata",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                DetailRow(
                    label = "Created",
                    value = formatDetailDate(hsnCode.createdAt)
                )

                DetailRow(
                    label = "Last Updated",
                    value = formatDetailDate(hsnCode.updatedAt)
                )
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    isMultiline: Boolean = false,
    isChip: Boolean = false,
    isStatus: Boolean = false,
    statusActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        when {
            isStatus -> {
                AssistChip(
                    onClick = { },
                    label = { Text(value) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (statusActive) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.errorContainer
                        },
                        labelColor = if (statusActive) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                )
            }
            isChip -> {
                AssistChip(
                    onClick = { },
                    label = { Text(value) }
                )
            }
            else -> {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = if (isMultiline) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun EmptyHsnCodeDetailsState(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "HSN Code Not Found",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "The requested HSN code could not be found",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onNavigateBack) {
            Text("Go Back")
        }
    }
}

private fun getCategoryDisplayName(category: HsnCategory): String {
    return when (category) {
        HsnCategory.GENERAL -> "General"
        HsnCategory.AGRICULTURE -> "Agriculture"
        HsnCategory.TEXTILES -> "Textiles"
        HsnCategory.CHEMICALS -> "Chemicals"
        HsnCategory.MACHINERY -> "Machinery"
        HsnCategory.ELECTRONICS -> "Electronics"
        HsnCategory.VEHICLES -> "Vehicles"
        HsnCategory.PRECIOUS_METALS -> "Precious Metals"
        HsnCategory.FOOD_BEVERAGES -> "Food & Beverages"
        HsnCategory.TOBACCO -> "Tobacco"
        HsnCategory.CONSTRUCTION -> "Construction"
        HsnCategory.HEALTHCARE -> "Healthcare"
    }
}

@OptIn(kotlin.time.ExperimentalTime::class)
private fun formatDetailDate(timestamp: Long): String {
    if (timestamp == 0L) return "Unknown"

    // Simple date formatting - in a real app, use proper date formatting
    val date = kotlinx.datetime.Instant.fromEpochMilliseconds(timestamp)
    return date.toString().take(19).replace("T", " ") // Show YYYY-MM-DD HH:MM:SS format
}