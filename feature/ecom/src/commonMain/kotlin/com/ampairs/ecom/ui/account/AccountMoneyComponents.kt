package com.ampairs.ecom.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ampairs.common.locale.LocalAppLocale
import com.ampairs.common.locale.formatMoney

/**
 * Buyer account money-position UI building blocks — a local, presentational copy of the payment
 * module's Collections components (`MonoAmount` / `SectionHeader` / direction colours / aging bar).
 * Kept in `ecom` rather than imported because `feature/ecom` must not depend on the `feature/payment`
 * impl module (cross-feature deps go through `-api` only). Colours resolve from [MaterialTheme]
 * tokens so light & dark stay correct; tabular money uses [FontFamily.Monospace].
 */

/** Semantic money-direction colours for the buyer statement, mirroring the payment convention. */
object StatementColors {
    /** A ledger debit — a raised bill increasing what the buyer owes. */
    val debit: Color
        @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.primary

    /** A ledger credit — a payment reducing what the buyer owes. */
    val credit: Color
        @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.tertiary

    /** Increasing-severity palette for aging segments (current → most overdue). */
    val agingBuckets: List<Color>
        @Composable @ReadOnlyComposable get() = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.error,
        )
}

/** Tabular (monospace) amount, single line. */
@Composable
fun MonoAmount(
    amount: Double,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    fontSize: TextUnit = 14.sp,
    fontWeight: FontWeight = FontWeight.SemiBold,
    prefix: String = "",
) {
    val locale = LocalAppLocale.current
    Text(
        text = prefix + formatMoney(amount, locale),
        color = color,
        fontFamily = FontFamily.Monospace,
        fontWeight = fontWeight,
        fontSize = fontSize,
        maxLines = 1,
        modifier = modifier,
    )
}

/** Uppercase section caption used above lists (matches the payment statement look). */
@Composable
fun MoneySectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 0.8.sp,
        modifier = modifier.padding(horizontal = 16.dp, top = 18.dp, bottom = 6.dp),
    )
}

/** A proportional aging bar + legend, given (label, amount, colour) segments. */
@Composable
fun AgingBar(segments: List<AgingSegment>, modifier: Modifier = Modifier) {
    val total = segments.sumOf { it.amount }.coerceAtLeast(1.0)
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().height(9.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            segments.filter { it.amount > 0 }.forEach { seg ->
                Box(
                    modifier = Modifier
                        .weight((seg.amount / total).toFloat().coerceAtLeast(0.001f))
                        .fillMaxSize()
                        .background(seg.color, RoundedCornerShape(3.dp)),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            segments.forEach { seg ->
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(7.dp).background(seg.color, RoundedCornerShape(2.dp)))
                        Text(
                            text = " " + seg.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    MonoAmount(
                        amount = seg.amount,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
    }
}

data class AgingSegment(val label: String, val amount: Double, val color: Color)
