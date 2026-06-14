package com.ampairs.tax.ui.list

import androidx.compose.foundation.background
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.ampairs.tax.domain.model.TaxCode
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.stringResource
import ampairsapp.feature.tax.generated.resources.Res
import ampairsapp.feature.tax.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTaxCodesScreen(
    onNavigateToEdit: (String) -> Unit,
    onNavigateToSearch: () -> Unit,
    viewModel: MyTaxCodesViewModel = metroViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val taxCodes by viewModel.taxCodes.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val isExpanded = windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)

    Scaffold(
        floatingActionButton = {
            if (!isExpanded) {
                ExtendedFloatingActionButton(
                    onClick = onNavigateToSearch,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(stringResource(Res.string.tax_list_add_code)) }
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Inline header
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(Res.string.tax_list_title),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (taxCodes.isNotEmpty()) {
                                Text(
                                    text = stringResource(Res.string.tax_list_count, taxCodes.size),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (isExpanded) {
                                Button(onClick = onNavigateToSearch) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(stringResource(Res.string.tax_list_add_code))
                                }
                                Spacer(Modifier.width(4.dp))
                            }
                            IconButton(
                                onClick = { viewModel.syncTaxCodes() },
                                enabled = !uiState.isSyncing
                            ) {
                                if (uiState.isSyncing) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.Refresh, contentDescription = stringResource(Res.string.tax_list_cd_sync))
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.onSearchQueryChange(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(Res.string.tax_list_search_placeholder), style = MaterialTheme.typography.bodyMedium) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                    Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.tax_list_cd_search_clear), modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        shape = MaterialTheme.shapes.medium
                    )
                    Spacer(Modifier.height(8.dp))
                    // Filter chips row
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = uiState.showFavoritesOnly,
                            onClick = { viewModel.toggleFavoritesOnly() },
                            label = { Text(stringResource(Res.string.tax_list_filter_favorites)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (uiState.showFavoritesOnly) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                        FilterChip(
                            selected = uiState.sortBy == TaxCodeSortBy.USAGE_COUNT,
                            onClick = { viewModel.onSortByChange(TaxCodeSortBy.USAGE_COUNT) },
                            label = { Text(stringResource(Res.string.tax_list_filter_most_used)) }
                        )
                        FilterChip(
                            selected = uiState.sortBy == TaxCodeSortBy.RECENTLY_ADDED,
                            onClick = { viewModel.onSortByChange(TaxCodeSortBy.RECENTLY_ADDED) },
                            label = { Text(stringResource(Res.string.tax_list_filter_recently_added)) }
                        )
                        FilterChip(
                            selected = uiState.sortBy == TaxCodeSortBy.CODE,
                            onClick = { viewModel.onSortByChange(TaxCodeSortBy.CODE) },
                            label = { Text(stringResource(Res.string.tax_list_filter_by_code)) }
                        )
                    }
                }
            }

            // Stats card (when there are codes)
            if (taxCodes.isNotEmpty()) {
                val favCount = taxCodes.count { it.isFavorite }
                val unsyncedCount = taxCodes.count { it.syncStatus != "SYNCED" }
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TaxStatItem(
                            icon = Icons.Default.Code,
                            label = stringResource(Res.string.tax_list_stats_total),
                            value = taxCodes.size.toString()
                        )
                        TaxStatItem(
                            icon = Icons.Default.Favorite,
                            label = stringResource(Res.string.tax_list_stats_favorites),
                            value = favCount.toString(),
                            iconTint = MaterialTheme.colorScheme.error
                        )
                        if (unsyncedCount > 0) {
                            TaxStatItem(
                                icon = Icons.Default.CloudOff,
                                label = stringResource(Res.string.tax_list_stats_unsynced),
                                value = unsyncedCount.toString(),
                                iconTint = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            // Content
            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                uiState.errorMessage != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                                Text(stringResource(Res.string.tax_retry))
                            }
                        }
                    }
                }
                taxCodes.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                                text = stringResource(Res.string.tax_list_empty_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(Res.string.tax_list_empty_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(onClick = onNavigateToSearch) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(Res.string.tax_list_browse_codes))
                            }
                        }
                    }
                }
                isExpanded -> {
                    TaxCodesTable(
                        taxCodes = taxCodes,
                        onCardClick = { onNavigateToEdit(it.id) },
                        onFavoriteToggle = { viewModel.toggleFavorite(it) },
                        onUnsubscribe = { code -> viewModel.showUnsubscribeDialog(code) }
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(taxCodes, key = { it.id }) { taxCode ->
                            TaxCodeCard(
                                taxCode = taxCode,
                                onCardClick = { onNavigateToEdit(taxCode.id) },
                                onFavoriteToggle = { viewModel.toggleFavorite(taxCode.id) },
                                onUnsubscribe = { viewModel.showUnsubscribeDialog(taxCode) }
                            )
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }

    // Unsubscribe dialog
    if (uiState.codeToUnsubscribe != null) {
        val code = uiState.codeToUnsubscribe!!
        AlertDialog(
            onDismissRequest = { viewModel.hideUnsubscribeDialog() },
            icon = {
                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            },
            title = { Text(stringResource(Res.string.tax_unsub_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(Res.string.tax_unsub_dialog_text))
                    Text(
                        text = "${code.code} - ${code.description}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (code.usageCount > 0) {
                        Text(
                            text = stringResource(Res.string.tax_unsub_used_times_format, code.usageCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.unsubscribeFromCode() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(Res.string.tax_unsub_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideUnsubscribeDialog() }) {
                    Text(stringResource(Res.string.tax_cancel))
                }
            }
        )
    }

    // Sync success snackbar
    if (uiState.syncSuccessMessage != null) {
        LaunchedEffect(uiState.syncSuccessMessage) {
            kotlinx.coroutines.delay(2000)
            viewModel.clearSyncMessage()
        }
    }
}

@Composable
private fun TaxCodesTable(
    taxCodes: List<TaxCode>,
    onCardClick: (TaxCode) -> Unit,
    onFavoriteToggle: (String) -> Unit,
    onUnsubscribe: (TaxCode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Header row
        Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.tax_list_col_code),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1.5f)
                )
                Text(
                    text = stringResource(Res.string.tax_list_col_type),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stringResource(Res.string.tax_list_col_status),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stringResource(Res.string.tax_list_col_usage),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(0.8f)
                )
                Spacer(Modifier.width(72.dp))
            }
        }
        HorizontalDivider()
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(taxCodes, key = { it.id }) { taxCode ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCardClick(taxCode) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1.5f)) {
                        Text(
                            text = taxCode.code,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = taxCode.shortDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = taxCode.codeType.name,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    val syncLabel = when (taxCode.syncStatus) {
                        "SYNCED" -> stringResource(Res.string.tax_sync_synced)
                        "PENDING" -> stringResource(Res.string.tax_sync_pending)
                        "DELETE_PENDING" -> stringResource(Res.string.tax_sync_deleting)
                        else -> taxCode.syncStatus
                    }
                    val syncColor = when (taxCode.syncStatus) {
                        "SYNCED" -> MaterialTheme.colorScheme.primary
                        "PENDING" -> MaterialTheme.colorScheme.tertiary
                        "DELETE_PENDING" -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Text(
                        text = syncLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = syncColor,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = taxCode.usageCount.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(0.8f)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                        IconButton(onClick = { onFavoriteToggle(taxCode.id) }) {
                            Icon(
                                imageVector = if (taxCode.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = stringResource(Res.string.tax_list_cd_favorite),
                                tint = if (taxCode.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(onClick = { onUnsubscribe(taxCode) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(Res.string.tax_list_cd_unsubscribe),
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
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
    Surface(
        modifier = modifier.fillMaxWidth().clickable(onClick = onCardClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
                Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                    IconButton(onClick = onFavoriteToggle) {
                        Icon(
                            imageVector = if (taxCode.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = stringResource(Res.string.tax_list_cd_favorite),
                            tint = if (taxCode.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onUnsubscribe) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(Res.string.tax_list_cd_unsubscribe),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (taxCode.description != taxCode.shortDescription) {
                Text(
                    text = taxCode.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                AssistChip(
                    onClick = {},
                    label = { Text(taxCode.codeType.name, style = MaterialTheme.typography.labelSmall) }
                )
                if (taxCode.usageCount > 0) {
                    AssistChip(
                        onClick = {},
                        label = { Text(stringResource(Res.string.tax_list_used_times_format, taxCode.usageCount), style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = {
                            Icon(Icons.Default.Timeline, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                }
                TaxSyncStatusChip(syncStatus = taxCode.syncStatus)
            }

            taxCode.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun TaxSyncStatusChip(syncStatus: String, modifier: Modifier = Modifier) {
    val (icon, color) = when (syncStatus) {
        "SYNCED" -> Icons.Default.CloudDone to MaterialTheme.colorScheme.primary
        "PENDING" -> Icons.Default.CloudQueue to MaterialTheme.colorScheme.tertiary
        "DELETE_PENDING" -> Icons.Default.CloudOff to MaterialTheme.colorScheme.error
        else -> Icons.Default.CloudQueue to MaterialTheme.colorScheme.onSurfaceVariant
    }
    val label = when (syncStatus) {
        "SYNCED" -> stringResource(Res.string.tax_sync_synced)
        "PENDING" -> stringResource(Res.string.tax_sync_pending)
        "DELETE_PENDING" -> stringResource(Res.string.tax_sync_deleting)
        else -> syncStatus
    }
    AssistChip(
        onClick = {},
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
        colors = AssistChipDefaults.assistChipColors(labelColor = color, leadingIconContentColor = color),
        modifier = modifier
    )
}

@Composable
private fun TaxStatItem(
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
        Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
        Text(text = value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}
