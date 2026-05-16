package com.ampairs.subscription.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ampairs.subscription.domain.model.*
import com.ampairs.subscription.ui.components.*
import com.ampairs.subscription.viewmodel.SubscriptionEvent
import com.ampairs.subscription.viewmodel.SubscriptionViewModel
import com.ampairs.subscription.viewmodel.InvoiceViewModel
import com.ampairs.common.util.formatCurrencyWithCode
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.koinInject
import androidx.compose.foundation.clickable

/**
 * Main subscription management screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    viewModel: SubscriptionViewModel,
    onNavigateToPlanComparison: () -> Unit,
    onNavigateToBillingHistory: () -> Unit,
    onNavigateToDeviceManagement: () -> Unit,
    onNavigateToUsageDetails: () -> Unit,
    onNavigateToInvoices: (() -> Unit)? = null,
    onNavigateToInvoiceDetail: ((String) -> Unit)? = null,
    onCheckoutUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
    invoiceViewModel: InvoiceViewModel = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()
    val subscription by viewModel.subscription.collectAsState()
    val plans by viewModel.plans.collectAsState()
    val usageStatus by viewModel.usageStatus.collectAsState()
    val currentPlan by viewModel.currentPlan.collectAsState()
    val activeSeasonalDiscount by viewModel.activeSeasonalDiscount.collectAsState()
    val activePreLaunchDiscount by viewModel.activePreLaunchDiscount.collectAsState()
    val isNewUser by viewModel.isNewUser.collectAsState()

    // Invoice states
    val invoiceSummary by invoiceViewModel.invoiceSummary.collectAsState()
    val unpaidInvoices by invoiceViewModel.invoices.collectAsState()

    var showCancelDialog by remember { mutableStateOf(false) }
    var showUpgradeSheet by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Load invoice summary on screen entry
    LaunchedEffect(Unit) {
        invoiceViewModel.loadInvoiceSummary()
        // Load only overdue and pending invoices for display
        invoiceViewModel.loadInvoices(status = InvoiceStatus.OVERDUE)
    }

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
                is SubscriptionEvent.PurchaseVerified -> {
                    snackbarHostState.showSnackbar("Subscription activated!")
                }
                is SubscriptionEvent.PlanChanged -> {
                    snackbarHostState.showSnackbar("Plan changed successfully")
                }
                is SubscriptionEvent.SubscriptionCancelled -> {
                    snackbarHostState.showSnackbar(
                        if (event.immediate) "Subscription cancelled" else "Subscription will cancel at period end"
                    )
                }
                is SubscriptionEvent.Error -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                else -> {}
            }
        }
    }

    Scaffold(
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
                // Pre-Launch Banner (HIGHEST PRIORITY - shown first if eligible)
                item {
                    activePreLaunchDiscount?.let { preLaunchDiscount ->
                        if (preLaunchDiscount.isEligible(isNewUser)) {
                            PreLaunchHeroBanner(discount = preLaunchDiscount)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                // Seasonal Discount Banner (if active and no pre-launch)
                item {
                    if (activePreLaunchDiscount == null || !activePreLaunchDiscount!!.isEligible(isNewUser)) {
                        SeasonalDiscountBanner(seasonalDiscount = activeSeasonalDiscount)
                    }
                }

                // Current Subscription Card
                item {
                    subscription?.let { sub ->
                        CurrentSubscriptionCard(
                            subscription = sub,
                            onUpgrade = { showUpgradeSheet = true },
                            onManage = onNavigateToPlanComparison
                        )
                    }
                }

                // Unpaid Invoices Alert (if any)
                item {
                    if (invoiceSummary != null && (invoiceSummary!!.overdueInvoices > 0 || invoiceSummary!!.pendingInvoices > 0)) {
                        UnpaidInvoicesCard(
                            summary = invoiceSummary!!,
                            onViewInvoices = onNavigateToInvoices,
                            onViewInvoiceDetail = onNavigateToInvoiceDetail
                        )
                    }
                }

                // Usage Summary
                item {
                    usageStatus?.let { usage ->
                        UsageSummaryCard(
                            usageStatus = usage,
                            onViewDetails = onNavigateToUsageDetails
                        )
                    }
                }

                // Quick Actions
                item {
                    QuickActionsSection(
                        subscription = subscription,
                        onViewPlans = onNavigateToPlanComparison,
                        onBillingHistory = onNavigateToBillingHistory,
                        onInvoices = onNavigateToInvoices,
                        onManageDevices = onNavigateToDeviceManagement,
                        onCancel = { showCancelDialog = true }
                    )
                }

                // Available Plans
                if (plans.isNotEmpty()) {
                    item {
                        Text(
                            text = "Available Plans",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(plans.filter { it.planCode != subscription?.planCode }) { plan ->
                        PlanCard(
                            plan = plan,
                            isCurrentPlan = plan.planCode == subscription?.planCode,
                            selectedBillingCycle = uiState.selectedBillingCycle,
                            onSelectPlan = {
                                viewModel.selectPlan(plan.planCode)
                                showUpgradeSheet = true
                            },
                            onStartTrial = {
                                if (plan.trialDays > 0) {
                                    viewModel.startTrial(plan.planCode, plan.trialDays)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Cancel Dialog
    if (showCancelDialog) {
        CancelSubscriptionDialog(
            onDismiss = { showCancelDialog = false },
            onConfirm = { immediate, reason ->
                viewModel.cancelSubscription(immediate, reason)
                showCancelDialog = false
            }
        )
    }

    // Upgrade Bottom Sheet
    if (showUpgradeSheet) {
        UpgradeBottomSheet(
            plans = plans,
            currentPlanCode = subscription?.planCode ?: "FREE",
            selectedBillingCycle = uiState.selectedBillingCycle,
            onBillingCycleChange = { viewModel.selectBillingCycle(it) },
            onDismiss = { showUpgradeSheet = false },
            onSelectPlan = { planCode ->
                viewModel.changePlan(planCode, uiState.selectedBillingCycle, false)
                showUpgradeSheet = false
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
private fun QuickActionsSection(
    subscription: SubscriptionState?,
    onViewPlans: () -> Unit,
    onBillingHistory: () -> Unit,
    onInvoices: (() -> Unit)? = null,
    onManageDevices: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionButton(
                    icon = Icons.Default.CompareArrows,
                    label = "Compare Plans",
                    onClick = onViewPlans,
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    icon = Icons.Default.Receipt,
                    label = "Billing",
                    onClick = onBillingHistory,
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    icon = Icons.Default.Devices,
                    label = "Devices",
                    onClick = onManageDevices,
                    modifier = Modifier.weight(1f)
                )
            }

            // Second row with invoices button
            onInvoices?.let {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionButton(
                        icon = Icons.Default.Description,
                        label = "Invoices",
                        onClick = it,
                        modifier = Modifier.weight(1f)
                    )
                    // Placeholder buttons to maintain layout
                    Spacer(modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            // Subscription management actions
            if (subscription != null && !subscription.isFree &&
                (subscription.status == SubscriptionStatus.ACTIVE || subscription.status == SubscriptionStatus.TRIALING)) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Cancel, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Cancel Subscription")
                }
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

/**
 * Card displaying unpaid invoices alert
 */
