package com.ampairs.subscription.ui.components

import androidx.compose.animation.core.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ampairs.subscription.domain.model.*
import kotlinx.coroutines.delay

// Festive orange/red color for seasonal discounts
private val SeasonalColor = Color(0xFFFF6B35)

/**
 * Eye-catching badge for active seasonal offers
 */
@Composable
fun SeasonalDiscountBadge(
    seasonalDiscount: SeasonalDiscount,
    modifier: Modifier = Modifier
) {
    if (!seasonalDiscount.isActive) return

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(
            topStart = 0.dp,
            topEnd = 8.dp,
            bottomStart = 8.dp,
            bottomEnd = 0.dp
        ),
        color = SeasonalColor
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "${seasonalDiscount.discountPercent}% OFF",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            seasonalDiscount.discountName?.let { name ->
                Text(
                    text = name.split(" ").take(2).joinToString(" "), // First 2 words
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Banner on pricing screen when seasonal discount is active
 */
@Composable
fun SeasonalDiscountBanner(
    seasonalDiscount: SeasonalDiscount?,
    modifier: Modifier = Modifier
) {
    seasonalDiscount ?: return
    if (!seasonalDiscount.isActive) return

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SeasonalColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Animated festive icon
            Icon(
                imageVector = Icons.Default.Celebration,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = seasonalDiscount.discountName ?: "Limited Time Offer!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Get ${seasonalDiscount.discountPercent}% OFF on all plans!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )

                // Countdown
                val daysRemaining = seasonalDiscount.getDaysRemaining()
                if (daysRemaining > 0) {
                    Text(
                        text = "⏰ Ends in $daysRemaining day${if (daysRemaining > 1) "s" else ""}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Compact countdown timer for seasonal discount
 */
@Composable
fun SeasonalDiscountCountdown(
    seasonalDiscount: SeasonalDiscount,
    modifier: Modifier = Modifier
) {
    if (!seasonalDiscount.isActive) return

    val daysRemaining = seasonalDiscount.getDaysRemaining()
    if (daysRemaining <= 0) return

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = SeasonalColor.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = null,
                tint = SeasonalColor,
                modifier = Modifier.size(20.dp)
            )

            Text(
                text = "Offer ends in: $daysRemaining day${if (daysRemaining > 1) "s" else ""}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = SeasonalColor
            )
        }
    }
}

/**
 * Complete discount breakdown showing all stacked discounts
 */
@Composable
fun DiscountBreakdownCard(
    plan: SubscriptionPlanDefinition,
    workspaceCount: Int,
    currency: String = "INR",
    modifier: Modifier = Modifier
) {
    val discounts = plan.getDiscountBreakdown(currency, workspaceCount)
    if (discounts.isEmpty()) return

    val basePrice = plan.getMonthlyPrice(currency)
    val finalPrice = plan.getPriceWithDiscount(currency, workspaceCount)
    val totalSavings = basePrice - finalPrice

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "💰 Your Savings",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )

                Text(
                    text = "₹%.2f".format(totalSavings * workspaceCount) + " saved!",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Divider(modifier = Modifier.padding(vertical = 4.dp))

            discounts.forEach { discount ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (discount.type == DiscountType.SEASONAL) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = SeasonalColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "✓ ${discount.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (discount.type == DiscountType.SEASONAL) SeasonalColor
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            fontWeight = if (discount.type == DiscountType.SEASONAL) FontWeight.Bold
                            else FontWeight.Normal
                        )
                    }
                    Text(
                        text = "-₹%.2f".format(discount.amount),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

/**
 * Price display with original price strikethrough when discounts apply
 */
@Composable
fun PriceWithDiscounts(
    plan: SubscriptionPlanDefinition,
    workspaceCount: Int,
    currency: String = "INR",
    modifier: Modifier = Modifier
) {
    val hasDiscounts = plan.hasAnyDiscount(workspaceCount)
    val basePrice = plan.getMonthlyPrice(currency)
    val finalPrice = plan.getPriceWithDiscount(currency, workspaceCount)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End
    ) {
        // Show original price with strikethrough if discounts apply
        if (hasDiscounts) {
            Text(
                text = "₹%.2f".format(basePrice),
                style = MaterialTheme.typography.bodyMedium,
                textDecoration = TextDecoration.LineThrough,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        // Discounted/final price
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "₹%.2f".format(finalPrice),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (hasDiscounts) MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "/workspace",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = "/month",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Urgency indicator based on days remaining
 */
@Composable
fun DiscountUrgencyBadge(
    seasonalDiscount: SeasonalDiscount,
    modifier: Modifier = Modifier
) {
    if (!seasonalDiscount.isActive) return

    val (text, color) = when {
        seasonalDiscount.isCritical() -> "LAST DAY!" to Color(0xFFD32F2F)
        seasonalDiscount.isEndingSoon() -> "ENDING SOON" to Color(0xFFFF6B35)
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
