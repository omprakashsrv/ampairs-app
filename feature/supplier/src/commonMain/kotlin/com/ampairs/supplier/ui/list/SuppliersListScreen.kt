package com.ampairs.supplier.ui.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ampairs.supplier.domain.SupplierListItem
import dev.zacsweers.metrox.viewmodel.metroViewModel
import ampairsapp.feature.supplier.generated.resources.Res
import ampairsapp.feature.supplier.generated.resources.supplier_list_add_btn
import ampairsapp.feature.supplier.generated.resources.supplier_list_add_cd
import ampairsapp.feature.supplier.generated.resources.supplier_list_empty
import ampairsapp.feature.supplier.generated.resources.supplier_list_empty_desc
import ampairsapp.feature.supplier.generated.resources.supplier_list_error_title
import ampairsapp.feature.supplier.generated.resources.supplier_list_search_hint
import ampairsapp.feature.supplier.generated.resources.supplier_list_title
import ampairsapp.feature.supplier.generated.resources.supplier_retry
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuppliersListScreen(
    onSupplierClick: (String) -> Unit,
    onCreateSupplier: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SuppliersListViewModel = metroViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.syncSuppliers()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text(stringResource(Res.string.supplier_list_title)) })
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateSupplier,
                icon = { Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.supplier_list_add_cd)) },
                text = { Text(stringResource(Res.string.supplier_list_add_btn)) },
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text(stringResource(Res.string.supplier_list_search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
            )

            if (uiState.isRefreshing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            when {
                uiState.isLoading && uiState.suppliers.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                uiState.error != null && uiState.suppliers.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                stringResource(Res.string.supplier_list_error_title),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            uiState.error?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                            TextButton(onClick = viewModel::loadSuppliers) {
                                Text(stringResource(Res.string.supplier_retry))
                            }
                        }
                    }
                }

                uiState.suppliers.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                stringResource(Res.string.supplier_list_empty),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                stringResource(Res.string.supplier_list_empty_desc),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                    ) {
                        items(uiState.suppliers, key = { it.id }) { supplier ->
                            SupplierRow(supplier = supplier, onClick = { onSupplierClick(supplier.id) })
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SupplierRow(
    supplier: SupplierListItem,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            supplier.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        val subtitle = listOfNotNull(
            supplier.phone?.takeIf { it.isNotBlank() },
            supplier.city?.takeIf { it.isNotBlank() },
        ).joinToString(" • ")
        if (subtitle.isNotBlank()) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
