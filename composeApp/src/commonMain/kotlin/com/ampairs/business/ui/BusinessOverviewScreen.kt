package com.ampairs.business.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ampairs.form.data.repository.ConfigRepository
import com.ampairs.form.domain.EntityType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Business Overview Screen - Dashboard with key business information.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessOverviewScreen(
    onNavigateToProfile: () -> Unit = {},
    onNavigateToOperations: () -> Unit = {},
    onNavigateToTax: () -> Unit = {},
    onNavigateToCustomAttributes: () -> Unit = {},
    onNavigateToFormConfig: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: BusinessOverviewViewModel = koinInject(),
    configRepository: ConfigRepository = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()
    var isRefreshing by remember { mutableStateOf(false) }

    // Check if custom attributes exist for business entity
    var hasCustomAttributes by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val config = configRepository.observeConfigSchema(EntityType.BUSINESS).first()
            hasCustomAttributes = config?.attributeDefinitions?.any { it.visible } ?: false
        }
    }

    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading) {
            isRefreshing = false
        }
    }

    PullToRefreshBox(
        modifier = modifier.fillMaxSize(),
        state = pullRefreshState,
        isRefreshing = isRefreshing,
        onRefresh = {
            if (!isRefreshing) {
                isRefreshing = true
                viewModel.refresh()
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Business Overview",
                style = MaterialTheme.typography.headlineMedium
            )

            when {
                uiState.isLoading && uiState.overview == null -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.error != null -> {
                    val errorText = uiState.error ?: "Unknown error"

                    // Show create profile option if business doesn't exist
                    if (errorText.contains("not found", ignoreCase = true)) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.Business,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "No Business Profile Found",
                                        style = MaterialTheme.typography.titleLarge,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Text(
                                        text = "Create your business profile to get started with managing your business operations, settings, and compliance.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Button(
                                        onClick = onNavigateToProfile,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Create Business Profile")
                                    }
                                }
                            }
                        }
                    } else {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorText,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }

                uiState.overview != null -> {
                    val overview = uiState.overview!!

                    // Business Info Card
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(overview.name, style = MaterialTheme.typography.headlineSmall)
                            Text("Type: ${overview.businessType}", style = MaterialTheme.typography.bodyMedium)
                            if (overview.email != null) {
                                Text("Email: ${overview.email}", style = MaterialTheme.typography.bodyMedium)
                            }
                            if (overview.phone != null) {
                                Text("Phone: ${overview.phone}", style = MaterialTheme.typography.bodyMedium)
                            }
                            if (overview.address.isNotBlank()) {
                                Text("Address: ${overview.address}", style = MaterialTheme.typography.bodyMedium)
                            }
                            Text("Currency: ${overview.currency} | Timezone: ${overview.timezone}", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    // Quick Actions
                    Text("Quick Actions", style = MaterialTheme.typography.titleMedium)

                    OutlinedCard(modifier = Modifier.fillMaxWidth(), onClick = onNavigateToProfile) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Business, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text("Profile & Registration", style = MaterialTheme.typography.titleMedium)
                                Text("Company details and registration info", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    OutlinedCard(modifier = Modifier.fillMaxWidth(), onClick = onNavigateToOperations) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text("Operations", style = MaterialTheme.typography.titleMedium)
                                Text("Timezone, currency, business hours", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    OutlinedCard(modifier = Modifier.fillMaxWidth(), onClick = onNavigateToTax) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text("Tax Configuration", style = MaterialTheme.typography.titleMedium)
                                Text("GST/VAT and tax compliance", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    // Custom Attributes Section - Only show if custom attributes are defined
                    if (hasCustomAttributes) {
                        OutlinedCard(modifier = Modifier.fillMaxWidth(), onClick = onNavigateToCustomAttributes) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Extension, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Column {
                                    Text("Custom Attributes", style = MaterialTheme.typography.titleMedium)
                                    Text("Additional business information and custom fields", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    OutlinedCard(modifier = Modifier.fillMaxWidth(), onClick = onNavigateToFormConfig) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text("Form Configuration", style = MaterialTheme.typography.titleMedium)
                                Text("Customize business form fields and attributes", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
