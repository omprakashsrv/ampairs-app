package com.ampairs.product.ui.variant

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ampairs.product.domain.ProductVariant
import com.ampairs.tax.util.formatDecimal
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import org.jetbrains.compose.resources.stringResource
import ampairsapp.feature.product.generated.resources.Res
import ampairsapp.feature.product.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VariantManagementScreen(
    productId: String,
    productName: String,
    onNavigateToForm: (variantId: String?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VariantManagementViewModel = assistedMetroViewModel<VariantManagementViewModel, VariantManagementViewModel.Factory> { create(productId) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.variantToDelete != null) {
        DeleteConfirmationDialog(
            variantName = uiState.variantToDelete!!.variantName,
            isDeleting = uiState.isDeleting,
            onConfirm = { viewModel.deleteVariant() },
            onDismiss = { viewModel.hideDeleteDialog() }
        )
    }

    uiState.successMessage?.let { message ->
        LaunchedEffect(message) {
            kotlinx.coroutines.delay(2000)
            viewModel.clearSuccessMessage()
        }
        Snackbar(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(message)
        }
    }

    uiState.errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { viewModel.clearErrorMessage() },
            title = { Text(stringResource(Res.string.prod_dialog_error)) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearErrorMessage() }) {
                    Text(stringResource(Res.string.prod_ok))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.prod_variant_list_title, productName)) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToForm(null) }
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.prod_variant_add))
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.variants.isEmpty()) {
            EmptyVariantsState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                onAddVariant = { onNavigateToForm(null) }
            )
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    SummaryCard(
                        totalStock = uiState.totalStock,
                        variantCount = uiState.variants.size
                    )
                }

                items(
                    items = uiState.variants,
                    key = { it.id }
                ) { variant ->
                    VariantCard(
                        variant = variant,
                        onEdit = { onNavigateToForm(variant.id) },
                        onDelete = { viewModel.showDeleteDialog(variant) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(
    totalStock: Double,
    variantCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = stringResource(Res.string.prod_details_total_stock_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = totalStock.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(Res.string.prod_details_variants_count_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = variantCount.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun VariantCard(
    variant: ProductVariant,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onEdit
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = variant.variantName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(Res.string.prod_sku_format, variant.sku),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = stringResource(Res.string.prod_details_cd_edit),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(Res.string.prod_details_cd_delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            val attributes = buildList {
                variant.attribute1Name?.let { name ->
                    variant.attribute1Value?.let { value ->
                        add("$name: $value")
                    }
                }
                variant.attribute2Name?.let { name ->
                    variant.attribute2Value?.let { value ->
                        add("$name: $value")
                    }
                }
                variant.attribute3Name?.let { name ->
                    variant.attribute3Value?.let { value ->
                        add("$name: $value")
                    }
                }
            }

            if (attributes.isNotEmpty()) {
                Text(
                    text = attributes.joinToString(" • "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                variant.mrp?.let { mrp ->
                    PriceChip(label = stringResource(Res.string.prod_label_mrp), value = mrp)
                }
                variant.sellingPrice?.let { price ->
                    PriceChip(label = stringResource(Res.string.prod_variant_selling), value = price)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.prod_stock_format, variant.stockQuantity),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                when {
                    variant.isOutOfStock -> {
                        AssistChip(
                            onClick = {},
                            label = { Text(stringResource(Res.string.prod_out_of_stock)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                labelColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        )
                    }
                    variant.isLowStock -> {
                        AssistChip(
                            onClick = {},
                            label = { Text(stringResource(Res.string.prod_variant_low_stock)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                labelColor = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        )
                    }
                }
            }

            if (!variant.active) {
                AssistChip(
                    onClick = {},
                    label = { Text(stringResource(Res.string.prod_inactive)) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
        }
    }
}

@Composable
private fun PriceChip(
    label: String,
    value: Double,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "$label:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = value.formatDecimal(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun EmptyVariantsState(
    onAddVariant: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(Res.string.prod_variant_empty_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(Res.string.prod_variant_empty_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onAddVariant) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(Res.string.prod_variant_add))
            }
        }
    }
}

@Composable
private fun DeleteConfirmationDialog(
    variantName: String,
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.prod_variant_delete_title)) },
        text = { Text(stringResource(Res.string.prod_variant_delete_confirm, variantName)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isDeleting
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(stringResource(Res.string.prod_details_delete_btn), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isDeleting
            ) {
                Text(stringResource(Res.string.prod_cancel))
            }
        }
    )
}
