package com.ampairs.subscription.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ampairs.subscription.domain.model.*

/**
 * Dialog to confirm subscription cancellation
 */
@Composable
fun CancelSubscriptionDialog(
    onDismiss: () -> Unit,
    onConfirm: (immediate: Boolean, reason: String?) -> Unit
) {
    var cancelImmediately by remember { mutableStateOf(false) }
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text("Cancel Subscription")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Are you sure you want to cancel your subscription? " +
                    "You'll lose access to premium features."
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.selectable(
                        selected = cancelImmediately,
                        onClick = { cancelImmediately = !cancelImmediately }
                    )
                ) {
                    Checkbox(
                        checked = cancelImmediately,
                        onCheckedChange = { cancelImmediately = it }
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            "Cancel immediately",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "Otherwise, access continues until period ends",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason (optional)") },
                    placeholder = { Text("Help us improve...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(cancelImmediately, reason.ifBlank { null }) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Cancel Subscription")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Keep Subscription")
            }
        }
    )
}

/**
 * Dialog to pause subscription
 */
@Composable
fun PauseSubscriptionDialog(
    onDismiss: () -> Unit,
    onConfirm: (days: Int, reason: String?) -> Unit
) {
    var pauseDays by remember { mutableStateOf(30) }
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.Pause, contentDescription = null)
        },
        title = {
            Text("Pause Subscription")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Pause your subscription temporarily. " +
                    "You won't be billed during the pause period, " +
                    "but you'll have limited access."
                )

                // Pause duration selector
                Text(
                    "Pause duration",
                    style = MaterialTheme.typography.labelMedium
                )

                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(7 to "1 week", 14 to "2 weeks", 30 to "1 month", 60 to "2 months").forEachIndexed { index, (days, label) ->
                        SegmentedButton(
                            selected = pauseDays == days,
                            onClick = { pauseDays = days },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = 4
                            )
                        ) {
                            Text(label)
                        }
                    }
                }

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason (optional)") },
                    placeholder = { Text("Why are you pausing?") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(pauseDays, reason.ifBlank { null }) }) {
                Text("Pause for $pauseDays days")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Bottom sheet for plan upgrade/change
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpgradeBottomSheet(
    plans: List<SubscriptionPlanDefinition>,
    currentPlanCode: String,
    selectedBillingCycle: BillingCycle,
    onBillingCycleChange: (BillingCycle) -> Unit,
    onDismiss: () -> Unit,
    onSelectPlan: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Choose Your Plan",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            // Billing cycle selector
            Text(
                text = "Billing Cycle",
                style = MaterialTheme.typography.labelMedium
            )

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                BillingCycle.entries.forEachIndexed { index, cycle ->
                    SegmentedButton(
                        selected = selectedBillingCycle == cycle,
                        onClick = { onBillingCycleChange(cycle) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = BillingCycle.entries.size
                        )
                    ) {
                        Column {
                            Text(cycle.displayName)
                            if (cycle.discountPercent > 0) {
                                Text(
                                    "Save ${cycle.discountPercent}%",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            // Plans list
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(plans.sortedBy { it.displayOrder }) { plan ->
                    CompactPlanCard(
                        plan = plan,
                        isCurrentPlan = plan.planCode == currentPlanCode,
                        billingCycle = selectedBillingCycle,
                        onSelect = { onSelectPlan(plan.planCode) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactPlanCard(
    plan: SubscriptionPlanDefinition,
    isCurrentPlan: Boolean,
    billingCycle: BillingCycle,
    onSelect: () -> Unit
) {
    val price = billingCycle.calculateDiscountedPrice(plan.monthlyPriceInr)
    val pricePerMonth = price / billingCycle.months

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (isCurrentPlan) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = plan.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (isCurrentPlan) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                "Current",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
                Text(
                    text = plan.description ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                if (plan.monthlyPriceInr > 0) {
                    Text(
                        text = "₹${pricePerMonth.toLong()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "/month",
                        style = MaterialTheme.typography.labelSmall
                    )
                } else {
                    Text(
                        text = "Free",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            if (!isCurrentPlan) {
                Button(
                    onClick = onSelect,
                    modifier = Modifier.defaultMinSize(minWidth = 80.dp)
                ) {
                    Text("Select")
                }
            }
        }
    }
}

/**
 * Upgrade banner that can be shown contextually
 */
@Composable
fun UpgradeBanner(
    feature: String,
    requiredPlan: SubscriptionPlan,
    onUpgrade: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Upgrade to ${requiredPlan.displayName}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Access $feature and more",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row {
                TextButton(onClick = onDismiss) {
                    Text("Later")
                }
                Button(onClick = onUpgrade) {
                    Text("Upgrade")
                }
            }
        }
    }
}

/**
 * Limit warning banner
 */
@Composable
fun LimitWarningBanner(
    resourceType: String,
    current: Int,
    limit: Int,
    onUpgrade: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isExceeded = current >= limit
    val percentUsed = (current.toFloat() / limit * 100).toInt()

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isExceeded) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.tertiaryContainer
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (isExceeded) Icons.Default.Error else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isExceeded) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onTertiaryContainer
                    }
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (isExceeded) {
                            "${resourceType.replaceFirstChar { it.uppercase() }} limit reached"
                        } else {
                            "Using ${percentUsed}% of $resourceType limit"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "$current of $limit used",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Button(
                onClick = onUpgrade,
                colors = if (isExceeded) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                Text("Upgrade")
            }
        }
    }
}
