package com.ampairs.customer.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.customer.domain.CustomerListItem
import dev.zacsweers.metrox.viewmodel.metroViewModel
import com.ampairs.customer.util.CustomerConstants.TITLE_CUSTOMERS
import ampairsapp.feature.customer.generated.resources.Res
import ampairsapp.feature.customer.generated.resources.customer_form_settings_cd
import ampairsapp.feature.customer.generated.resources.customer_list_refresh_cd
import ampairsapp.feature.customer.generated.resources.customer_list_add_cd
import ampairsapp.feature.customer.generated.resources.customer_list_add_btn
import ampairsapp.feature.customer.generated.resources.customer_list_refreshing
import ampairsapp.feature.customer.generated.resources.customer_list_empty
import ampairsapp.feature.customer.generated.resources.customer_list_empty_desc
import ampairsapp.feature.customer.generated.resources.customer_list_error_title
import ampairsapp.feature.customer.generated.resources.customer_list_search_hint
import ampairsapp.feature.customer.generated.resources.customer_retry
import ampairsapp.feature.customer.generated.resources.customer_image_content_desc
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersListScreen(
    onCustomerClick: (String) -> Unit,
    onCreateCustomer: () -> Unit,
    onFormConfig: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: CustomersListViewModel = metroViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.syncCustomers()
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(TITLE_CUSTOMERS) },
            actions = {
                IconButton(onClick = onFormConfig) {
                    Icon(Icons.Default.Settings, contentDescription = stringResource(Res.string.customer_form_settings_cd))
                }
                IconButton(
                    onClick = viewModel::syncCustomers,
                    enabled = !uiState.isRefreshing
                ) {
                    if (uiState.isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(Res.string.customer_list_refresh_cd))
                    }
                }
                IconButton(onClick = onCreateCustomer) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.customer_list_add_cd))
                }
            }
        )

        SearchBar(
            query = uiState.searchQuery,
            onQueryChange = viewModel::updateSearchQuery,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )

        when {
            uiState.isLoading && uiState.customers.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.error != null && uiState.customers.isEmpty() -> {
                val errorMessage = uiState.error ?: return@Column
                ErrorMessage(
                    error = errorMessage,
                    onRetry = viewModel::loadCustomers,
                    modifier = Modifier.fillMaxSize()
                )
            }

            uiState.customers.isEmpty() -> {
                EmptyState(
                    onCreateCustomer = onCreateCustomer,
                    modifier = Modifier.fillMaxSize()
                )
            }

            else -> {
                CustomersList(
                    customers = uiState.customers,
                    onCustomerClick = onCustomerClick,
                    isRefreshing = uiState.isRefreshing,
                    modifier = Modifier.fillMaxSize()
                )
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
        placeholder = { Text(stringResource(Res.string.customer_list_search_hint)) },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = stringResource(Res.string.customer_form_settings_cd))
        },
        modifier = modifier,
        singleLine = true
    )
}

@Composable
private fun CustomersList(
    customers: List<CustomerListItem>,
    onCustomerClick: (String) -> Unit,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (isRefreshing) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = stringResource(Res.string.customer_list_refreshing),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        items(customers, key = { it.id }) { customer ->
            CustomerCard(
                customer = customer,
                onClick = { onCustomerClick(customer.id) }
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomerCard(
    customer: CustomerListItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                if (!customer.primaryThumbnailUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ApiUrlBuilder.buildCompleteUrl(customer.primaryThumbnailUrl!!),
                        contentDescription = stringResource(Res.string.customer_image_content_desc),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = customer.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                if (customer.phone != null) {
                    Text(
                        text = customer.phone!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (customer.city != null) {
                    Text(
                        text = customer.city!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(
    onCreateCustomer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(Res.string.customer_list_empty),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = stringResource(Res.string.customer_list_empty_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onCreateCustomer) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(Res.string.customer_list_add_btn))
        }
    }
}

@Composable
private fun ErrorMessage(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(Res.string.customer_list_error_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error
        )

        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onRetry) {
            Text(stringResource(Res.string.customer_retry))
        }
    }
}
