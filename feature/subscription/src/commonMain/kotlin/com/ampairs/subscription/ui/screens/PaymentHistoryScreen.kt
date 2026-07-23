package com.ampairs.subscription.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ampairs.common.navigation.ScreenBackButton
import com.ampairs.subscription.domain.model.*
import com.ampairs.subscription.viewmodel.SubscriptionViewModel

/**
 * Screen for viewing payment/billing history
 *
 * Note: This screen requires the following backend API:
 * GET /api/v1/subscription/payments - Returns paginated payment history
 *
 * See: SUBSCRIPTION_BACKEND_API_REQUIREMENTS.md for full API specification
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentHistoryScreen(
    viewModel: SubscriptionViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val subscription by viewModel.subscription.collectAsStateWithLifecycle()
    val payments by viewModel.paymentHistory.collectAsStateWithLifecycle()
    val isLoadingPayments by viewModel.isLoadingPayments.collectAsStateWithLifecycle()
    val hasMorePayments by viewModel.hasMorePayments.collectAsStateWithLifecycle()

    // Load payment history on first composition
    LaunchedEffect(Unit) {
        viewModel.loadPaymentHistory(page = 0, refresh = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Billing History") },
                navigationIcon = {
                    ScreenBackButton(onClick = onNavigateBack, contentDescription = "Back")
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshPaymentHistory() },
                        enabled = !isLoadingPayments
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        if (isLoadingPayments && payments.isEmpty()) {
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
                // Current billing summary
                item {
                    subscription?.let { sub ->
                        BillingSummaryCard(subscription = sub)
                    }
                }

                // Payment history header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Payment History",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (payments.isEmpty()) {
                    // Empty state
                    item {
                        EmptyPaymentHistoryCard()
                    }
                } else {
                    // Payment transactions
                    items(payments) { payment ->
                        PaymentTransactionCard(payment = payment)
                    }

                    // Load more button
                    if (hasMorePayments) {
                        item {
                            Button(
                                onClick = { viewModel.loadMorePayments() },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isLoadingPayments
                            ) {
                                if (isLoadingPayments) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(if (isLoadingPayments) "Loading..." else "Load More")
                            }
                        }
                    }
                }

                // Info card about payment history
                item {
                    PaymentInfoCard()
                }
            }
        }
    }
}

@Composable
private fun BillingSummaryCard(subscription: SubscriptionState) {
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
                        text = SubscriptionPlan.fromCode(subscription.planCode).displayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Status chip
                Surface(
                    color = when (subscription.status) {
                        SubscriptionStatus.ACTIVE -> MaterialTheme.colorScheme.primaryContainer
                        SubscriptionStatus.TRIALING -> MaterialTheme.colorScheme.secondaryContainer
                        SubscriptionStatus.PAST_DUE -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = subscription.status.displayName,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            if (!subscription.isFree) {
                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Billing Cycle",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = subscription.billingCycle.displayName,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    subscription.nextBillingAmount?.let { amount ->
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Next Bill",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatAmount(amount, subscription.currency),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    subscription.currentPeriodEnd?.let { periodEnd ->
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Next Payment",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatDate(periodEnd),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyPaymentHistoryCard() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Receipt,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "No Payment History",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Your payment transactions will appear here once you subscribe to a paid plan.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PaymentTransactionCard(payment: PaymentTransaction) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Status icon
            Surface(
                color = when (payment.status) {
                    PaymentStatus.SUCCEEDED -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                    PaymentStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
                    PaymentStatus.PENDING -> MaterialTheme.colorScheme.tertiaryContainer
                    PaymentStatus.REFUNDED -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = when (payment.status) {
                        PaymentStatus.SUCCEEDED -> Icons.Default.CheckCircle
                        PaymentStatus.FAILED -> Icons.Default.Error
                        PaymentStatus.PENDING -> Icons.Default.Schedule
                        PaymentStatus.REFUNDED -> Icons.Default.Undo
                        else -> Icons.Default.Receipt
                    },
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp),
                    tint = when (payment.status) {
                        PaymentStatus.SUCCEEDED -> Color(0xFF4CAF50)
                        PaymentStatus.FAILED -> MaterialTheme.colorScheme.error
                        PaymentStatus.PENDING -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = payment.description ?: "Subscription Payment",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = payment.getPaymentMethodDisplay(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                payment.paidAt?.let {
                    Text(
                        text = formatDate(it),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Amount
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatAmount(payment.amount, payment.currency),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when (payment.status) {
                        PaymentStatus.REFUNDED -> MaterialTheme.colorScheme.tertiary
                        PaymentStatus.FAILED -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
                Surface(
                    color = when (payment.status) {
                        PaymentStatus.SUCCEEDED -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                        PaymentStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
                        PaymentStatus.PENDING -> MaterialTheme.colorScheme.tertiaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = getPaymentStatusDisplayName(payment.status),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentInfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Payment Information",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "All payments are processed securely through our payment partners. Receipts are sent to your registered email address.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "For billing inquiries, contact support@ampairs.com",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun formatAmount(amount: Double, currency: String): String {
    val symbol = when (currency.uppercase()) {
        "INR" -> "₹"
        "USD" -> "$"
        "EUR" -> "€"
        "GBP" -> "£"
        else -> currency
    }
    // Simple formatting for KMP compatibility
    val wholePart = amount.toLong()
    val decimalPart = ((amount - wholePart) * 100).toInt()
    return "$symbol$wholePart.${decimalPart.toString().padStart(2, '0')}"
}

private fun formatDate(isoDate: String): String {
    // Simple date formatting - in production use proper date library
    return try {
        val parts = isoDate.take(10).split("-")
        if (parts.size == 3) {
            "${parts[2]}/${parts[1]}/${parts[0]}"
        } else {
            isoDate.take(10)
        }
    } catch (e: Exception) {
        isoDate.take(10)
    }
}

private fun getPaymentStatusDisplayName(status: PaymentStatus): String {
    return when (status) {
        PaymentStatus.PENDING -> "Pending"
        PaymentStatus.PROCESSING -> "Processing"
        PaymentStatus.SUCCEEDED -> "Succeeded"
        PaymentStatus.FAILED -> "Failed"
        PaymentStatus.REFUNDED -> "Refunded"
        PaymentStatus.DISPUTED -> "Disputed"
        PaymentStatus.CANCELLED -> "Cancelled"
    }
}