@Composable
private fun UnpaidInvoicesCard(
    summary: InvoiceSummary,
    onViewInvoices: (() -> Unit)? = null,
    onViewInvoiceDetail: ((String) -> Unit)? = null
) {
    val hasOverdue = summary.overdueInvoices > 0
    val totalUnpaid = summary.overdueInvoices + summary.pendingInvoices

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (hasOverdue)
                MaterialTheme.colorScheme.errorContainer
            else
                MaterialTheme.colorScheme.secondaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (hasOverdue) Icons.Default.Warning else Icons.Default.Info,
                        contentDescription = null,
                        tint = if (hasOverdue)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (hasOverdue) "Overdue Invoices" else "Pending Invoices",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (hasOverdue)
                            MaterialTheme.colorScheme.onErrorContainer
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "$totalUnpaid",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Message
            Text(
                text = if (hasOverdue) {
                    "You have ${summary.overdueInvoices} overdue invoice${if (summary.overdueInvoices > 1) "s" else ""}. " +
                    "Please pay to avoid service interruption."
                } else {
                    "You have ${summary.pendingInvoices} pending invoice${if (summary.pendingInvoices > 1) "s" else ""}. " +
                    "Payment due soon."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (hasOverdue)
                    MaterialTheme.colorScheme.onErrorContainer
                else
                    MaterialTheme.colorScheme.onSurface
            )

            HorizontalDivider()

            // Amount and next due date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total Outstanding",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (hasOverdue)
                            MaterialTheme.colorScheme.onErrorContainer
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = formatCurrencyWithCode(summary.totalOutstanding, "INR"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (hasOverdue)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary
                    )
                }

                summary.nextDueDate?.let { dueDate ->
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Next Due",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (hasOverdue)
                                MaterialTheme.colorScheme.onErrorContainer
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = dueDate.substringBefore("T"),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Action button
            Button(
                onClick = { onViewInvoices?.invoke() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (hasOverdue)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Description, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (hasOverdue) "Pay Now" else "View Invoices")
            }
        }
    }
}
