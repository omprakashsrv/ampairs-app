package com.ampairs.tax.ui.rates

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ampairs.tax.domain.BusinessType
import com.ampairs.tax.domain.TaxRateListItem
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaxRatesListScreen(
    onTaxRateClick: (String) -> Unit,
    onCreateTaxRate: () -> Unit,
    onFormConfig: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: TaxRatesListViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSearchBar by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadTaxRates()
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Top App Bar with Search and Filter
        TopAppBar(
            title = { Text("Tax Rates") },
            actions = {
                IconButton(onClick = onFormConfig) {
                    Icon(Icons.Default.Settings, contentDescription = "Form Settings")
                }
                IconButton(onClick = { showSearchBar = !showSearchBar }) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
                IconButton(onClick = { showFilterDialog = true }) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filter")
                }
            }
        )

        // Search Bar
        if (showSearchBar) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                label = { Text("Search tax rates") },
                placeholder = { Text("Enter HSN code...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // Active Filters Display
        if (uiState.selectedBusinessType != null) {
            ElevatedFilterChip(
                onClick = { viewModel.updateBusinessTypeFilter(null) },
                label = { Text("Business: ${getBusinessTypeDisplayName(uiState.selectedBusinessType!!)}") },
                selected = true,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        // Content
        Box(modifier = Modifier.weight(1f)) {
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
                    EmptyTaxRatesState(
                        onCreateTaxRate = onCreateTaxRate,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                uiState.showNoResults -> {
                    NoTaxRatesFoundState(
                        searchQuery = uiState.searchQuery,
                        onClearFilters = viewModel::clearFilters,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                else -> {
                    TaxRatesContent(
                        taxRates = uiState.filteredTaxRates,
                        onTaxRateClick = onTaxRateClick,
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = viewModel::refreshTaxRates,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Error Snackbar
            uiState.error?.let { error ->
                LaunchedEffect(error) {
                    // Show error snackbar
                    viewModel.clearError()
                }
            }
        }

        // Floating Action Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            FloatingActionButton(
                onClick = onCreateTaxRate,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Tax Rate")
            }
        }
    }

    // Filter Dialog
    if (showFilterDialog) {
        TaxRateFilterDialog(
            selectedBusinessType = uiState.selectedBusinessType,
            onBusinessTypeChange = viewModel::updateBusinessTypeFilter,
            onDismiss = { showFilterDialog = false },
            onClearFilters = {
                viewModel.clearFilters()
                showFilterDialog = false
            }
        )
    }
}

@Composable
private fun TaxRatesContent(
    taxRates: List<TaxRateListItem>,
    onTaxRateClick: (String) -> Unit,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = taxRates,
            key = { it.id }
        ) { taxRate ->
            TaxRateItem(
                taxRate = taxRate,
                onClick = { onTaxRateClick(taxRate.id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaxRateItem(
    taxRate: TaxRateListItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "HSN ${taxRate.hsnCode}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = taxRate.hsnDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "${taxRate.gstRate}%",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (taxRate.cessRate != null && taxRate.cessRate > 0) {
                        Text(
                            text = "+ ${taxRate.cessRate}% Cess",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Details Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AssistChip(
                        onClick = { },
                        label = { Text(getBusinessTypeDisplayName(taxRate.businessType)) },
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    if (!taxRate.isActive) {
                        AssistChip(
                            onClick = { },
                            label = { Text("Inactive") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        )
                    }
                }

                Text(
                    text = formatEffectiveDate(taxRate.effectiveFrom),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyTaxRatesState(
    onCreateTaxRate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No Tax Rates",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Create your first tax rate to get started",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onCreateTaxRate) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create Tax Rate")
        }
    }
}

@Composable
private fun NoTaxRatesFoundState(
    searchQuery: String,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No Results Found",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (searchQuery.isNotBlank()) {
            Text(
                text = "No tax rates found for \"$searchQuery\"",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = "No tax rates match the current filters",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedButton(onClick = onClearFilters) {
            Text("Clear Filters")
        }
    }
}

@Composable
private fun TaxRateFilterDialog(
    selectedBusinessType: BusinessType?,
    onBusinessTypeChange: (BusinessType?) -> Unit,
    onDismiss: () -> Unit,
    onClearFilters: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter Tax Rates") },
        text = {
            Column {
                Text(
                    text = "Business Type",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                BusinessType.entries.forEach { businessType ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = selectedBusinessType == businessType,
                            onClick = { onBusinessTypeChange(businessType) }
                        )
                        Text(
                            text = getBusinessTypeDisplayName(businessType),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = selectedBusinessType == null,
                        onClick = { onBusinessTypeChange(null) }
                    )
                    Text(
                        text = "All Types",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        },
        dismissButton = {
            TextButton(onClick = onClearFilters) {
                Text("Clear All")
            }
        }
    )
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
private fun formatEffectiveDate(timestamp: Long): String {
    val now = Clock.System.now().toEpochMilliseconds()
    val daysDiff = (now - timestamp) / (24 * 60 * 60 * 1000)

    return when {
        daysDiff < 1 -> "Today"
        daysDiff < 7 -> "${daysDiff}d ago"
        daysDiff < 30 -> "${daysDiff / 7}w ago"
        daysDiff < 365 -> "${daysDiff / 30}mo ago"
        else -> "${daysDiff / 365}y ago"
    }
}