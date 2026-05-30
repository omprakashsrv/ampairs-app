package com.ampairs.customer.ui.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass
import com.ampairs.common.util.DateTimeFormatter
import com.ampairs.customer.domain.Customer
import com.ampairs.customer.ui.components.images.CustomerImageManagementScreen
import com.ampairs.customer.ui.components.images.CustomerImageViewModel
import com.ampairs.customer.util.CustomerConstants.ERROR_CUSTOMER_NOT_FOUND
import com.ampairs.customer.util.CustomerConstants.TITLE_CUSTOMER_DETAILS
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import ampairsapp.feature.customer.generated.resources.Res
import ampairsapp.feature.customer.generated.resources.customer_edit
import ampairsapp.feature.customer.generated.resources.customer_delete
import ampairsapp.feature.customer.generated.resources.customer_tab_details
import ampairsapp.feature.customer.generated.resources.customer_tab_images
import ampairsapp.feature.customer.generated.resources.customer_section_basic
import ampairsapp.feature.customer.generated.resources.customer_section_financial
import ampairsapp.feature.customer.generated.resources.customer_section_address
import ampairsapp.feature.customer.generated.resources.customer_section_billing
import ampairsapp.feature.customer.generated.resources.customer_section_shipping
import ampairsapp.feature.customer.generated.resources.customer_section_location
import ampairsapp.feature.customer.generated.resources.customer_section_additional
import ampairsapp.feature.customer.generated.resources.customer_section_system
import ampairsapp.feature.customer.generated.resources.customer_label_name
import ampairsapp.feature.customer.generated.resources.customer_label_ref_id
import ampairsapp.feature.customer.generated.resources.customer_label_email
import ampairsapp.feature.customer.generated.resources.customer_label_phone
import ampairsapp.feature.customer.generated.resources.customer_label_landline
import ampairsapp.feature.customer.generated.resources.customer_label_type
import ampairsapp.feature.customer.generated.resources.customer_label_group
import ampairsapp.feature.customer.generated.resources.customer_label_gstin
import ampairsapp.feature.customer.generated.resources.customer_label_pan
import ampairsapp.feature.customer.generated.resources.customer_label_credit_limit
import ampairsapp.feature.customer.generated.resources.customer_label_credit_days
import ampairsapp.feature.customer.generated.resources.customer_label_address
import ampairsapp.feature.customer.generated.resources.customer_label_street
import ampairsapp.feature.customer.generated.resources.customer_label_street2
import ampairsapp.feature.customer.generated.resources.customer_label_city
import ampairsapp.feature.customer.generated.resources.customer_label_state
import ampairsapp.feature.customer.generated.resources.customer_label_pincode
import ampairsapp.feature.customer.generated.resources.customer_label_country
import ampairsapp.feature.customer.generated.resources.customer_label_coordinates
import ampairsapp.feature.customer.generated.resources.customer_label_created
import ampairsapp.feature.customer.generated.resources.customer_label_updated
import ampairsapp.feature.customer.generated.resources.customer_status_active_label
import ampairsapp.feature.customer.generated.resources.customer_status_inactive_label
import ampairsapp.feature.customer.generated.resources.customer_section_status
import ampairsapp.feature.customer.generated.resources.customer_details_load_error
import ampairsapp.feature.customer.generated.resources.customer_retry
import ampairsapp.feature.customer.generated.resources.customer_delete_title
import ampairsapp.feature.customer.generated.resources.customer_delete_confirm
import ampairsapp.feature.customer.generated.resources.customer_cancel
import ampairsapp.feature.customer.generated.resources.customer_credit_days_value
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailsScreen(
    customerId: String,
    onNavigateBack: () -> Unit,
    onEditCustomer: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CustomerDetailsViewModel = assistedMetroViewModel<CustomerDetailsViewModel, CustomerDetailsViewModel.Factory> { create(customerId) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val imagesConfig by viewModel.imagesConfig.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(customerId) {
        viewModel.loadCustomer()
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(uiState.customer?.name ?: TITLE_CUSTOMER_DETAILS) },
            actions = {
                if (uiState.customer != null) {
                    IconButton(onClick = { onEditCustomer(customerId) }) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(Res.string.customer_edit))
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.customer_delete))
                    }
                }
            }
        )

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
                val errorMessage = uiState.error ?: return@Column
                ErrorMessage(
                    error = errorMessage,
                    onRetry = viewModel::loadCustomer,
                    modifier = Modifier.fillMaxSize()
                )
            }

            uiState.customer != null -> {
                val currentCustomer = uiState.customer ?: return@Column
                CustomerDetailsContent(
                    customer = currentCustomer,
                    showCustomerImages = imagesConfig.visible,
                    customerImagesReadOnly = imagesConfig.readOnly,
                    modifier = Modifier.fillMaxSize()
                )
            }

            else -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(ERROR_CUSTOMER_NOT_FOUND)
                }
            }
        }
    }

    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            customerName = uiState.customer?.name ?: "",
            onConfirm = {
                showDeleteDialog = false
                viewModel.deleteCustomer {
                    onNavigateBack()
                }
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

@Composable
private fun CustomerDetailsContent(
    customer: Customer,
    showCustomerImages: Boolean,
    customerImagesReadOnly: Boolean,
    modifier: Modifier = Modifier
) {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val isCompactOrMedium = !windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)

    if (isCompactOrMedium) {
        CustomerDetailsTabLayout(
            customer = customer,
            showCustomerImages = showCustomerImages,
            customerImagesReadOnly = customerImagesReadOnly,
            modifier = modifier
        )
    } else {
        CustomerDetailsSideBySideLayout(
            customer = customer,
            showCustomerImages = showCustomerImages,
            customerImagesReadOnly = customerImagesReadOnly,
            modifier = modifier
        )
    }
}

