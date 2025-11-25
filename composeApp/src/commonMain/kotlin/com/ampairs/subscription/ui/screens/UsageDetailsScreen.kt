package com.ampairs.subscription.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ampairs.subscription.domain.model.SubscriptionLimits
import com.ampairs.subscription.domain.model.SubscriptionPlan
import com.ampairs.subscription.feature.ResourceStatus
import com.ampairs.subscription.feature.ResourceUsageStatus
import com.ampairs.subscription.feature.UsageStatus
import com.ampairs.subscription.viewmodel.SubscriptionViewModel

/**
 * Screen for viewing detailed usage breakdown
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsageDetailsScreen(
    viewModel: SubscriptionViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPlans: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val subscription by viewModel.subscription.collectAsState()
    val usageStatus by viewModel.usageStatus.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Usage Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // Current plan summary
                item {
                    subscription?.let { sub ->
                        CurrentPlanSummaryCard(
                            planCode = sub.planCode,
                            onUpgrade = onNavigateToPlans
                        )
                    }
                }

                // Usage overview
                item {
                    usageStatus?.let { usage ->
                        UsageOverviewCard(usageStatus = usage)
                    }
                }

                // Detailed usage breakdown
                item {
                    usageStatus?.let { usage ->
                        Text(
                            text = "Resource Usage",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Individual resource cards
                item {
                    usageStatus?.let { usage ->
                        UsageResourceCard(
                            title = "Customers",
                            icon = Icons.Default.People,
                            status = usage.customerStatus,
                            description = "Total customers in your workspace"
                        )
                    }
                }

                item {
                    usageStatus?.let { usage ->
                        UsageResourceCard(
                            title = "Products",
                            icon = Icons.Default.Inventory,
                            status = usage.productStatus,
                            description = "Total products in your catalog"
                        )
                    }
                }

                item {
                    usageStatus?.let { usage ->
                        UsageResourceCard(
                            title = "Invoices",
                            icon = Icons.Default.Receipt,
                            status = usage.invoiceStatus,
                            description = "Invoices created this month"
                        )
                    }
                }

                item {
                    usageStatus?.let { usage ->
                        UsageResourceCard(
                            title = "Devices",
                            icon = Icons.Default.Devices,
                            status = usage.deviceStatus,
                            description = "Registered devices"
                        )
                    }
                }

                item {
                    usageStatus?.let { usage ->
                        UsageResourceCard(
                            title = "Team Members",
                            icon = Icons.Default.Group,
                            status = usage.memberStatus,
                            description = "Members in your workspace"
                        )
                    }
                }

                item {
                    usageStatus?.let { usage ->
                        UsageResourceCard(
                            title = "Storage",
                            icon = Icons.Default.Storage,
                            status = usage.storageStatus,
                            description = "Storage used",
                            suffix = " GB"
                        )
                    }
                }

                // Upgrade prompt if limits are approaching
                item {
                    usageStatus?.let { usage ->
                        if (usage.hasAnyWarning || usage.hasAnyLimitExceeded) {
                            UpgradePromptCard(
                                hasExceeded = usage.hasAnyLimitExceeded,
                                onUpgrade = onNavigateToPlans
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrentPlanSummaryCard(
    planCode: String,
    onUpgrade: () -> Unit
) {
    val plan = SubscriptionPlan.fromCode(planCode)
    val limits = when (plan) {
        SubscriptionPlan.FREE -> SubscriptionLimits.FREE
        SubscriptionPlan.STARTER -> SubscriptionLimits.STARTER
        SubscriptionPlan.PROFESSIONAL -> SubscriptionLimits.PROFESSIONAL
        SubscriptionPlan.ENTERPRISE -> SubscriptionLimits.ENTERPRISE
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Current Plan",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = plan.displayName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (plan != SubscriptionPlan.ENTERPRISE) {
                    FilledTonalButton(onClick = onUpgrade) {
                        Icon(Icons.Default.Upgrade, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Upgrade")
                    }
                }
            }

            // Plan limits summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LimitSummaryItem("Customers", limits.maxCustomers)
                LimitSummaryItem("Products", limits.maxProducts)
                LimitSummaryItem("Invoices", limits.maxInvoicesPerMonth)
            }
        }
    }
}

@Composable
private fun LimitSummaryItem(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = if (value == -1) "∞" else value.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun UsageOverviewCard(usageStatus: UsageStatus) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Usage Overview",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                // Status indicator
                Surface(
                    color = when {
                        usageStatus.hasAnyLimitExceeded -> MaterialTheme.colorScheme.errorContainer
                        usageStatus.hasAnyWarning -> MaterialTheme.colorScheme.tertiaryContainer
                        else -> MaterialTheme.colorScheme.primaryContainer
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = when {
                                usageStatus.hasAnyLimitExceeded -> Icons.Default.Warning
                                usageStatus.hasAnyWarning -> Icons.Default.Info
                                else -> Icons.Default.CheckCircle
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = when {
                                usageStatus.hasAnyLimitExceeded -> "Limits Exceeded"
                                usageStatus.hasAnyWarning -> "Approaching Limits"
                                else -> "Good Standing"
                            },
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            // Quick usage bars
            QuickUsageBar("Customers", usageStatus.customerStatus)
            QuickUsageBar("Products", usageStatus.productStatus)
            QuickUsageBar("Invoices", usageStatus.invoiceStatus)
            QuickUsageBar("Devices", usageStatus.deviceStatus)
        }
    }
}

@Composable
private fun QuickUsageBar(label: String, status: ResourceUsageStatus) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = if (status.status == ResourceStatus.UNLIMITED) {
                    "${status.current} / Unlimited"
                } else {
                    "${status.current} / ${status.limit} (${status.percentUsed}%)"
                },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }

        if (status.status != ResourceStatus.UNLIMITED) {
            LinearProgressIndicator(
                progress = { (status.percentUsed / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = when (status.status) {
                    ResourceStatus.EXCEEDED -> MaterialTheme.colorScheme.error
                    ResourceStatus.WARNING -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.primary
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun UsageResourceCard(
    title: String,
    icon: ImageVector,
    status: ResourceUsageStatus,
    description: String,
    suffix: String = ""
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon with status color
            Surface(
                color = when (status.status) {
                    ResourceStatus.EXCEEDED -> MaterialTheme.colorScheme.errorContainer
                    ResourceStatus.WARNING -> MaterialTheme.colorScheme.tertiaryContainer
                    ResourceStatus.UNLIMITED -> MaterialTheme.colorScheme.secondaryContainer
                    ResourceStatus.OK -> MaterialTheme.colorScheme.primaryContainer
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp),
                    tint = when (status.status) {
                        ResourceStatus.EXCEEDED -> MaterialTheme.colorScheme.onErrorContainer
                        ResourceStatus.WARNING -> MaterialTheme.colorScheme.onTertiaryContainer
                        ResourceStatus.UNLIMITED -> MaterialTheme.colorScheme.onSecondaryContainer
                        ResourceStatus.OK -> MaterialTheme.colorScheme.onPrimaryContainer
                    }
                )
            }

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Progress
                if (status.status != ResourceStatus.UNLIMITED) {
                    LinearProgressIndicator(
                        progress = { (status.percentUsed / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = when (status.status) {
                            ResourceStatus.EXCEEDED -> MaterialTheme.colorScheme.error
                            ResourceStatus.WARNING -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.primary
                        },
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }

            // Usage numbers
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${status.current}$suffix",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (status.status == ResourceStatus.UNLIMITED) {
                        "Unlimited"
                    } else {
                        "of ${status.limit}$suffix"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun UpgradePromptCard(
    hasExceeded: Boolean,
    onUpgrade: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (hasExceeded) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (hasExceeded) Icons.Default.Warning else Icons.Default.Info,
                    contentDescription = null,
                    tint = if (hasExceeded) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.tertiary
                    }
                )
                Text(
                    text = if (hasExceeded) {
                        "You've exceeded some limits"
                    } else {
                        "You're approaching your limits"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = if (hasExceeded) {
                    "Upgrade your plan to continue creating new resources and unlock more features."
                } else {
                    "Consider upgrading your plan to avoid interruptions and get more resources."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = onUpgrade,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Upgrade, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("View Upgrade Options")
            }
        }
    }
}
