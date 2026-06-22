package com.ampairs.payment.ui.components

import ampairsapp.feature.payment.generated.resources.Res
import ampairsapp.feature.payment.generated.resources.payment_clearance_bounced
import ampairsapp.feature.payment.generated.resources.payment_clearance_cancelled
import ampairsapp.feature.payment.generated.resources.payment_clearance_cleared
import ampairsapp.feature.payment.generated.resources.payment_clearance_pending
import ampairsapp.feature.payment.generated.resources.payment_mode_card
import ampairsapp.feature.payment.generated.resources.payment_mode_cash
import ampairsapp.feature.payment.generated.resources.payment_mode_cheque
import ampairsapp.feature.payment.generated.resources.payment_mode_imps
import ampairsapp.feature.payment.generated.resources.payment_mode_neft
import ampairsapp.feature.payment.generated.resources.payment_mode_net_banking
import ampairsapp.feature.payment.generated.resources.payment_mode_other
import ampairsapp.feature.payment.generated.resources.payment_mode_rtgs
import ampairsapp.feature.payment.generated.resources.payment_mode_transfer
import ampairsapp.feature.payment.generated.resources.payment_mode_upi
import ampairsapp.feature.payment.generated.resources.payment_mode_wallet
import ampairsapp.feature.payment.generated.resources.payment_paid
import ampairsapp.feature.payment.generated.resources.payment_received
import ampairsapp.feature.payment.generated.resources.payment_to_pay
import ampairsapp.feature.payment.generated.resources.payment_to_receive
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SouthWest
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ampairs.common.locale.AppLocale
import com.ampairs.common.locale.LocalAppLocale
import com.ampairs.common.locale.formatMoney
import com.ampairs.payment.domain.ClearanceStatus
import com.ampairs.payment.domain.PaymentMode
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Reusable Collections building blocks — the "Component inventory" from the design, implemented once
 * and shared across every Collections screen. Direction colours resolve from [MaterialTheme] tokens
 * (receivable → primary, to-pay / advance → tertiary, overdue → error) so light & dark stay correct;
 * tabular money uses [FontFamily.Monospace].
 */

/** Semantic money-direction colours, all token-derived. */
object CollectionsColors {
    val receivable: Color
        @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.primary
    val payable: Color
        @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.tertiary
    val overdue: Color
        @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.error

    /** Increasing-severity ordering for the aging bar / legend. */
    val agingBuckets: List<Color>
        @Composable @ReadOnlyComposable get() = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.error,
        )

    @Composable
    @ReadOnlyComposable
    fun direction(isReceivable: Boolean, settled: Boolean = false): Color = when {
        settled -> MaterialTheme.colorScheme.onSurfaceVariant
        isReceivable -> receivable
        else -> payable
    }
}

/** Tabular amount in the one money-direction colour rule used everywhere. */
@Composable
fun MoneyText(
    amount: Double,
    isReceivable: Boolean,
    modifier: Modifier = Modifier,
    settled: Boolean = false,
    fontSize: androidx.compose.ui.unit.TextUnit = 15.sp,
    fontWeight: FontWeight = FontWeight.SemiBold,
) {
    val locale = LocalAppLocale.current
    Text(
        text = if (settled) formatMoney(0.0, locale) else formatMoney(amount, locale),
        color = CollectionsColors.direction(isReceivable, settled),
        fontFamily = FontFamily.Monospace,
        fontWeight = fontWeight,
        fontSize = fontSize,
        maxLines = 1,
        modifier = modifier,
    )
}

/** Plain tabular amount (no direction colour) — for neutral figures like running balances. */
@Composable
fun MonoAmount(
    amount: Double,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    fontSize: androidx.compose.ui.unit.TextUnit = 14.sp,
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

/** Circular avatar with up-to-2-char initials; container colour picked deterministically by name. */
@Composable
fun PartyAvatar(name: String, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    val containers = listOf(
        MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer,
        MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer,
    )
    val (bg, fg) = containers[(name.hashCode() % containers.size + containers.size) % containers.size]
    Box(
        modifier = modifier.size(size).background(bg, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initialsOf(name),
            color = fg,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

private fun initialsOf(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> (parts[0].first().toString() + parts[1].first().toString()).uppercase()
    }
}

/** Uppercase section caption used above lists. */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 0.8.sp,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 6.dp),
    )
}

/** A single proportional aging bar + its legend cells, given (label, amount, colour) segments. */
@Composable
fun AgingBar(
    segments: List<AgingSegment>,
    modifier: Modifier = Modifier,
) {
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
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(seg.color, RoundedCornerShape(2.dp)),
                        )
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

