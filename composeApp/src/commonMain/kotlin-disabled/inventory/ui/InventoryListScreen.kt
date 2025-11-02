package com.ampairs.inventory.ui

import ampairsapp.composeapp.generated.resources.Res
import ampairsapp.composeapp.generated.resources.ic_add
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import com.ampairs.inventory.viewmodel.InventoryListViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InventoryListScreen(
    onNewInventory: (() -> (Unit))?,
    onInventorySelected: (String, String) -> Unit,
) {
    val viewModel: InventoryListViewModel = koinInject<InventoryListViewModel>()

    val inventories = viewModel.inventories.collectAsLazyPagingItems()
    val lazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        TextField(
            modifier = Modifier.fillMaxWidth(),
            maxLines = 1,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = { Text("Search Inventory") },
            value = viewModel.searchText, onValueChange = {
                viewModel.searchText = it
                inventories.refresh()
                scope.launch {
                    lazyListState.animateScrollToItem(0)
                }
            })

        if (inventories.itemCount > 0) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxHeight()
            ) {
                items(
                    count = inventories.itemCount,
                    key = { index -> inventories[index]?.id ?: index },
                    contentType = { 1 },
                    itemContent = { index ->
                        val customer = inventories[index]
                        ListItem(
                            shadowElevation = 2.dp,
                            modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null)
                                .combinedClickable(
                                    onClick = {
                                        customer?.id?.let {
                                            onInventorySelected(
                                                it,
                                                viewModel.company?.id.toString()
                                            )
                                        }
                                    }
                                ),
                            headlineContent = {
                                Text(
                                    customer?.description.toString(),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            supportingContent = {
                                Text(
                                    customer?.stock.toString(),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            })
                    }
                )
            }
        } else {
            if (onNewInventory != null) {
                IconButton(
                    modifier = Modifier.padding(20.dp).align(Alignment.CenterHorizontally),
                    onClick = {
                        onNewInventory()
                    }) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_add),
                        contentDescription = "Visibility",
                        modifier = Modifier.width(16.dp).height(16.dp)
                    )
                }
            }
        }

    }

}