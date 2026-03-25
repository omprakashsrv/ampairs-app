package com.ampairs.subscription.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ampairs.subscription.domain.model.*
import com.ampairs.subscription.feature.ResourceStatus
import com.ampairs.subscription.feature.UsageStatus

/**
 * Card showing current subscription status
 */
@Composable
fun CurrentSubscriptionCard(
    subscription: SubscriptionState,
    onUpgrade: () -> Unit,
    onManage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val planColor = when (SubscriptionPlan.fromCode(subscription.planCode)) {
        SubscriptionPlan.FREE -> MaterialTheme.colorScheme.outline
        SubscriptionPlan.STARTER -> MaterialTheme.colorScheme.primary
        SubscriptionPlan.PROFESSIONAL -> MaterialTheme.colorScheme.secondary
        SubscriptionPlan.ENTERPRISE -> MaterialTheme.colorScheme.tertiary
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
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
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = planColor
                    )
                }

                SubscriptionStatusChip(status = subscription.status)
            }

            // Trial or billing info
            if (subscription.isInTrial()) {
                subscription.trialEndsAt?.let { trialEnd ->
                    TrialInfoRow(
                        daysRemaining = subscription.daysRemaining,
                        trialEndsAt = trialEnd
                    )
                }
            } else if (!subscription.isFree) {
                BillingInfoRow(
                    billingCycle = subscription.billingCycle,
                    nextBillingAmount = subscription.nextBillingAmount,
                    currency = subscription.currency,
                    periodEnd = subscription.currentPeriodEnd
                )
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (subscription.planCode != "ENTERPRISE") {
                    Button(
                        onClick = onUpgrade,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Upgrade, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Upgrade")
                    }
                }
                OutlinedButton(
                    onClick = onManage,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Manage")
                }
            }
        }
    }
}

@Composable
fun SubscriptionStatusChip(
    status: SubscriptionStatus,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = when (status) {
        SubscriptionStatus.ACTIVE -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        SubscriptionStatus.TRIALING -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        SubscriptionStatus.PAST_DUE -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        SubscriptionStatus.PAUSED -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        SubscriptionStatus.CANCELLED, SubscriptionStatus.EXPIRED -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.surface to MaterialTheme.colorScheme.onSurface
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor
    ) {
        Text(
            text = status.displayName,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = textColor
        )
    }
}

@Composable
private fun TrialInfoRow(
    daysRemaining: Long,
    trialEndsAt: String
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Timer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Trial Period",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = "$daysRemaining days remaining",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun BillingInfoRow(
    billingCycle: BillingCycle,
    nextBillingAmount: Double?,
    currency: String,
    periodEnd: String?
) {
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
                text = billingCycle.displayName,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        nextBillingAmount?.let { amount ->
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Next Bill",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatAmount(amount, currency),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Usage summary card
 */
@Composable
fun UsageSummaryCard(
    usageStatus: UsageStatus,
    onViewDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
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
                    text = "Usage Summary",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                if (usageStatus.hasAnyWarning || usageStatus.hasAnyLimitExceeded) {
                    Icon(
                        if (usageStatus.hasAnyLimitExceeded) Icons.Default.Warning else Icons.Default.Info,
                        contentDescription = null,
                        tint = if (usageStatus.hasAnyLimitExceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            // Usage bars
            UsageProgressRow("Customers", usageStatus.customerStatus)
            UsageProgressRow("Products", usageStatus.productStatus)
            UsageProgressRow("Invoices", usageStatus.invoiceStatus)
            UsageProgressRow("Devices", usageStatus.deviceStatus)

            TextButton(
                onClick = onViewDetails,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("View Details")
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
        }
    }
}

@Composable
private fun UsageProgressRow(
    label: String,
    status: com.ampairs.subscription.feature.ResourceUsageStatus
) {
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
                    "${status.current} / ${status.limit}"
                },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }

        if (status.status != ResourceStatus.UNLIMITED) {
            LinearProgressIndicator(
                progress = { (status.percentUsed / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(4.dp),
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

/**
 * Plan selection card
 */
@Composable
fun PlanCard(
    plan: SubscriptionPlanDefinition,
    isCurrentPlan: Boolean,
    selectedBillingCycle: BillingCycle,
    onSelectPlan: () -> Unit,
    onStartTrial: () -> Unit,
    modifier: Modifier = Modifier
) {
    val price = selectedBillingCycle.calculateDiscountedPrice(plan.monthlyPriceInr)
    val pricePerMonth = price / selectedBillingCycle.months

    Card(
        modifier = modifier.fillMaxWidth(),
        border = if (isCurrentPlan) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
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
                        text = plan.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    plan.description?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (plan.monthlyPriceInr > 0) {
                    // Use PriceWithDiscounts component to show final price after all discounts
                    PriceWithDiscounts(
                        plan = plan,
                        workspaceCount = 1, // TODO: Get from ViewModel
                        currency = "INR"
                    )
                } else {
                    Text(
                        text = "Free",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Pre-launch badge (MOST prominent - shown first)
            if (plan.preLaunchDiscount.isActive) {
                PreLaunchBadge(discount = plan.preLaunchDiscount)
            }

            // Seasonal discount badge
            if (plan.seasonalDiscount.isActive) {
                SeasonalDiscountBadge(seasonalDiscount = plan.seasonalDiscount)
            }

            // Multi-workspace discount badge
            if (plan.multiWorkspaceDiscount.isAvailable) {
                DiscountBadge(discount = plan.multiWorkspaceDiscount)
            }

            // Discount breakdown
            if (plan.hasAnyDiscount(1)) { // Pass workspace count from viewModel
                DiscountBreakdownCard(
                    plan = plan,
                    workspaceCount = 1 // This should come from ViewModel
                )
            }

            // Key features
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FeatureRow("${formatLimit(plan.limits.maxCustomers)} customers")
                FeatureRow("${formatLimit(plan.limits.maxProducts)} products")
                FeatureRow("${formatLimit(plan.limits.maxInvoicesPerMonth)} invoices/month")
                FeatureRow("${formatLimit(plan.limits.maxDevices)} devices")
                if (plan.features.apiAccessEnabled) {
                    FeatureRow("API access")
                }
                if (plan.features.prioritySupport) {
                    FeatureRow("Priority support")
                }
            }

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (plan.trialDays > 0 && !isCurrentPlan) {
                    OutlinedButton(
                        onClick = onStartTrial,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Try ${plan.trialDays} days free")
                    }
                }

                if (!isCurrentPlan) {
                    Button(
                        onClick = onSelectPlan,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (plan.monthlyPriceInr > 0) "Subscribe" else "Downgrade")
                    }
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Current Plan",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureRow(feature: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Default.Check,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = feature,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

private fun formatLimit(value: Int): String {
    return if (value == -1) "Unlimited" else value.toString()
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
