package com.ampairs.subscription.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ampairs.common.util.formatDecimal
import com.ampairs.subscription.domain.model.PreLaunchDiscount
import kotlinx.coroutines.delay

// Vibrant orange for pre-launch theme
private val PreLaunchOrange = Color(0xFFFF6B35)
private val PreLaunchGold = Color(0xFFF7931E)

/**
 * Compact pre-launch badge for plan cards
 */
@Composable
fun PreLaunchBadge(
    discount: PreLaunchDiscount,
    modifier: Modifier = Modifier
) {
    if (!discount.isActive) return

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = PreLaunchOrange,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "🚀",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${discount.discountPercent}% OFF",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "PRE-LAUNCH",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.9f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Prominent hero banner for top of subscription screen
 */
@Composable
fun PreLaunchHeroBanner(
    discount: PreLaunchDiscount,
    modifier: Modifier = Modifier
) {
    if (!discount.isActive) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(PreLaunchOrange, PreLaunchGold)
                )
            )
            .padding(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // "PRE-LAUNCH" label
            Text(
                text = "🚀 PRE-LAUNCH OFFER",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.9f),
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Main discount message
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${discount.discountPercent}%",
                    style = MaterialTheme.typography.displayMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "OFF",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Subtitle
            Text(
                text = "Early Bird Special - Limited Time!",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.95f),
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Countdown
            val daysRemaining = discount.getDaysRemaining()
            if (daysRemaining > 0) {
                PreLaunchCountdown(
                    daysRemaining = daysRemaining,
                    textColor = Color.White
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // User eligibility notice
            if (discount.newUsersOnly) {
                Text(
                    text = "✨ Available for new subscribers",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }
    }
}

/**
 * Compact countdown showing days remaining
 */
@Composable
fun PreLaunchCountdown(
    daysRemaining: Long,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    if (daysRemaining <= 0) return

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "⏰ Ends in: ",
            style = MaterialTheme.typography.bodyMedium,
            color = textColor.copy(alpha = 0.9f),
            fontWeight = FontWeight.Medium
        )

        Surface(
            shape = RoundedCornerShape(4.dp),
            color = Color.White.copy(alpha = 0.2f)
        ) {
            Text(
                text = "$daysRemaining day${if (daysRemaining > 1) "s" else ""}",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Complete discount breakdown card with pre-launch highlighted
 */
@Composable
fun PreLaunchDiscountBreakdown(
    basePrice: Double,
    preLaunchDiscount: PreLaunchDiscount,
    isEligible: Boolean,
    otherDiscounts: List<Pair<String, Double>> = emptyList(),
    currency: String = "₹",
    modifier: Modifier = Modifier
) {
    if (!preLaunchDiscount.isActive || !isEligible) return

    var currentPrice = basePrice

    // Calculate pre-launch savings
    val preLaunchSavings = basePrice * preLaunchDiscount.discountPercent / 100.0
    currentPrice -= preLaunchSavings

    // Calculate other discounts
    val otherSavings = otherDiscounts.sumOf { it.second }
    currentPrice -= otherSavings

    val totalSavings = preLaunchSavings + otherSavings
    val savingsPercent = ((totalSavings / basePrice) * 100).toInt()

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = PreLaunchOrange.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🚀", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Your Savings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PreLaunchOrange
                ) {
                    Text(
                        text = "Save $savingsPercent%",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Divider()

            // Base price
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Base Price",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "$currency${basePrice.formatDecimal()}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            // Pre-launch discount (highlighted)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = PreLaunchOrange
                    ) {
                        Text(
                            text = "EARLY BIRD",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pre-Launch (-${preLaunchDiscount.discountPercent}%)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = PreLaunchOrange
                    )
                }
                Text(
                    text = "-$currency${preLaunchSavings.formatDecimal()}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = PreLaunchOrange
                )
            }

            // Other discounts
            otherDiscounts.forEach { (name, amount) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "✓ $name",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "-$currency${amount.formatDecimal()}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Divider()

            // Final price
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Final Price",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$currency${currentPrice.formatDecimal()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PreLaunchOrange
                )
            }

            // Savings highlight
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF4CAF50).copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(text = "💰", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "You save $currency${totalSavings.formatDecimal()} every month!",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2E7D32)
                    )
                }
            }
        }
    }
}

/**
 * Eligibility indicator for pre-launch discount
 */
@Composable
fun PreLaunchEligibilityBadge(
    isEligible: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = if (isEligible) Color(0xFF4CAF50).copy(alpha = 0.1f)
        else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = if (isEligible) "✓" else "✗",
                style = MaterialTheme.typography.labelMedium,
                color = if (isEligible) Color(0xFF2E7D32)
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (isEligible) "You're eligible!" else "Not eligible",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = if (isEligible) Color(0xFF2E7D32)
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Urgency indicator based on days remaining
 */
@Composable
fun PreLaunchUrgencyBadge(
    discount: PreLaunchDiscount,
    modifier: Modifier = Modifier
) {
    if (!discount.isActive) return

    val daysRemaining = discount.getDaysRemaining()

    val (text, color) = when {
        daysRemaining <= 0 -> return
        daysRemaining == 1L -> "LAST DAY!" to Color(0xFFD32F2F)
        daysRemaining <= 3 -> "ENDING SOON" to PreLaunchOrange
        daysRemaining <= 7 -> "Limited Time" to Color(0xFFF57C00)
        else -> return
    }

    Surface(
        modifier = modifier,
        color = color,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

/**
 * Info card explaining pre-launch discount benefits
 */
@Composable
fun PreLaunchInfoCard(
    discount: PreLaunchDiscount,
    modifier: Modifier = Modifier
) {
    if (!discount.isActive) return

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = PreLaunchOrange.copy(alpha = 0.05f)
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
                Text(text = "🚀", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "Pre-Launch Benefits",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            BenefitItem("${discount.discountPercent}% discount on all plans")
            BenefitItem("Stacks with other offers for maximum savings")
            BenefitItem("Lock in early-bird pricing")
            BenefitItem("Join our founding members")

            if (discount.newUsersOnly) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "* Available for new subscribers only",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BenefitItem(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = PreLaunchOrange,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
