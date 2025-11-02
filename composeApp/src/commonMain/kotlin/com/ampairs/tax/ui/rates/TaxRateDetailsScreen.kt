package com.ampairs.tax.ui.rates

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
import com.ampairs.tax.domain.BusinessType
import com.ampairs.tax.domain.TaxRate
import com.ampairs.tax.domain.TaxType
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.time.Clock

@OptIn(ExperimentalMaterial3Api::class, kotlin.time.ExperimentalTime::class)
@Composable
fun TaxRateDetailsScreen(
    taxRateId: String,
    onNavigateBack: () -> Unit,
    onEditTaxRate: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TaxRateDetailsViewModel = koinViewModel { parametersOf(taxRateId) }
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(taxRateId) {
        viewModel.refreshTaxRate()
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("Tax Rate Details") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                uiState.taxRate?.let { taxRate ->
                    IconButton(onClick = { onEditTaxRate(taxRate.id) }) {
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
                EmptyTaxRateDetailsState(
                    onNavigateBack = onNavigateBack,
                    modifier = Modifier.fillMaxSize()
                )
            }

            uiState.showContent -> {
                TaxRateDetailsContent(
                    taxRate = uiState.taxRate!!,
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
            title = { Text("Delete Tax Rate") },
            text = { Text("Are you sure you want to delete this tax rate? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteTaxRate {
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
private fun TaxRateDetailsContent(
    taxRate: TaxRate,
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

        // Main Tax Rate Information
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Tax Rate Information",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                // HSN Code
                DetailRow(
                    label = "HSN Code",
                    value = taxRate.hsnCode,
                    isHighlighted = true
                )

                // Tax Type
                DetailRow(
                    label = "Tax Type",
                    value = getTaxTypeDisplayName(taxRate.taxType)
                )

                // Tax Rate with Cess
                DetailRow(
                    label = "Tax Rate",
                    value = taxRate.formattedRate,
                    isHighlighted = true
                )

                if (taxRate.cessAmountPerUnit != null) {
                    DetailRow(
                        label = "Cess Amount per Unit",
                        value = "₹${taxRate.cessAmountPerUnit}"
                    )
                }

                // Business Type
                DetailRow(
                    label = "Business Type",
                    value = getBusinessTypeDisplayName(taxRate.businessType)
                )

                // Geographical Zone
                DetailRow(
                    label = "Geographical Zone",
                    value = taxRate.geographicalZone
                )
            }
        }

        // Effective Period
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Effective Period",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                DetailRow(
                    label = "Effective From",
                    value = formatDetailDate(taxRate.effectiveFrom)
                )

                DetailRow(
                    label = "Effective To",
                    value = taxRate.effectiveTo?.let { formatDetailDate(it) } ?: "Open-ended"
                )

                DetailRow(
                    label = "Current Status",
                    value = if (taxRate.isCurrentlyEffective) "Currently Effective" else "Not Effective",
                    isStatus = true,
                    statusActive = taxRate.isCurrentlyEffective
                )
            }
        }

        // Tax Calculation Preview
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Tax Calculation Preview",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "For ₹1000 base amount:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val baseAmount = 1000.0
                val taxAmount = baseAmount * taxRate.ratePercentage / 100
                val cessAmount = (taxRate.cessRate?.let { baseAmount * it / 100 } ?: 0.0) +
                        (taxRate.cessAmountPerUnit ?: 0.0)
                val totalAmount = baseAmount + taxAmount + cessAmount

                DetailRow(
                    label = "Base Amount",
                    value = "₹${baseAmount.toInt()}"
                )

                DetailRow(
                    label = "Tax Amount (${taxRate.ratePercentage}%)",
                    value = "₹${taxAmount.toInt()}"
                )

                if (cessAmount > 0) {
                    DetailRow(
                        label = "Cess Amount",
                        value = "₹${cessAmount.toInt()}"
                    )
                }

                HorizontalDivider()

                DetailRow(
                    label = "Total Amount",
                    value = "₹${totalAmount.toInt()}",
                    isHighlighted = true
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
                    label = "Version",
                    value = taxRate.versionNumber.toString()
                )

                DetailRow(
                    label = "Status",
                    value = if (taxRate.isActive) "Active" else "Inactive",
                    isStatus = true,
                    statusActive = taxRate.isActive
                )

                DetailRow(
                    label = "Created",
                    value = formatDetailDate(taxRate.createdAt)
                )

                DetailRow(
                    label = "Last Updated",
                    value = formatDetailDate(taxRate.updatedAt)
                )
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    isHighlighted: Boolean = false,
    isStatus: Boolean = false,
    statusActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )

        if (isStatus) {
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
        } else {
            Text(
                text = value,
                style = if (isHighlighted) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                color = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EmptyTaxRateDetailsState(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Tax Rate Not Found",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "The requested tax rate could not be found",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onNavigateBack) {
            Text("Go Back")
        }
    }
}

private fun getTaxTypeDisplayName(taxType: TaxType): String {
    return when (taxType) {
        TaxType.GST -> "GST"
        TaxType.CGST -> "CGST"
        TaxType.SGST -> "SGST"
        TaxType.IGST -> "IGST"
        TaxType.CESS -> "Cess"
        TaxType.VAT -> "VAT"
        TaxType.EXCISE -> "Excise"
    }
}

private fun getBusinessTypeDisplayName(businessType: BusinessType): String {
    return when (businessType) {
        BusinessType.REGULAR -> "Regular"
        BusinessType.COMPOSITION -> "Composition"
        BusinessType.EXEMPT -> "Exempt"
        BusinessType.NIL_RATED -> "Nil Rated"
        BusinessType.ZERO_RATED -> "Zero Rated"
    }
}

@OptIn(kotlin.time.ExperimentalTime::class)
private fun formatDetailDate(timestamp: Long): String {
    if (timestamp == 0L) return "Unknown"

    // Simple date formatting - in a real app, use proper date formatting
    val date = kotlinx.datetime.Instant.fromEpochMilliseconds(timestamp)
    return date.toString().take(19).replace("T", " ") // Show YYYY-MM-DD HH:MM:SS format
}