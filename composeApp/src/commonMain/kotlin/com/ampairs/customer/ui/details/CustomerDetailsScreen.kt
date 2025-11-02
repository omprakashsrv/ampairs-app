package com.ampairs.customer.ui.details

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass
import com.ampairs.customer.domain.Customer
import com.ampairs.customer.ui.components.images.CustomerImageManagementScreen
import com.ampairs.form.data.repository.ConfigRepository
import kotlinx.coroutines.flow.first
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import com.ampairs.customer.util.CustomerConstants.TITLE_CUSTOMER_DETAILS
import com.ampairs.customer.util.CustomerConstants.ERROR_CUSTOMER_NOT_FOUND

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailsScreen(
    customerId: String,
    onNavigateBack: () -> Unit,
    onEditCustomer: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CustomerDetailsViewModel = koinViewModel { parametersOf(customerId) }
) {
    val uiState by viewModel.uiState.collectAsState()
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
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
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
    modifier: Modifier = Modifier
) {
    // Get window size class to determine layout
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val isCompactOrMedium = windowSizeClass.windowWidthSizeClass != WindowWidthSizeClass.EXPANDED

    if (isCompactOrMedium) {
        // Compact/Medium: Use TabLayout for mobile/tablet
        CustomerDetailsTabLayout(customer = customer, modifier = modifier)
    } else {
        // Expanded: Use side-by-side layout for desktop/large screens
        CustomerDetailsSideBySideLayout(customer = customer, modifier = modifier)
    }
}

@Composable
private fun CustomerDetailsTabLayout(
    customer: Customer,
    modifier: Modifier = Modifier
) {
    val configRepository: ConfigRepository = koinInject()
    var selectedTabIndex by remember { mutableStateOf(0) }

    // Load form config for customerImages field
    var imagesFieldConfig by remember { mutableStateOf<com.ampairs.form.domain.EntityFieldConfig?>(null) }

    LaunchedEffect(Unit) {
        val config = configRepository.observeConfigSchema("customer").first()
        imagesFieldConfig = config?.fieldConfigs?.find { it.fieldName == "customerImages" }
    }

    // Filter tabs based on visibility configuration
    val tabs = buildList {
        add("Details")
        if (imagesFieldConfig?.visible != false) {
            add("Images")
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTabIndex) {
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
            1 -> if (tabs.getOrNull(1) == "Images") {
                CustomerImagesTab(
                    customer = customer,
                    readOnly = imagesFieldConfig?.enabled == false,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun CustomerDetailsSideBySideLayout(
    customer: Customer,
    modifier: Modifier = Modifier
) {
    val configRepository: ConfigRepository = koinInject()

    // Load form config for customerImages field
    var imagesFieldConfig by remember { mutableStateOf<com.ampairs.form.domain.EntityFieldConfig?>(null) }

    LaunchedEffect(Unit) {
        val config = configRepository.observeConfigSchema("customer").first()
        imagesFieldConfig = config?.fieldConfigs?.find { it.fieldName == "customerImages" }
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Left side: Customer Details (60% width)
        Card(
            modifier = Modifier
                .weight(0.6f)
                .fillMaxHeight()
        ) {
            CustomerDetailsTab(
                customer = customer,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Right side: Customer Images (40% width) - if visible
        if (imagesFieldConfig?.visible != false) {
            Card(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight()
            ) {
                CustomerImageManagementScreen(
                    customerId = customer.uid,
                    readOnly = imagesFieldConfig?.enabled == false,
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
        // Basic Information
        InfoSection(title = "Basic Information") {
            InfoRow(label = "Name", value = customer.name)
            customer.refId?.let {
                InfoRow(label = "Reference ID", value = it)
            }
            customer.email?.let {
                InfoRow(label = "Email", value = it)
            }
            customer.phone?.let {
                InfoRow(label = "Phone", value = "+${customer.countryCode} $it")
            }
            customer.landline?.let {
                InfoRow(label = "Landline", value = it)
            }
            customer.customerType?.let {
                InfoRow(label = "Type", value = it)
            }
            customer.customerGroup?.let {
                InfoRow(label = "Group", value = it)
            }
            InfoRow(label = "Status", value = if (customer.active) "Active" else "Inactive")
            customer.status?.let {
                InfoRow(label = "Status", value = it)
            }
        }

        // Financial Information
        if (customer.gstNumber != null || customer.panNumber != null || customer.creditLimit != null || customer.creditDays != null) {
            InfoSection(title = "Financial Information") {
                customer.gstNumber?.let {
                    InfoRow(label = "GSTIN", value = it)
                }
                customer.panNumber?.let {
                    InfoRow(label = "PAN Number", value = it)
                }
                customer.creditLimit?.let {
                    InfoRow(label = "Credit Limit", value = "₹${it}")
                }
                customer.creditDays?.let {
                    InfoRow(label = "Credit Days", value = "$it days")
                }
            }
        }

        // Address Information
        if (customer.address != null || customer.street != null || customer.city != null) {
            InfoSection(title = "Address") {
                customer.address?.let {
                    InfoRow(label = "Address", value = it)
                }
                customer.street?.let {
                    InfoRow(label = "Street", value = it)
                }
                customer.street2?.let {
                    InfoRow(label = "Street 2", value = it)
                }
                customer.city?.let {
                    InfoRow(label = "City", value = it)
                }
                customer.state?.let {
                    InfoRow(label = "State", value = it)
                }
                customer.pincode?.let {
                    InfoRow(label = "PIN Code", value = it)
                }
                InfoRow(label = "Country", value = customer.country)
            }
        }

        // Billing Address
        customer.billingAddress?.let { billingAddress ->
            InfoSection(title = "Billing Address") {
                InfoRow(label = "Street", value = billingAddress.street)
                InfoRow(label = "City", value = billingAddress.city)
                InfoRow(label = "State", value = billingAddress.state)
                InfoRow(label = "PIN Code", value = billingAddress.pincode)
                InfoRow(label = "Country", value = billingAddress.country)
            }
        }

        // Shipping Address
        customer.shippingAddress?.let { shippingAddress ->
            InfoSection(title = "Shipping Address") {
                InfoRow(label = "Street", value = shippingAddress.street)
                InfoRow(label = "City", value = shippingAddress.city)
                InfoRow(label = "State", value = shippingAddress.state)
                InfoRow(label = "PIN Code", value = shippingAddress.pincode)
                InfoRow(label = "Country", value = shippingAddress.country)
            }
        }

        // Location Information
        if (customer.latitude != null || customer.longitude != null) {
            InfoSection(title = "Location") {
                customer.latitude?.let { lat ->
                    customer.longitude?.let { lng ->
                        InfoRow(label = "Coordinates", value = "$lat, $lng")
                    }
                }
            }
        }

        // Custom Attributes
        customer.attributes?.takeIf { it.isNotEmpty() }?.let { attrs ->
            InfoSection(title = "Additional Information") {
                attrs.forEach { (key, value) ->
                    InfoRow(label = key, value = value)
                }
            }
        }

        // System Information
        if (customer.createdAt != null || customer.updatedAt != null) {
            InfoSection(title = "System Information") {
                customer.createdAt?.let {
                    InfoRow(label = "Created", value = it)
                }
                customer.updatedAt?.let {
                    InfoRow(label = "Last Updated", value = it)
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
    Card(
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
            text = "Failed to load customer",
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
            Text("Retry")
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
        title = { Text("Delete Customer") },
        text = {
            Text("Are you sure you want to delete $customerName? This action cannot be undone.")
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}