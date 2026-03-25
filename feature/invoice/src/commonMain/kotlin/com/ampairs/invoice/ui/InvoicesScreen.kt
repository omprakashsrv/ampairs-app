package com.ampairs.invoice.ui

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
import com.ampairs.invoice.viewmodel.InvoicesViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InvoicesScreen(onInvoiceSelected: (String) -> Unit) {
    val viewModel: InvoicesViewModel = koinInject<InvoicesViewModel>()

    val lazyListState = rememberLazyListState()
    val invoices = viewModel.invoices.collectAsLazyPagingItems()
    val scope = rememberCoroutineScope()
    val invoicesState = viewModel.invoicesState.value

    Column(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        when (invoicesState) {
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
                invoices.refresh()
                scope.launch {
                    lazyListState.animateScrollToItem(0)
                }
            })

        if (invoices.itemCount > 0) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxHeight()
            ) {
                items(
                    count = invoices.itemCount,
                    key = { index -> invoices[index]?.id ?: index },
                    contentType = { 1 },
                    itemContent = { index ->
                        val invoice = invoices[index]
                        ListItem(
                            shadowElevation = 2.dp,
                            modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null).clickable {
                                onInvoiceSelected(invoice?.id.toString())
                            },
                            headlineContent = {
                                Text(
                                    invoice?.invoice_number.toString(),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            supportingContent = {
                                Text(
                                    invoice?.from_customer_name.toString(),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            trailingContent = {
                                Text(
                                    invoice?.status ?: "",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            })
                    }
                )
            }
        }

    }
}