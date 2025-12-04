package com.ampairs.subscription.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import com.ampairs.common.util.formatDecimal
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ampairs.subscription.domain.model.*
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.minutes

/**
 * Badge showing discount percentage
 */
@Composable
fun DiscountBadge(
    discount: MultiWorkspaceDiscount,
    modifier: Modifier = Modifier
) {
    if (!discount.isAvailable) return

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LocalOffer,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = "${discount.discountPercent}% OFF for ${discount.minWorkspaces}+ workspaces",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

/**
 * Pricing calculator showing breakdown with discount
 */
@Composable
fun PricingCalculator(
    basePrice: Double,
    workspaceCount: Int,
    discount: MultiWorkspaceDiscount,
    currency: String = "₹",
    modifier: Modifier = Modifier
) {
    val hasDiscount = workspaceCount >= discount.minWorkspaces && discount.isAvailable

    val discountedPrice = if (hasDiscount) {
        basePrice * (100 - discount.discountPercent) / 100
    } else {
        basePrice
    }

    val totalPrice = discountedPrice * workspaceCount
    val savings = if (hasDiscount) {
        (basePrice - discountedPrice) * workspaceCount
    } else {
        0.0
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Your Price Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Divider(modifier = Modifier.padding(vertical = 4.dp))

            // Workspace count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$workspaceCount workspace(s)",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "$currency${basePrice.formatDecimal()} each",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Discount applied
            if (hasDiscount) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Multi-workspace discount (${discount.discountPercent}%)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "-$currency${savings.formatDecimal()}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 4.dp))

            // Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total per month",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$currency${totalPrice.formatDecimal()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Encourage message for more workspaces
            if (!hasDiscount && discount.isAvailable) {
                val workspacesNeeded = discount.minWorkspaces - workspaceCount
                InfoBanner(
                    message = "Add $workspacesNeeded more workspace(s) to get ${discount.discountPercent}% off!",
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

/**
 * Info banner for discount-related messages
 */
@Composable
fun InfoBanner(
    message: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

/**
 * Success banner for discount eligibility
 */
@Composable
fun SuccessBanner(
    message: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🎉",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

/**
 * Discount info dialog with detailed explanation
 */
@Composable
fun DiscountInfoDialog(
    discount: MultiWorkspaceDiscount,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Multi-Workspace Discount") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "How it works:",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )

                Text("• Create ${discount.minWorkspaces} or more workspaces")
                Text("• Get ${discount.discountPercent}% off on your subscription")
                Text("• Discount applies to all your workspaces")
                Text("• Stacks with annual billing discount")

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Example:",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )

                Text("3 workspaces × ₹500 = ₹1,500/month")
                Text("With 20% discount = ₹1,200/month")
                Text(
                    text = "You save ₹300/month! 🎉",
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it!")
            }
        }
    )
}