@Composable
private fun CustomerDetailsTabLayout(
    customer: Customer,
    showCustomerImages: Boolean,
    customerImagesReadOnly: Boolean,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableStateOf(0) }

    val tabs = buildList {
        add(stringResource(Res.string.customer_tab_details))
        if (showCustomerImages) add(stringResource(Res.string.customer_tab_images))
    }

    Column(modifier = modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) }
                )
            }
        }

        when (selectedTabIndex) {
            0 -> CustomerDetailsTab(customer = customer, modifier = Modifier.fillMaxSize())
            1 -> if (tabs.size > 1) {
                CustomerImagesTab(
                    customer = customer,
                    readOnly = customerImagesReadOnly,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun CustomerDetailsSideBySideLayout(
    customer: Customer,
    showCustomerImages: Boolean,
    customerImagesReadOnly: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedCard(
            modifier = Modifier
                .weight(0.6f)
                .fillMaxHeight()
        ) {
            CustomerDetailsTab(
                customer = customer,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (showCustomerImages) {
            OutlinedCard(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight()
            ) {
                CustomerImageManagementScreen(
                    customerId = customer.uid,
                    readOnly = customerImagesReadOnly,
                    viewModel = assistedMetroViewModel<CustomerImageViewModel, CustomerImageViewModel.Factory> { create(customer.uid) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun CustomerImagesTab(
    customer: Customer,
    readOnly: Boolean = false,
    modifier: Modifier = Modifier
) {
    CustomerImageManagementScreen(
        customerId = customer.uid,
        readOnly = readOnly,
        viewModel = assistedMetroViewModel<CustomerImageViewModel, CustomerImageViewModel.Factory> { create(customer.uid) },
        modifier = modifier.padding(16.dp)
    )
}

@Composable
private fun CustomerDetailsTab(
    customer: Customer,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        InfoSection(title = stringResource(Res.string.customer_section_basic)) {
            InfoRow(label = stringResource(Res.string.customer_label_name), value = customer.name)
            customer.refId?.let {
                InfoRow(label = stringResource(Res.string.customer_label_ref_id), value = it)
            }
            customer.email?.let {
                InfoRow(label = stringResource(Res.string.customer_label_email), value = it)
            }
            customer.phone?.let {
                InfoRow(label = stringResource(Res.string.customer_label_phone), value = "+${customer.countryCode} $it")
            }
            customer.landline?.let {
                InfoRow(label = stringResource(Res.string.customer_label_landline), value = it)
            }
            customer.customerType?.let {
                InfoRow(label = stringResource(Res.string.customer_label_type), value = it)
            }
            customer.customerGroup?.let {
                InfoRow(label = stringResource(Res.string.customer_label_group), value = it)
            }
            InfoRow(
                label = stringResource(Res.string.customer_section_status),
                value = if (customer.active) stringResource(Res.string.customer_status_active_label)
                        else stringResource(Res.string.customer_status_inactive_label)
            )
            customer.status?.let {
                InfoRow(label = stringResource(Res.string.customer_section_status), value = it)
            }
        }

        if (customer.gstNumber != null || customer.panNumber != null || customer.creditLimit != null || customer.creditDays != null) {
            InfoSection(title = stringResource(Res.string.customer_section_financial)) {
                customer.gstNumber?.let {
                    InfoRow(label = stringResource(Res.string.customer_label_gstin), value = it)
                }
                customer.panNumber?.let {
                    InfoRow(label = stringResource(Res.string.customer_label_pan), value = it)
                }
                customer.creditLimit?.let {
                    InfoRow(label = stringResource(Res.string.customer_label_credit_limit), value = "₹$it")
                }
                customer.creditDays?.let {
                    InfoRow(label = stringResource(Res.string.customer_label_credit_days), value = stringResource(Res.string.customer_credit_days_value, it))
                }
            }
        }

        if (customer.address != null || customer.street != null || customer.city != null) {
            InfoSection(title = stringResource(Res.string.customer_section_address)) {
                customer.address?.let {
                    InfoRow(label = stringResource(Res.string.customer_label_address), value = it)
                }
                customer.street?.let {
                    InfoRow(label = stringResource(Res.string.customer_label_street), value = it)
                }
                customer.street2?.let {
                    InfoRow(label = stringResource(Res.string.customer_label_street2), value = it)
                }
                customer.city?.let {
                    InfoRow(label = stringResource(Res.string.customer_label_city), value = it)
                }
                customer.state?.let {
                    InfoRow(label = stringResource(Res.string.customer_label_state), value = it)
                }
                customer.pincode?.let {
                    InfoRow(label = stringResource(Res.string.customer_label_pincode), value = it)
                }
                InfoRow(label = stringResource(Res.string.customer_label_country), value = customer.country)
            }
        }

        customer.billingAddress?.let { billingAddress ->
            InfoSection(title = stringResource(Res.string.customer_section_billing)) {
                InfoRow(label = stringResource(Res.string.customer_label_street), value = billingAddress.street)
                InfoRow(label = stringResource(Res.string.customer_label_city), value = billingAddress.city)
                InfoRow(label = stringResource(Res.string.customer_label_state), value = billingAddress.state)
                InfoRow(label = stringResource(Res.string.customer_label_pincode), value = billingAddress.pincode)
                InfoRow(label = stringResource(Res.string.customer_label_country), value = billingAddress.country)
            }
        }

        customer.shippingAddress?.let { shippingAddress ->
            InfoSection(title = stringResource(Res.string.customer_section_shipping)) {
                InfoRow(label = stringResource(Res.string.customer_label_street), value = shippingAddress.street)
                InfoRow(label = stringResource(Res.string.customer_label_city), value = shippingAddress.city)
                InfoRow(label = stringResource(Res.string.customer_label_state), value = shippingAddress.state)
                InfoRow(label = stringResource(Res.string.customer_label_pincode), value = shippingAddress.pincode)
                InfoRow(label = stringResource(Res.string.customer_label_country), value = shippingAddress.country)
            }
        }

        if (customer.latitude != null || customer.longitude != null) {
            InfoSection(title = stringResource(Res.string.customer_section_location)) {
                customer.latitude?.let { lat ->
                    customer.longitude?.let { lng ->
                        InfoRow(label = stringResource(Res.string.customer_label_coordinates), value = "$lat, $lng")
                    }
                }
            }
        }

        customer.attributes?.takeIf { it.isNotEmpty() }?.let { attrs ->
            InfoSection(title = stringResource(Res.string.customer_section_additional)) {
                attrs.forEach { (key, value) ->
                    InfoRow(label = key, value = value)
                }
            }
        }

        if (customer.createdAt != null || customer.updatedAt != null) {
            InfoSection(title = stringResource(Res.string.customer_section_system)) {
                customer.createdAt?.let {
                    InfoRow(label = stringResource(Res.string.customer_label_created), value = DateTimeFormatter.formatTimestamp(it))
                }
                customer.updatedAt?.let {
                    InfoRow(label = stringResource(Res.string.customer_label_updated), value = DateTimeFormatter.formatTimestamp(it))
                }
            }
        }
    }
}

@Composable
private fun InfoSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(2f)
        )
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
            text = stringResource(Res.string.customer_details_load_error),
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

@Composable
private fun DeleteConfirmationDialog(
    customerName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.customer_delete_title)) },
        text = {
            Text(stringResource(Res.string.customer_delete_confirm, customerName))
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(Res.string.customer_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.customer_cancel))
            }
        }
    )
}
