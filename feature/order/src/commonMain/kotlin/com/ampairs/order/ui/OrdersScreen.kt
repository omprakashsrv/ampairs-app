package com.ampairs.order.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import com.ampairs.common.model.UiState
import com.ampairs.order.viewmodel.OrdersViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OrdersScreen(onOrderSelected: (String) -> Unit) {

    val viewModel: OrdersViewModel = koinInject<OrdersViewModel>()

    val lazyListState = rememberLazyListState()
    val orders = viewModel.orders.collectAsLazyPagingItems()
    val scope = rememberCoroutineScope()
    val ordersState = viewModel.ordersState.value

    Column(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        when (ordersState) {
            UiState.Empty -> {}
            is UiState.Error -> {}
            is UiState.Loading -> {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            is UiState.Success -> {}
        }
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            maxLines = 1,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = { Text("Search") },
            value = viewModel.searchText, onValueChange = {
                viewModel.searchText = it
                orders.refresh()
                scope.launch {
                    lazyListState.animateScrollToItem(0)
                }
            })

        if (orders.itemCount > 0) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxHeight()
            ) {
                items(
                    count = orders.itemCount,
                    key = { index -> orders[index]?.id ?: index },
                    contentType = { 1 },
                    itemContent = { index ->
                        val order = orders[index]
                        ListItem(
                            shadowElevation = 2.dp,
                            modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null)
                                .clickable {
                                    onOrderSelected(order?.id.toString())
                                },
                            headlineContent = {
                                Text(
                                    order?.orderNumber.toString(),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            supportingContent = {
                                Text(
                                    order?.fromCustomerName.toString(),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            trailingContent = {
                                Text(
                                    order?.status ?: "",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            })
                    }
                )
            }
        }

    }
}