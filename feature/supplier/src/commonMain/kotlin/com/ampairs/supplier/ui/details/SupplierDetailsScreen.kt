package com.ampairs.supplier.ui.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import com.ampairs.supplier.domain.Supplier
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import ampairsapp.feature.supplier.generated.resources.Res
import ampairsapp.feature.supplier.generated.resources.supplier_delete_cancel
import ampairsapp.feature.supplier.generated.resources.supplier_delete_confirm
import ampairsapp.feature.supplier.generated.resources.supplier_delete_confirm_msg
import ampairsapp.feature.supplier.generated.resources.supplier_delete_confirm_title
import ampairsapp.feature.supplier.generated.resources.supplier_details_address
import ampairsapp.feature.supplier.generated.resources.supplier_details_back
import ampairsapp.feature.supplier.generated.resources.supplier_details_contact
import ampairsapp.feature.supplier.generated.resources.supplier_details_credit_days
import ampairsapp.feature.supplier.generated.resources.supplier_details_credit_limit
import ampairsapp.feature.supplier.generated.resources.supplier_details_delete
import ampairsapp.feature.supplier.generated.resources.supplier_details_edit
import ampairsapp.feature.supplier.generated.resources.supplier_details_email
import ampairsapp.feature.supplier.generated.resources.supplier_details_gst
import ampairsapp.feature.supplier.generated.resources.supplier_details_landline
import ampairsapp.feature.supplier.generated.resources.supplier_details_not_found
import ampairsapp.feature.supplier.generated.resources.supplier_details_outstanding
import ampairsapp.feature.supplier.generated.resources.supplier_details_pan
import ampairsapp.feature.supplier.generated.resources.supplier_details_phone
import ampairsapp.feature.supplier.generated.resources.supplier_details_tax
import ampairsapp.feature.supplier.generated.resources.supplier_details_title
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierDetailsScreen(
    supplierId: String,
    onNavigateBack: () -> Unit,
    onEditSupplier: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SupplierDetailsViewModel = assistedMetroViewModel<SupplierDetailsViewModel, SupplierDetailsViewModel.Factory>(
        key = supplierId
    ) { create(supplierId) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(supplierId) {
        viewModel.loadSupplier()
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(Res.string.supplier_delete_confirm_title)) },
            text = { Text(stringResource(Res.string.supplier_delete_confirm_msg)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteSupplier(onSuccess = onNavigateBack)
                }) { Text(stringResource(Res.string.supplier_delete_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(Res.string.supplier_delete_cancel))
                }
            },
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.supplier_details_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.supplier_details_back),
                        )
                    }
                },
                actions = {
                    val supplier = uiState.supplier
                    if (supplier != null) {
                        IconButton(onClick = { onEditSupplier(supplier.uid) }) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(Res.string.supplier_details_edit))
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.supplier_details_delete))
                        }
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                uiState.supplier == null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(Res.string.supplier_details_not_found))
                    }
                }

                else -> {
                    SupplierDetailsContent(supplier = uiState.supplier!!)
                }
            }
        }
    }
}

@Composable
private fun SupplierDetailsContent(supplier: Supplier) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            supplier.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        DetailSection(stringResource(Res.string.supplier_details_contact)) {
            DetailRow(stringResource(Res.string.supplier_details_phone), supplier.phone)
            DetailRow(stringResource(Res.string.supplier_details_landline), supplier.landline)
            DetailRow(stringResource(Res.string.supplier_details_email), supplier.email)
        }

        DetailSection(stringResource(Res.string.supplier_details_address)) {
            DetailRow(null, listOfNotNull(supplier.street, supplier.street2, supplier.city, supplier.state, supplier.pincode, supplier.country)
                .filter { it.isNotBlank() }
                .joinToString(", ")
                .takeIf { it.isNotBlank() })
        }

        DetailSection(stringResource(Res.string.supplier_details_tax)) {
            DetailRow(stringResource(Res.string.supplier_details_gst), supplier.gstNumber)
            DetailRow(stringResource(Res.string.supplier_details_pan), supplier.panNumber)
            DetailRow(stringResource(Res.string.supplier_details_credit_limit), supplier.creditLimit?.toString())
            DetailRow(stringResource(Res.string.supplier_details_credit_days), supplier.creditDays?.toString())
            DetailRow(stringResource(Res.string.supplier_details_outstanding), supplier.outstandingAmount?.toString())
        }
    }
}

@Composable
private fun DetailSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        HorizontalDivider()
        content()
    }
}

@Composable
private fun DetailRow(label: String?, value: String?) {
    if (value.isNullOrBlank()) return
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        if (label != null) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
