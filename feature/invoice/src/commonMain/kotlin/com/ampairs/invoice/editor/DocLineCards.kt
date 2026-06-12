package com.ampairs.invoice.editor

import ampairsapp.feature.invoice.generated.resources.Res
import ampairsapp.feature.invoice.generated.resources.doc_line_exempt
import ampairsapp.feature.invoice.generated.resources.doc_stepper_minus_cd
import ampairsapp.feature.invoice.generated.resources.doc_stepper_plus_cd
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ampairs.common.format.toDecimal
import com.ampairs.common.format.toInr
import com.ampairs.invoice.editor.entry.EntryMatcher
import com.ampairs.tax.calculation.document.DiscountKind
import org.jetbrains.compose.resources.stringResource

/**
 * Compact (phone) line cards (spec 010 v2): name + line total + chips, with in-place −/+
 * quantity steppers (≥44dp targets, unit-decimal aware, floor at the unit's minimum step).
 * Tapping the card opens the per-line bottom sheet.
 */
@Composable
fun DocLineCards(
    lines: List<DocLineUi>,
    onQuantity: (String, Double) -> Unit,
    onEdit: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(lines.size, key = { lines[it].id }) { i ->
            LineCard(lines[i], onQuantity, onEdit)
        }
    }
}

@Composable
private fun LineCard(
    line: DocLineUi,
    onQuantity: (String, Double) -> Unit,
    onEdit: (String) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cs.surfaceContainerLow,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().clickable { onEdit(line.id) },
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(
                        line.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    if (line.variantSku != null) {
                        Text(
                            line.variantSku,
                            style = MaterialTheme.typography.labelSmall,
                            color = cs.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
                Text(
                    line.lineTotal.toInr(),
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Row(
                Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                QuantityStepper(line, onQuantity)
                CardChip("${line.unitPrice.toInr()}/${line.unitName}")
                if (line.discountAmount > 0.0) {
                    CardChip(
                        if (line.discountKind == DiscountKind.PERCENT) "−${line.discountAmount.toDecimal()}%"
                        else "−${line.discountAmount.toInr()}",
                        emphasized = true,
                    )
                }
                CardChip(
                    if (line.exempt) stringResource(Res.string.doc_line_exempt)
                    else "GST ${ratePctLabel(line.gstRatePercent ?: 0.0)}"
                )
            }
        }
    }
}

/** −/+ stepper: steps by 1 (or the unit's smallest decimal step), floors at the minimum step. */
@Composable
private fun QuantityStepper(line: DocLineUi, onQuantity: (String, Double) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val minStep = if (line.decimalPlaces > 0) {
        var s = 1.0
        repeat(line.decimalPlaces) { s /= 10.0 }
        s
    } else 1.0
    fun step(delta: Double) {
        val next = EntryMatcher.clampToDecimals(line.quantity + delta, line.decimalPlaces)
        if (next >= minStep) onQuantity(line.id, next)
    }
    Surface(shape = RoundedCornerShape(50), color = cs.surfaceContainerHigh) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StepButton("−", stringResource(Res.string.doc_stepper_minus_cd)) { step(-1.0) }
            Text(
                "${line.quantity.toDecimal()} ${line.unitName}",
                style = MaterialTheme.typography.labelMedium,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            StepButton("+", stringResource(Res.string.doc_stepper_plus_cd)) { step(1.0) }
        }
    }
}

@Composable
private fun StepButton(symbol: String, cd: String, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(50),
        color = cs.surfaceContainerHighest,
        modifier = Modifier
            .sizeIn(minWidth = 44.dp, minHeight = 44.dp)
            .clickable(onClick = onClick),
    ) {
        Text(
            symbol,
            style = MaterialTheme.typography.titleMedium,
            color = cs.onSurface,
            modifier = Modifier.padding(12.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun CardChip(text: String, emphasized: Boolean = false) {
    val cs = MaterialTheme.colorScheme
    Surface(
        color = if (emphasized) cs.secondaryContainer else cs.surfaceContainerHigh,
        shape = RoundedCornerShape(50),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = if (emphasized) cs.onSecondaryContainer else cs.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            maxLines = 1,
        )
    }
}
