package com.ampairs.tax.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ampairs.tax.domain.model.MasterTaxCode
import com.ampairs.tax.domain.model.TaxCodeType
import com.ampairs.tax.domain.model.TaxCode
import org.koin.compose.viewmodel.koinViewModel

/**
 * Tax Code Search Screen
 *
 * Two-tab interface:
 * 1. My Codes - Workspace subscribed codes (offline)
 * 2. Master Codes - Search global registry (requires network)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaxCodeSearchScreen(
    onCodeSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TaxCodeSearchViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val workspaceCodes by viewModel.workspaceCodes.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tax Codes") },
                colors = TopAppBarDefaults.topAppBarColors()
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
            SearchBar(
                query = searchQuery,
                onQueryChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            // Tab Row
            TabRow(
                selectedTabIndex = uiState.selectedTab.ordinal,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = uiState.selectedTab == SearchTab.MY_CODES,
                    onClick = { viewModel.onTabChange(SearchTab.MY_CODES) },
                    text = { Text("My Codes") }
                )
                Tab(
                    selected = uiState.selectedTab == SearchTab.MASTER_CODES,
                    onClick = { viewModel.onTabChange(SearchTab.MASTER_CODES) },
                    text = { Text("Search All") }
                )
            }

            // Filters (only for master codes)
            if (uiState.selectedTab == SearchTab.MASTER_CODES) {
                FilterRow(
                    selectedCodeType = uiState.selectedCodeType,
                    onCodeTypeChange = { viewModel.onCodeTypeFilterChange(it) },
                    selectedCategory = uiState.selectedCategory,
                    onCategoryChange = { viewModel.onCategoryFilterChange(it) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Content
            when (uiState.selectedTab) {
                SearchTab.MY_CODES -> {
                    WorkspaceCodesContent(
                        codes = workspaceCodes,
                        onCodeClick = { code ->
                            viewModel.incrementUsageCount(code.id)
                            onCodeSelected(code.id)
                        },
                        onFavoriteToggle = { code ->
                            viewModel.toggleFavorite(code.id, !code.isFavorite)
                        },
                        onUnsubscribe = { code ->
                            viewModel.unsubscribeFromCode(code.id)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                SearchTab.MASTER_CODES -> {
                    MasterCodesContent(
                        codes = uiState.masterCodes,
                        subscribedCodeId = uiState.lastSubscribedCode?.id,
                        isSearching = uiState.isSearching,
                        searchError = uiState.searchError,
                        onCodeClick = { codeId ->
                            // Navigate to detail screen
                            onCodeSelected(codeId)
                        },
                        onSubscribe = { code ->
                            viewModel.subscribeToCode(code, isFavorite = false)
                        },
                        onClearError = { viewModel.clearSearchError() },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Error Snackbars
            if (uiState.subscribeError != null) {
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearSubscribeError() }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(uiState.subscribeError ?: "")
                }
            }

            if (uiState.unsubscribeError != null) {
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearUnsubscribeError() }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(uiState.unsubscribeError ?: "")
                }
            }
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
        placeholder = { Text("Search tax codes...") },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = "Search")
        },
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
private fun FilterRow(
    selectedCodeType: TaxCodeType?,
    onCodeTypeChange: (TaxCodeType?) -> Unit,
    selectedCategory: String?,
    onCategoryChange: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Code Type Filter
        var codeTypeExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = codeTypeExpanded,
            onExpandedChange = { codeTypeExpanded = it },
            modifier = Modifier.weight(1f)
        ) {
            OutlinedTextField(
                value = selectedCodeType?.name ?: "All Types",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = codeTypeExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = codeTypeExpanded,
                onDismissRequest = { codeTypeExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("All Types") },
                    onClick = {
                        onCodeTypeChange(null)
                        codeTypeExpanded = false
                    }
                )
                TaxCodeType.entries.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type.name) },
                        onClick = {
                            onCodeTypeChange(type)
                            codeTypeExpanded = false
                        }
                    )
                }
            }
        }

        // Category Filter (placeholder - will be dynamic from backend)
        var categoryExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = categoryExpanded,
            onExpandedChange = { categoryExpanded = it },
            modifier = Modifier.weight(1f)
        ) {
            OutlinedTextField(
                value = selectedCategory ?: "All Categories",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = categoryExpanded,
                onDismissRequest = { categoryExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("All Categories") },
                    onClick = {
                        onCategoryChange(null)
                        categoryExpanded = false
                    }
                )
                // Add categories dynamically from backend
            }
        }
    }
}

@Composable
private fun WorkspaceCodesContent(
    codes: List<TaxCode>,
    onCodeClick: (TaxCode) -> Unit,
    onFavoriteToggle: (TaxCode) -> Unit,
    onUnsubscribe: (TaxCode) -> Unit,
    modifier: Modifier = Modifier
) {
    if (codes.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No tax codes subscribed yet.\nSearch and add codes from 'Search All' tab.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(codes, key = { it.id }) { code ->
                WorkspaceTaxCodeCard(
                    code = code,
                    onClick = { onCodeClick(code) },
                    onFavoriteToggle = { onFavoriteToggle(code) },
                    onUnsubscribe = { onUnsubscribe(code) }
                )
            }
        }
    }
}

@Composable
private fun WorkspaceTaxCodeCard(
    code: TaxCode,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onUnsubscribe: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = code.code,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = code.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        AssistChip(
                            onClick = {},
                            label = { Text(code.codeType.name) }
                        )
                        if (code.usageCount > 0) {
                            AssistChip(
                                onClick = {},
                                label = { Text("Used ${code.usageCount}x") }
                            )
                        }
                        // Sync Status Indicator
                        SyncStatusChip(syncStatus = code.syncStatus)
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onFavoriteToggle) {
                        Icon(
                            imageVector = if (code.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (code.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onUnsubscribe) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Unsubscribe",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MasterCodesContent(
    codes: List<MasterTaxCode>,
    subscribedCodeId: String?,
    isSearching: Boolean,
    searchError: String?,
    onCodeClick: (String) -> Unit,
    onSubscribe: (MasterTaxCode) -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            isSearching -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            searchError != null -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = searchError,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    TextButton(onClick = onClearError) {
                        Text("Dismiss")
                    }
                }
            }

            codes.isEmpty() -> {
                Text(
                    text = "Search for tax codes to add to your workspace",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(codes, key = { it.id }) { code ->
                        MasterTaxCodeCard(
                            code = code,
                            onClick = {
                                // If this code was just subscribed, navigate to it
                                if (subscribedCodeId != null) {
                                    onCodeClick(subscribedCodeId)
                                }
                            },
                            onSubscribe = { onSubscribe(code) },
                            isSubscribed = code.id == subscribedCodeId
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MasterTaxCodeCard(
    code: MasterTaxCode,
    onClick: () -> Unit,
    onSubscribe: () -> Unit,
    isSubscribed: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = code.code,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = code.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        AssistChip(
                            onClick = {},
                            label = { Text(code.codeType.name) }
                        )
                        if (code.chapter != null) {
                            AssistChip(
                                onClick = {},
                                label = { Text("Ch. ${code.chapter}") }
                            )
                        }
                        if (code.defaultTaxRate != null) {
                            AssistChip(
                                onClick = {},
                                label = { Text("${code.defaultTaxRate}%") }
                            )
                        }
                    }
                }

                if (isSubscribed) {
                    FilledTonalButton(
                        onClick = onClick,
                        modifier = Modifier.padding(start = 8.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text("View")
                    }
                } else {
                    FilledTonalButton(
                        onClick = onSubscribe,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add")
                    }
                }
            }
        }
    }
}

/**
 * Sync Status Indicator Chip
 * Shows the current sync status of a tax code
 */
@Composable
private fun SyncStatusChip(
    syncStatus: String,
    modifier: Modifier = Modifier
) {
    val (icon, label, color) = when (syncStatus) {
        "SYNCED" -> Triple(
            Icons.Default.CloudDone,
            "Synced",
            MaterialTheme.colorScheme.primary
        )
        "PENDING" -> Triple(
            Icons.Default.CloudQueue,
            "Pending",
            MaterialTheme.colorScheme.tertiary
        )
        "DELETE_PENDING" -> Triple(
            Icons.Default.CloudOff,
            "Deleting",
            MaterialTheme.colorScheme.error
        )
        else -> Triple(
            Icons.Default.CloudQueue,
            syncStatus,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    AssistChip(
        onClick = {},
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(16.dp),
                tint = color
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            labelColor = color,
            leadingIconContentColor = color
        ),
        modifier = modifier
    )
}
