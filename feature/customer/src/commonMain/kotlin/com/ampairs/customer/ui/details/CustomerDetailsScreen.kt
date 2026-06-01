package com.ampairs.customer.ui.details

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass
import com.ampairs.common.util.DateTimeFormatter
import com.ampairs.customer.domain.Customer
import com.ampairs.customer.ui.components.images.CustomerImageManagementScreen
import com.ampairs.customer.ui.components.images.CustomerImageViewModel
import com.ampairs.customer.ui.list.CustomerAvatar
import com.ampairs.customer.util.CustomerConstants.ERROR_CUSTOMER_NOT_FOUND
import com.ampairs.customer.util.CustomerConstants.TITLE_CUSTOMER_DETAILS
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import ampairsapp.feature.customer.generated.resources.Res
import ampairsapp.feature.customer.generated.resources.customer_edit
import ampairsapp.feature.customer.generated.resources.customer_delete
import ampairsapp.feature.customer.generated.resources.customer_tab_details
import ampairsapp.feature.customer.generated.resources.customer_tab_images
import ampairsapp.feature.customer.generated.resources.customer_tab_overview
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
import ampairsapp.feature.customer.generated.resources.customer_action_bill
import ampairsapp.feature.customer.generated.resources.customer_action_call
import ampairsapp.feature.customer.generated.resources.customer_details_credit_limit_stat
import ampairsapp.feature.customer.generated.resources.customer_details_credit_days_stat
import ampairsapp.feature.customer.generated.resources.customer_details_no_limit
import ampairsapp.feature.customer.generated.resources.customer_details_na
import ampairsapp.feature.customer.generated.resources.customer_details_days_suffix
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

    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val isExpanded = windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)

    LaunchedEffect(customerId) {
        viewModel.loadCustomer()
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(uiState.customer?.name ?: TITLE_CUSTOMER_DETAILS) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null)
                }
            },
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
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            uiState.error != null -> {
                ErrorMessage(
                    error = uiState.error!!,
                    onRetry = viewModel::loadCustomer,
                    modifier = Modifier.fillMaxSize()
                )
            }

            uiState.customer != null -> {
                val customer = uiState.customer!!
                if (isExpanded) {
                    CustomerDetailsExpanded(
                        customer = customer,
                        showImages = imagesConfig.visible,
                        imagesReadOnly = imagesConfig.readOnly,
                        onEdit = { onEditCustomer(customerId) },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    CustomerDetailsMobile(
                        customer = customer,
                        showImages = imagesConfig.visible,
                        imagesReadOnly = imagesConfig.readOnly,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            else -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                viewModel.deleteCustomer { onNavigateBack() }
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

// ─── Mobile layout ────────────────────────────────────────────────────────────

@Composable
private fun CustomerDetailsMobile(
    customer: Customer,
    showImages: Boolean,
    imagesReadOnly: Boolean,
    modifier: Modifier = Modifier
) {
    val tabs = buildList {
        add(stringResource(Res.string.customer_tab_overview))
        if (showImages) add(stringResource(Res.string.customer_tab_images))
    }
    var selectedTab by remember { mutableStateOf(0) }

    Column(modifier = modifier) {
        CustomerHeroSection(customer = customer)

        if (tabs.size > 1) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
        }

        when (selectedTab) {
            0 -> CustomerOverviewTab(customer = customer, modifier = Modifier.fillMaxSize())
            1 -> if (showImages) {
                CustomerImageManagementScreen(
                    customerId = customer.uid,
                    readOnly = imagesReadOnly,
                    viewModel = assistedMetroViewModel<CustomerImageViewModel, CustomerImageViewModel.Factory> { create(customer.uid) },
                    modifier = Modifier.fillMaxSize().padding(16.dp)
                )
            }
        }
    }
}

// ─── Desktop/expanded layout ──────────────────────────────────────────────────

@Composable
private fun CustomerDetailsExpanded(
    customer: Customer,
    showImages: Boolean,
    imagesReadOnly: Boolean,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = buildList {
        add(stringResource(Res.string.customer_tab_overview))
        if (showImages) add(stringResource(Res.string.customer_tab_images))
    }
    var selectedTab by remember { mutableStateOf(0) }

    Row(modifier = modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        // Left panel: hero + stats
        OutlinedCard(modifier = Modifier.width(280.dp).fillMaxHeight()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CustomerAvatar(
                    name = customer.name,
                    imageUrl = null,
                    size = 88
                )
                Text(
                    text = customer.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                customer.phone?.let {
                    Text(
                        text = "+${customer.countryCode} $it",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                customer.customerType?.let {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        label = stringResource(Res.string.customer_details_credit_limit_stat),
                        value = customer.creditLimit?.let { "₹$it" } ?: stringResource(Res.string.customer_details_no_limit),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = stringResource(Res.string.customer_details_credit_days_stat),
                        value = customer.creditDays?.let { "${it}${stringResource(Res.string.customer_details_days_suffix)}" }
                            ?: stringResource(Res.string.customer_details_na),
                        modifier = Modifier.weight(1f)
                    )
                }
                StatCard(
                    label = if (customer.active) stringResource(Res.string.customer_status_active_label)
                            else stringResource(Res.string.customer_status_inactive_label),
                    value = if (customer.active) "Active" else "Inactive",
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider()

                Button(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(Res.string.customer_edit))
                }
            }
        }

        // Right panel: tabs
        OutlinedCard(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (tabs.size > 1) {
                    PrimaryTabRow(selectedTabIndex = selectedTab) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = { Text(title) }
                            )
                        }
                    }
                }
                when (selectedTab) {
                    0 -> CustomerOverviewTab(customer = customer, modifier = Modifier.fillMaxSize())
                    1 -> if (showImages) {
                        CustomerImageManagementScreen(
                            customerId = customer.uid,
                            readOnly = imagesReadOnly,
                            viewModel = assistedMetroViewModel<CustomerImageViewModel, CustomerImageViewModel.Factory> { create(customer.uid) },
                            modifier = Modifier.fillMaxSize().padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─── Hero section (mobile) ────────────────────────────────────────────────────

@Composable
private fun CustomerHeroSection(
    customer: Customer,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                CustomerAvatar(
                    name = customer.name,
                    imageUrl = null,
                    size = 64
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = customer.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    customer.phone?.let {
                        Text(
                            text = "+${customer.countryCode} $it",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    customer.customerType?.let {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // Stats row
            if (customer.creditLimit != null || customer.creditDays != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    customer.creditLimit?.let {
                        StatCard(
                            label = stringResource(Res.string.customer_details_credit_limit_stat),
                            value = "₹$it",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    customer.creditDays?.let {
                        StatCard(
                            label = stringResource(Res.string.customer_details_credit_days_stat),
                            value = "${it}${stringResource(Res.string.customer_details_days_suffix)}",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Quick actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = {},
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp)
                ) {
                    Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(Res.string.customer_action_bill), style = MaterialTheme.typography.labelMedium)
                }
                FilledTonalButton(
                    onClick = {},
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(Res.string.customer_action_call), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

// ─── Stat card ────────────────────────────────────────────────────────────────

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─── Tab content ──────────────────────────────────────────────────────────────

@Composable
private fun CustomerOverviewTab(
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
                customer.address?.let { InfoRow(label = stringResource(Res.string.customer_label_address), value = it) }
                customer.street?.let { InfoRow(label = stringResource(Res.string.customer_label_street), value = it) }
                customer.street2?.let { InfoRow(label = stringResource(Res.string.customer_label_street2), value = it) }
                customer.city?.let { InfoRow(label = stringResource(Res.string.customer_label_city), value = it) }
                customer.state?.let { InfoRow(label = stringResource(Res.string.customer_label_state), value = it) }
                customer.pincode?.let { InfoRow(label = stringResource(Res.string.customer_label_pincode), value = it) }
                InfoRow(label = stringResource(Res.string.customer_label_country), value = customer.country)
            }
        }

        customer.billingAddress?.let { billing ->
            InfoSection(title = stringResource(Res.string.customer_section_billing)) {
                InfoRow(label = stringResource(Res.string.customer_label_street), value = billing.street ?: "")
                InfoRow(label = stringResource(Res.string.customer_label_city), value = billing.city ?: "")
                InfoRow(label = stringResource(Res.string.customer_label_state), value = billing.state ?: "")
                InfoRow(label = stringResource(Res.string.customer_label_pincode), value = billing.pincode ?: "")
                InfoRow(label = stringResource(Res.string.customer_label_country), value = billing.country ?: "")
            }
        }

        customer.shippingAddress?.let { shipping ->
            InfoSection(title = stringResource(Res.string.customer_section_shipping)) {
                InfoRow(label = stringResource(Res.string.customer_label_street), value = shipping.street ?: "")
                InfoRow(label = stringResource(Res.string.customer_label_city), value = shipping.city ?: "")
                InfoRow(label = stringResource(Res.string.customer_label_state), value = shipping.state ?: "")
                InfoRow(label = stringResource(Res.string.customer_label_pincode), value = shipping.pincode ?: "")
                InfoRow(label = stringResource(Res.string.customer_label_country), value = shipping.country ?: "")
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
                attrs.forEach { (key, value) -> InfoRow(label = key, value = value) }
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

// ─── Info components ──────────────────────────────────────────────────────────

@Composable
private fun InfoSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
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

// ─── Dialogs & error states ───────────────────────────────────────────────────

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
        Text(text = error, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) { Text(stringResource(Res.string.customer_retry)) }
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
        text = { Text(stringResource(Res.string.customer_delete_confirm, customerName)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(Res.string.customer_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.customer_cancel)) }
        }
    )
}
