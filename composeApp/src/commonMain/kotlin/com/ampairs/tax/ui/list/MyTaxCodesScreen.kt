package com.ampairs.tax.ui.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ampairs.tax.domain.model.TaxCode
import org.koin.compose.viewmodel.koinViewModel

/**
 * My Tax Codes Screen
 *
 * Displays user's subscribed tax codes with management features:
 * - View all subscribed codes
 * - Edit custom configurations
 * - Mark as favorite
 * - Unsubscribe from codes
 * - View usage statistics
 * - Offline-first with sync status indicators
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTaxCodesScreen(
    onNavigateToEdit: (String) -> Unit,
    onNavigateToSearch: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyTaxCodesViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val taxCodes by viewModel.taxCodes.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Tax Codes") },
                actions = {
                    // Search icon
                    IconButton(onClick = { viewModel.toggleSearchMode() }) {
                        Icon(
                            imageVector = if (uiState.isSearchMode) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = if (uiState.isSearchMode) "Close search" else "Search"
                        )
                    }

                    // Sync icon
                    IconButton(
                        onClick = { viewModel.syncTaxCodes() },
                        enabled = !uiState.isSyncing
                    ) {
                        if (uiState.isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Sync")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToSearch,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Code") }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar
            if (uiState.isSearchMode) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.onSearchQueryChange(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }

            // Filter Chips
            FilterChips(
                showFavoritesOnly = uiState.showFavoritesOnly,
                sortBy = uiState.sortBy,
                onShowFavoritesToggle = { viewModel.toggleFavoritesOnly() },
                onSortByChange = { viewModel.onSortByChange(it) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Stats Card
            if (taxCodes.isNotEmpty()) {
                StatsCard(
                    totalCodes = taxCodes.size,
                    favoriteCodes = taxCodes.count { it.isFavorite },
                    unsyncedCodes = taxCodes.count { it.syncStatus != "SYNCED" },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Tax Codes List
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = uiState.errorMessage ?: "",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            Button(onClick = { viewModel.loadTaxCodes() }) {
                                Text("Retry")
                            }
                        }
                    }
                }

                taxCodes.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "No tax codes yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Add codes from the master registry\nto start using them",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(onClick = onNavigateToSearch) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Browse Tax Codes")
                            }
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(taxCodes, key = { it.id }) { taxCode ->
                            TaxCodeCard(
                                taxCode = taxCode,
                                onCardClick = { onNavigateToEdit(taxCode.id) },
                                onFavoriteToggle = { viewModel.toggleFavorite(taxCode.id) },
                                onUnsubscribe = { viewModel.showUnsubscribeDialog(taxCode) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Unsubscribe Confirmation Dialog
    if (uiState.codeToUnsubscribe != null) {
        AlertDialog(
            onDismissRequest = { viewModel.hideUnsubscribeDialog() },
            title = { Text("Unsubscribe from Tax Code?") },
            text = {
                Text(
                    "Are you sure you want to unsubscribe from ${uiState.codeToUnsubscribe?.code}?\n\n" +
                        "This will remove the code from your workspace. You can re-add it later from the master registry."
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.unsubscribeFromCode() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Unsubscribe")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideUnsubscribeDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Sync Success Snackbar
    if (uiState.syncSuccessMessage != null) {
        LaunchedEffect(uiState.syncSuccessMessage) {
            kotlinx.coroutines.delay(2000)
            viewModel.clearSyncMessage()
        }

        Snackbar(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(uiState.syncSuccessMessage ?: "")
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = { Text("Search by code or description...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear")
                }
            }
        },
        singleLine = true
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterChips(
    showFavoritesOnly: Boolean,
    sortBy: TaxCodeSortBy,
    onShowFavoritesToggle: () -> Unit,
    onSortByChange: (TaxCodeSortBy) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Favorites filter
        FilterChip(
            selected = showFavoritesOnly,
            onClick = onShowFavoritesToggle,
            label = { Text("Favorites") },
            leadingIcon = {
                Icon(
                    imageVector = if (showFavoritesOnly) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        )

        // Sort by usage
        FilterChip(
            selected = sortBy == TaxCodeSortBy.USAGE_COUNT,
            onClick = { onSortByChange(TaxCodeSortBy.USAGE_COUNT) },
            label = { Text("Most Used") }
        )

        // Sort by recent
        FilterChip(
            selected = sortBy == TaxCodeSortBy.RECENTLY_ADDED,
            onClick = { onSortByChange(TaxCodeSortBy.RECENTLY_ADDED) },
            label = { Text("Recently Added") }
        )

        // Sort by code
        FilterChip(
            selected = sortBy == TaxCodeSortBy.CODE,
            onClick = { onSortByChange(TaxCodeSortBy.CODE) },
            label = { Text("By Code") }
        )
    }
}

@Composable
private fun StatsCard(
    totalCodes: Int,
    favoriteCodes: Int,
    unsyncedCodes: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                icon = Icons.Default.Code,
                label = "Total",
                value = totalCodes.toString()
            )
            StatItem(
                icon = Icons.Default.Favorite,
                label = "Favorites",
                value = favoriteCodes.toString(),
                iconTint = MaterialTheme.colorScheme.error
            )
            if (unsyncedCodes > 0) {
                StatItem(
                    icon = Icons.Default.CloudOff,
                    label = "Unsynced",
                    value = unsyncedCodes.toString(),
                    iconTint = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    iconTint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSecondaryContainer,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun TaxCodeCard(
    taxCode: TaxCode,
    onCardClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onUnsubscribe: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header row with code and actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = taxCode.code,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = taxCode.shortDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = onFavoriteToggle) {
                        Icon(
                            imageVector = if (taxCode.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Toggle favorite",
                            tint = if (taxCode.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
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

            // Description (expandable)
            if (taxCode.description != taxCode.shortDescription) {
                Text(
                    text = taxCode.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Chips row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Code type
                AssistChip(
                    onClick = {},
                    label = { Text(taxCode.codeType.name, style = MaterialTheme.typography.labelSmall) }
                )

                // Usage count
                if (taxCode.usageCount > 0) {
                    AssistChip(
                        onClick = {},
                        label = { Text("Used ${taxCode.usageCount}x", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Timeline,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }

                // Sync status
                SyncStatusChip(syncStatus = taxCode.syncStatus)
            }

            // Notes (if any)
            taxCode.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncStatusChip(
    syncStatus: String,
    modifier: Modifier = Modifier
) {
    val (icon, label, color) = when (syncStatus) {
        "SYNCED" -> Triple(Icons.Default.CloudDone, "Synced", MaterialTheme.colorScheme.primary)
        "PENDING" -> Triple(Icons.Default.CloudQueue, "Pending", MaterialTheme.colorScheme.tertiary)
        "DELETE_PENDING" -> Triple(Icons.Default.CloudOff, "Deleting", MaterialTheme.colorScheme.error)
        else -> Triple(Icons.Default.CloudQueue, syncStatus, MaterialTheme.colorScheme.onSurfaceVariant)
    }

    AssistChip(
        onClick = {},
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            labelColor = color,
            leadingIconContentColor = color
        ),
        modifier = modifier
    )
}
