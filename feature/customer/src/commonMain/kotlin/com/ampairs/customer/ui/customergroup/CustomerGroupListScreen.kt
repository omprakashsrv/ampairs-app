package com.ampairs.customer.ui.customergroup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ampairs.customer.domain.CustomerGroup
import dev.zacsweers.metrox.viewmodel.metroViewModel
import ampairsapp.feature.customer.generated.resources.Res
import ampairsapp.feature.customer.generated.resources.customer_active
import ampairsapp.feature.customer.generated.resources.customer_inactive
import ampairsapp.feature.customer.generated.resources.customer_delete
import ampairsapp.feature.customer.generated.resources.customer_edit
import ampairsapp.feature.customer.generated.resources.customer_error
import ampairsapp.feature.customer.generated.resources.customer_no_data_found
import ampairsapp.feature.customer.generated.resources.customer_unknown_error
import ampairsapp.feature.customer.generated.resources.customer_search_cd
import ampairsapp.feature.customer.generated.resources.customer_code_display
import ampairsapp.feature.customer.generated.resources.customer_group_list_title
import ampairsapp.feature.customer.generated.resources.customer_group_add_cd
import ampairsapp.feature.customer.generated.resources.customer_group_search_label
import ampairsapp.feature.customer.generated.resources.customer_group_search_hint
import ampairsapp.feature.customer.generated.resources.customer_group_no_data_desc
import ampairsapp.feature.customer.generated.resources.customer_group_create
import ampairsapp.feature.customer.generated.resources.customer_group_search_empty
import ampairsapp.feature.customer.generated.resources.customer_group_list_empty
import ampairsapp.feature.customer.generated.resources.customer_group_benefits
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerGroupListScreen(
    onCustomerGroupClick: (String) -> Unit,
    onAddCustomerGroup: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CustomerGroupListViewModel = metroViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showSearchBar by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.syncCustomerGroups()
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(Res.string.customer_group_list_title)) },
            actions = {
                IconButton(onClick = { showSearchBar = !showSearchBar }) {
                    Icon(Icons.Default.Search, contentDescription = stringResource(Res.string.customer_search_cd))
                }
                IconButton(onClick = onAddCustomerGroup) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.customer_group_add_cd))
                }
            }
        )

        if (showSearchBar) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                label = { Text(stringResource(Res.string.customer_group_search_label)) },
                placeholder = { Text(stringResource(Res.string.customer_group_search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

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

                uiState.error != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = if (uiState.error?.contains("CUSTOMER_NOT_FOUND") == true)
                                        stringResource(Res.string.customer_no_data_found)
                                    else
                                        stringResource(Res.string.customer_error),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (uiState.error?.contains("CUSTOMER_NOT_FOUND") == true)
                                        stringResource(Res.string.customer_group_no_data_desc)
                                    else
                                        uiState.error ?: stringResource(Res.string.customer_unknown_error),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }

                        if (uiState.error?.contains("CUSTOMER_NOT_FOUND") == true && uiState.searchQuery.isBlank()) {
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = onAddCustomerGroup,
                                modifier = Modifier.fillMaxWidth(0.8f)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(Res.string.customer_group_create))
                            }
                        }
                    }
                }

                uiState.customerGroups.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (uiState.searchQuery.isNotBlank())
                                stringResource(Res.string.customer_group_search_empty)
                            else
                                stringResource(Res.string.customer_group_list_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (uiState.searchQuery.isBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onAddCustomerGroup,
                                modifier = Modifier.fillMaxWidth(0.8f)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(Res.string.customer_group_create))
                            }
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.customerGroups) { customerGroup ->
                            CustomerGroupCard(
                                customerGroup = customerGroup,
                                onClick = { onCustomerGroupClick(customerGroup.uid) },
                                onDelete = { viewModel.deleteCustomerGroup(customerGroup.uid) }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.BottomEnd
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                FloatingActionButton(onClick = onAddCustomerGroup) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.customer_group_add_cd))
                }
            }
        }
    }
}

@Composable
private fun CustomerGroupCard(
    customerGroup: CustomerGroup,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = customerGroup.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    customerGroup.groupCode?.let { code ->
                        Text(
                            text = stringResource(Res.string.customer_code_display, code),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    customerGroup.description?.let { desc ->
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onClick, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = stringResource(Res.string.customer_edit),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(Res.string.customer_delete),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (customerGroup.defaultDiscountPercentage != null || customerGroup.priorityLevel != null) {
                    Column {
                        Text(
                            text = stringResource(Res.string.customer_group_benefits),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val benefitInfo = buildString {
                            customerGroup.defaultDiscountPercentage?.let { discount -> append("${discount}% discount") }
                            if (customerGroup.defaultDiscountPercentage != null && customerGroup.priorityLevel != null) append(" • ")
                            customerGroup.priorityLevel?.let { priority -> append("Priority $priority") }
                        }
                        Text(text = benefitInfo, style = MaterialTheme.typography.bodySmall)
                    }
                }

                AssistChip(
                    onClick = { },
                    label = {
                        Text(
                            text = if (customerGroup.active) stringResource(Res.string.customer_active)
                                   else stringResource(Res.string.customer_inactive),
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (customerGroup.active) MaterialTheme.colorScheme.primaryContainer
                                         else MaterialTheme.colorScheme.errorContainer,
                        labelColor = if (customerGroup.active) MaterialTheme.colorScheme.onPrimaryContainer
                                     else MaterialTheme.colorScheme.onErrorContainer
                    )
                )
            }
        }
    }
}