/** Segmented Received / Paid control (record-payment step 1, opening balance). */
@Composable
fun DirectionToggle(
    receivedSelected: Boolean,
    onReceived: () -> Unit,
    onPaid: () -> Unit,
    modifier: Modifier = Modifier,
    receivedLabel: String = stringResource(Res.string.payment_received),
    paidLabel: String = stringResource(Res.string.payment_paid),
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(14.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ToggleSegment(
            label = receivedLabel,
            icon = Icons.Filled.SouthWest,
            selected = receivedSelected,
            onClick = onReceived,
            modifier = Modifier.weight(1f),
        )
        ToggleSegment(
            label = paidLabel,
            icon = Icons.Filled.NorthEast,
            selected = !receivedSelected,
            onClick = onPaid,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ToggleSegment(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent
    val fg = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = modifier
            .height(40.dp)
            .background(bg, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
        Text(" $label", color = fg, style = MaterialTheme.typography.labelLarge)
    }
}

/** 3-column grid of payment-mode tiles. */
@Composable
fun ModePickerGrid(
    modes: List<PaymentMode>,
    selected: PaymentMode,
    onSelect: (PaymentMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        modes.chunked(3).forEach { rowModes ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowModes.forEach { mode ->
                    ModeTile(
                        mode = mode,
                        selected = mode == selected,
                        onClick = { onSelect(mode) },
                        modifier = Modifier.weight(1f),
                    )
                }
                // pad short last row so tiles keep their column width
                repeat(3 - rowModes.size) { Box(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun ModeTile(
    mode: PaymentMode,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
    val fg = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Column(
        modifier = modifier
            .aspectRatio(1f)
            .background(bg, RoundedCornerShape(16.dp))
            .border(if (selected) 2.dp else 1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(mode.icon(), contentDescription = null, tint = fg, modifier = Modifier.size(26.dp))
        Text(mode.label(), color = fg, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 6.dp))
    }
}

fun PaymentMode.icon(): ImageVector = when (this) {
    PaymentMode.CASH -> Icons.Filled.Payments
    PaymentMode.UPI -> Icons.Filled.QrCode2
    PaymentMode.CHEQUE -> Icons.Filled.AccountBalance
    PaymentMode.NEFT, PaymentMode.RTGS, PaymentMode.IMPS, PaymentMode.BANK_TRANSFER, PaymentMode.NET_BANKING -> Icons.Filled.SwapHoriz
    PaymentMode.CARD -> Icons.Filled.CreditCard
    PaymentMode.WALLET -> Icons.Filled.AccountBalanceWallet
    PaymentMode.OTHER -> Icons.Filled.MoreHoriz
}

@Composable
fun PaymentMode.label(): String = stringResource(
    when (this) {
        PaymentMode.CASH -> Res.string.payment_mode_cash
        PaymentMode.UPI -> Res.string.payment_mode_upi
        PaymentMode.CHEQUE -> Res.string.payment_mode_cheque
        PaymentMode.NEFT -> Res.string.payment_mode_neft
        PaymentMode.RTGS -> Res.string.payment_mode_rtgs
        PaymentMode.IMPS -> Res.string.payment_mode_imps
        PaymentMode.NET_BANKING -> Res.string.payment_mode_net_banking
        PaymentMode.CARD -> Res.string.payment_mode_card
        PaymentMode.BANK_TRANSFER -> Res.string.payment_mode_transfer
        PaymentMode.WALLET -> Res.string.payment_mode_wallet
        PaymentMode.OTHER -> Res.string.payment_mode_other
    },
)

/** Clearance status pill. */
@Composable
fun ClearanceChip(status: ClearanceStatus, modifier: Modifier = Modifier) {
    val (bg, fg, icon, labelRes) = when (status) {
        ClearanceStatus.PENDING -> Quad(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            Icons.Filled.Schedule,
            Res.string.payment_clearance_pending,
        )
        ClearanceStatus.CLEARED -> Quad(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            Icons.Filled.CheckCircle,
            Res.string.payment_clearance_cleared,
        )
        ClearanceStatus.BOUNCED -> Quad(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            Icons.Filled.Error,
            Res.string.payment_clearance_bounced,
        )
        ClearanceStatus.CANCELLED -> Quad(
            MaterialTheme.colorScheme.surfaceContainerHigh,
            MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Filled.Block,
            Res.string.payment_clearance_cancelled,
        )
    }
    Row(
        modifier = modifier
            .background(bg, RoundedCornerShape(9.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(14.dp))
        Text(" " + stringResource(labelRes), color = fg, style = MaterialTheme.typography.labelMedium)
    }
}

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
private operator fun <A, B, C, D> Quad<A, B, C, D>.component1() = a
private operator fun <A, B, C, D> Quad<A, B, C, D>.component2() = b
private operator fun <A, B, C, D> Quad<A, B, C, D>.component3() = c
private operator fun <A, B, C, D> Quad<A, B, C, D>.component4() = d

/** Per-row sync indicator: cloud_off (pending) vs cloud_done (synced). */
@Composable
fun SyncStatusIcon(pending: Boolean, modifier: Modifier = Modifier) {
    Icon(
        imageVector = if (pending) Icons.Filled.CloudOff else Icons.Filled.CloudDone,
        contentDescription = null,
        tint = if (pending) CollectionsColors.payable else MaterialTheme.colorScheme.outline,
        modifier = modifier.size(14.dp),
    )
}

/** Centered icon + copy empty state with up to two actions (icon-only, per the system — no art). */
@Composable
fun CollectionsEmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    primaryActionLabel: String? = null,
    onPrimaryAction: (() -> Unit)? = null,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(44.dp))
        }
        Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 22.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        if (primaryActionLabel != null && onPrimaryAction != null) {
            Button(onClick = onPrimaryAction, modifier = Modifier.padding(top = 24.dp)) {
                Text(primaryActionLabel)
            }
        }
        if (secondaryActionLabel != null && onSecondaryAction != null) {
            TextButton(onClick = onSecondaryAction, modifier = Modifier.padding(top = 4.dp)) {
                Text(secondaryActionLabel)
            }
        }
    }
}

/** Big 3×4 numeric keypad for the record-payment hero. Emits raw key presses to the caller. */
@Composable
fun AmountKeypad(
    onDigit: (Char) -> Unit,
    onDot: () -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(".", "0", "<"),
    )
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .clickable {
                                when (key) {
                                    "<" -> onBackspace()
                                    "." -> onDot()
                                    else -> onDigit(key.first())
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (key == "<") {
                            Icon(
                                Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        } else {
                            Text(
                                key,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 24.sp,
                                color = if (key == ".") MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}
