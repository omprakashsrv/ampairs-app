package com.ampairs.subscription.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ampairs.common.navigation.ScreenBackButton
import com.ampairs.subscription.domain.model.*
import com.ampairs.subscription.viewmodel.SubscriptionEvent
import com.ampairs.subscription.viewmodel.SubscriptionViewModel
import kotlinx.coroutines.flow.collectLatest

/**
 * Screen for comparing subscription plans and selecting a new plan
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanComparisonScreen(
    viewModel: SubscriptionViewModel,
    onNavigateBack: () -> Unit,
    onCheckoutUrl: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val subscription by viewModel.subscription.collectAsStateWithLifecycle()
    val plans by viewModel.plans.collectAsStateWithLifecycle()

    var selectedBillingCycle by remember { mutableStateOf(BillingCycle.ANNUAL) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var selectedPlanForChange by remember { mutableStateOf<SubscriptionPlanDefinition?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Handle events
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is SubscriptionEvent.TrialStarted -> {
                    snackbarHostState.showSnackbar("Trial started for ${event.planCode}")
                }
                is SubscriptionEvent.CheckoutReady -> {
                    event.response.checkoutUrl?.let { onCheckoutUrl(it) }
                }
                is SubscriptionEvent.PlanChanged -> {
                    snackbarHostState.showSnackbar("Plan changed successfully!")
                    onNavigateBack()
                }
                is SubscriptionEvent.Error -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                else -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Compare Plans") },
                navigationIcon = {
                    ScreenBackButton(onClick = onNavigateBack, contentDescription = "Back")
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                // Billing cycle selector
                item {
                    BillingCycleSelector(
                        selectedCycle = selectedBillingCycle,
                        onCycleSelected = { selectedBillingCycle = it }
                    )
                }

                // Current plan indicator
                item {
                    subscription?.let { sub ->
                        CurrentPlanIndicator(
                            planCode = sub.planCode,
                            status = sub.status
                        )
                    }
                }

                // Plans comparison
                items(plans.sortedBy { it.displayOrder }) { plan ->
                    val isCurrentPlan = plan.planCode == subscription?.planCode
                    PlanComparisonCard(
                        plan = plan,
                        isCurrentPlan = isCurrentPlan,
                        selectedBillingCycle = selectedBillingCycle,
                        onSelectPlan = {
                            if (!isCurrentPlan) {
                                selectedPlanForChange = plan
                                showConfirmDialog = true
                            }
                        },
                        onStartTrial = {
                            if (plan.trialDays > 0 && !isCurrentPlan) {
                                viewModel.startTrial(plan.planCode, plan.trialDays)
                            }
                        }
                    )
                }

                // Feature comparison table
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    FeatureComparisonSection(plans = plans)
                }
            }
        }
    }

    // Confirm plan change dialog
    if (showConfirmDialog && selectedPlanForChange != null) {
        val plan = selectedPlanForChange!!
        val price = selectedBillingCycle.calculateDiscountedPrice(plan.monthlyPriceInr)

        AlertDialog(
            onDismissRequest = {
                showConfirmDialog = false
                selectedPlanForChange = null
            },
            title = { Text("Confirm Plan Change") },
            text = {
                Column {
                    Text("Are you sure you want to change to ${plan.displayName}?")
                    Spacer(modifier = Modifier.height(8.dp))
                    if (plan.monthlyPriceInr > 0) {
                        Text(
                            text = "Amount: ${formatAmount(price, "INR")} (${selectedBillingCycle.displayName})",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.changePlan(plan.planCode, selectedBillingCycle, false)
                        showConfirmDialog = false
                        selectedPlanForChange = null
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        selectedPlanForChange = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Processing overlay
    if (uiState.isProcessing) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Text("Processing...")
                }
            }
        }
    }
}

@Composable
private fun BillingCycleSelector(
    selectedCycle: BillingCycle,
    onCycleSelected: (BillingCycle) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Billing Cycle",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BillingCycle.entries.forEach { cycle ->
                    FilterChip(
                        selected = cycle == selectedCycle,
                        onClick = { onCycleSelected(cycle) },
                        label = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(cycle.displayName)
                                if (cycle.discountPercent > 0) {
                                    Text(
                                        text = "Save ${cycle.discountPercent}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CurrentPlanIndicator(
    planCode: String,
    status: SubscriptionStatus
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Current Plan: ${SubscriptionPlan.fromCode(planCode).displayName}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.weight(1f))
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = status.displayName,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun PlanComparisonCard(
    plan: SubscriptionPlanDefinition,
    isCurrentPlan: Boolean,
    selectedBillingCycle: BillingCycle,
    onSelectPlan: () -> Unit,
    onStartTrial: () -> Unit
) {
    val price = selectedBillingCycle.calculateDiscountedPrice(plan.monthlyPriceInr)
    val pricePerMonth = if (selectedBillingCycle.months > 0) price / selectedBillingCycle.months else price

    Card(
        modifier = Modifier.fillMaxWidth(),
        border = if (isCurrentPlan) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = plan.displayName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (isCurrentPlan) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = "Current",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                    plan.description?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    if (plan.monthlyPriceInr > 0) {
                        Text(
                            text = formatAmount(pricePerMonth, "INR"),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "/month",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "Free",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            HorizontalDivider()

            // Limits
            Text(
                text = "Limits",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            LimitRow("Customers", plan.limits.maxCustomers)
            LimitRow("Products", plan.limits.maxProducts)
            LimitRow("Invoices/month", plan.limits.maxInvoicesPerMonth)
            LimitRow("Devices", plan.limits.maxDevices)
            LimitRow("Members", plan.limits.maxMembersPerWorkspace)
            LimitRow("Storage", plan.limits.maxStorageGb, suffix = " GB")

            HorizontalDivider()

            // Features
            Text(
                text = "Features",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            FeatureCheckRow("API Access", plan.features.apiAccessEnabled)
            FeatureCheckRow("Custom Branding", plan.features.customBrandingEnabled)
            FeatureCheckRow("SSO", plan.features.ssoEnabled)
            FeatureCheckRow("Audit Logs", plan.features.auditLogsEnabled)
            FeatureCheckRow("Priority Support", plan.features.prioritySupport)

            // Actions
            if (!isCurrentPlan) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (plan.trialDays > 0) {
                        OutlinedButton(
                            onClick = onStartTrial,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Try ${plan.trialDays} days free")
                        }
                    }
                    Button(
                        onClick = onSelectPlan,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (plan.monthlyPriceInr > 0) "Subscribe" else "Select")
                    }
                }
            }
        }
    }
}

@Composable
private fun LimitRow(label: String, value: Int, suffix: String = "") {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = if (value == -1) "Unlimited" else "$value$suffix",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun FeatureCheckRow(label: String, enabled: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Icon(
            imageVector = if (enabled) Icons.Default.Check else Icons.Default.Close,
            contentDescription = null,
            tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun FeatureComparisonSection(plans: List<SubscriptionPlanDefinition>) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Feature Comparison",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Header row
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Feature",
                    modifier = Modifier.weight(2f),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                plans.sortedBy { it.displayOrder }.forEach { plan ->
                    Text(
                        text = plan.displayName.take(4),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            HorizontalDivider()

            // Feature rows
            ComparisonRow("API Access", plans) { it.features.apiAccessEnabled }
            ComparisonRow("Branding", plans) { it.features.customBrandingEnabled }
            ComparisonRow("SSO", plans) { it.features.ssoEnabled }
            ComparisonRow("Audit Logs", plans) { it.features.auditLogsEnabled }
            ComparisonRow("Priority Support", plans) { it.features.prioritySupport }
        }
    }
}

@Composable
private fun ComparisonRow(
    label: String,
    plans: List<SubscriptionPlanDefinition>,
    getValue: (SubscriptionPlanDefinition) -> Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(2f),
            style = MaterialTheme.typography.bodySmall
        )
        plans.sortedBy { it.displayOrder }.forEach { plan ->
            Icon(
                imageVector = if (getValue(plan)) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                tint = if (getValue(plan)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                modifier = Modifier.weight(1f).size(16.dp)
            )
        }
    }
}

private fun formatAmount(amount: Double, currency: String): String {
    val symbol = when (currency.uppercase()) {
        "INR" -> "₹"
        "USD" -> "$"
        "EUR" -> "€"
        else -> currency
    }
    return "$symbol${amount.toLong()}"
}
