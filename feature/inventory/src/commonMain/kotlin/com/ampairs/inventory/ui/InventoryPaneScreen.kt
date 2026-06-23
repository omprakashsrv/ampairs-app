package com.ampairs.inventory.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ampairsapp.feature.inventory.generated.resources.Res
import ampairsapp.feature.inventory.generated.resources.inv_action_add_item
import ampairsapp.feature.inventory.generated.resources.inv_back
import ampairsapp.feature.inventory.generated.resources.inv_empty_filtered_title
import ampairsapp.feature.inventory.generated.resources.inv_filter_all
import ampairsapp.feature.inventory.generated.resources.inv_filter_inactive
import ampairsapp.feature.inventory.generated.resources.inv_filter_low
import ampairsapp.feature.inventory.generated.resources.inv_filter_out
import ampairsapp.feature.inventory.generated.resources.inv_list_empty_desc
import ampairsapp.feature.inventory.generated.resources.inv_list_empty_title
import ampairsapp.feature.inventory.generated.resources.inv_list_search_placeholder
import ampairsapp.feature.inventory.generated.resources.inv_list_select_prompt
import ampairsapp.feature.inventory.generated.resources.inv_list_title
import ampairsapp.feature.inventory.generated.resources.inv_sort
import ampairsapp.feature.inventory.generated.resources.inv_sort_name
import ampairsapp.feature.inventory.generated.resources.inv_sort_stock_low_high
import ampairsapp.feature.inventory.generated.resources.inv_sort_updated
import ampairsapp.feature.inventory.generated.resources.inv_sort_value_high_low
import ampairsapp.feature.inventory.generated.resources.inv_synced
import com.ampairs.common.locale.LocalAppLocale
import com.ampairs.common.locale.formatMoney
import com.ampairs.inventory.domain.InventoryItem
import com.ampairs.inventory.ui.components.InventoryEmptyState
import com.ampairs.inventory.ui.components.InventorySkeletonList
import com.ampairs.inventory.ui.components.MonoFamily
import com.ampairs.inventory.ui.components.StockBadge
import com.ampairs.inventory.ui.components.SyncChip
import com.ampairs.inventory.ui.components.color
import com.ampairs.inventory.ui.components.icon
import com.ampairs.inventory.ui.components.SyncChipState
import com.ampairs.inventory.ui.components.formatQty
import com.ampairs.inventory.ui.components.stockStatus
import com.ampairs.inventory.ui.detail.InventoryDetailScreen
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun InventoryPaneScreen(
    onAddItem: () -> Unit,
    onEditItem: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: InventoryListViewModel = metroViewModel(),
) {
    val navigator = rememberListDetailPaneScaffoldNavigator<String>()
    val scope = rememberCoroutineScope()
    val expanded = navigator.scaffoldDirective.maxHorizontalPartitions > 1

    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            AnimatedPane(Modifier) {
                InventoryListPane(
                    viewModel = viewModel,
                    selectedId = navigator.currentDestination?.contentKey,
                    onItemClick = { id ->
                        viewModel.selectItem(id)
                        scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, id) }
                    },
                    onAddItem = onAddItem,
                    onBack = onNavigateBack,
                )
            }
        },
        detailPane = {
            AnimatedPane(Modifier) {
                val id = navigator.currentDestination?.contentKey
                if (id.isNullOrBlank()) {
                    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(Res.string.inv_list_select_prompt),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    InventoryDetailScreen(
                        itemId = id,
                        onEditItem = onEditItem,
                        onBack = { if (navigator.canNavigateBack()) scope.launch { navigator.navigateBack() } },
                        showBackButton = !expanded,
                    )
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InventoryListPane(
    viewModel: InventoryListViewModel,
    selectedId: String?,
    onItemClick: (String) -> Unit,
    onAddItem: () -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val locale = LocalAppLocale.current
    var sortOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.inv_list_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.inv_back))
                    }
                },
                actions = {
                    SyncChip(if (state.isRefreshing) SyncChipState.PENDING else SyncChipState.SYNCED, Modifier.padding(end = 8.dp))
                    Box {
                        IconButton(onClick = { sortOpen = true }) {
                            Icon(Icons.Filled.Sort, contentDescription = stringResource(Res.string.inv_sort))
                        }
                        DropdownMenu(expanded = sortOpen, onDismissRequest = { sortOpen = false }) {
                            SortItem(stringResource(Res.string.inv_sort_name), InventorySort.NAME, viewModel) { sortOpen = false }
                            SortItem(stringResource(Res.string.inv_sort_updated), InventorySort.UPDATED, viewModel) { sortOpen = false }
                            SortItem(stringResource(Res.string.inv_sort_stock_low_high), InventorySort.STOCK_ASC, viewModel) { sortOpen = false }
                            SortItem(stringResource(Res.string.inv_sort_value_high_low), InventorySort.VALUE_DESC, viewModel) { sortOpen = false }
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddItem) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(Res.string.inv_action_add_item))
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                placeholder = { Text(stringResource(Res.string.inv_list_search_placeholder)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )

            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterPill(stringResource(Res.string.inv_filter_all), state.filter == InventoryFilter.ALL) { viewModel.setFilter(InventoryFilter.ALL) }
                FilterPill(stringResource(Res.string.inv_filter_low), state.filter == InventoryFilter.LOW) { viewModel.setFilter(InventoryFilter.LOW) }
                FilterPill(stringResource(Res.string.inv_filter_out), state.filter == InventoryFilter.OUT) { viewModel.setFilter(InventoryFilter.OUT) }
                FilterPill(stringResource(Res.string.inv_filter_inactive), state.filter == InventoryFilter.INACTIVE) { viewModel.setFilter(InventoryFilter.INACTIVE) }
            }

            when {
                state.isLoading && state.items.isEmpty() -> InventorySkeletonList()
                state.items.isEmpty() -> {
                    val title = if (state.filter == InventoryFilter.ALL && state.searchQuery.isBlank())
                        stringResource(Res.string.inv_list_empty_title) else stringResource(Res.string.inv_empty_filtered_title)
                    val desc = if (state.filter == InventoryFilter.ALL && state.searchQuery.isBlank())
                        stringResource(Res.string.inv_list_empty_desc) else null
                    InventoryEmptyState(
                        icon = Icons.Filled.Inventory2,
                        title = title,
                        description = desc,
                        actionLabel = if (desc != null) stringResource(Res.string.inv_action_add_item) else null,
                        onAction = if (desc != null) onAddItem else null,
                    )
                }
                else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)) {
                    items(state.items, key = { it.uid }) { item ->
                        InventoryRow(item, locale, item.uid == selectedId) { onItemClick(item.uid) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SortItem(label: String, sort: InventorySort, viewModel: InventoryListViewModel, onClose: () -> Unit) {
    DropdownMenuItem(text = { Text(label) }, onClick = { viewModel.setSort(sort); onClose() })
}

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

@Composable
private fun InventoryRow(item: InventoryItem, locale: com.ampairs.common.locale.AppLocale, selected: Boolean, onClick: () -> Unit) {
    val container = if (selected) MaterialTheme.colorScheme.secondaryContainer else androidx.compose.ui.graphics.Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(container)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            item.stockStatus().icon(),
            contentDescription = null,
            tint = item.stockStatus().color(),
            modifier = Modifier.size(22.dp),
        )
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(item.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (item.pendingSync) {
                    Icon(Icons.Filled.CloudQueue, contentDescription = stringResource(Res.string.inv_synced), tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(14.dp))
                }
            }
            Text(
                (item.sku?.let { "$it · " } ?: "") + formatMoney(item.sellingPrice, locale),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(formatQty(item.currentStock), style = MaterialTheme.typography.titleMedium, fontFamily = MonoFamily, color = item.stockStatus().color())
            StockBadge(item.stockStatus())
        }
    }
}
