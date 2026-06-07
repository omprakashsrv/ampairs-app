package com.ampairs.invoice.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ampairs.common.format.toDecimal
import com.ampairs.tax.calculation.document.DiscountKind
import com.ampairs.tax.calculation.document.OverallDiscountMode
import com.ampairs.tax.calculation.document.PriceMode
import com.ampairs.tax.calculation.document.TaxScenario

/** GST component grouped by name + rate (e.g. CGST @ 9%). */
data class TaxGroupUi(val name: String, val percentage: Double, val amount: Double)

/** UI model for the totals/GST breakdown panel (spec 010, design screen 8). */
data class TotalsUi(
    val subtotalGross: Double = 0.0,
    val lineDiscountTotal: Double = 0.0,
    val overallDiscountValue: Double = 0.0,
    val taxableSubtotal: Double = 0.0,
    val taxGroups: List<TaxGroupUi> = emptyList(),
    val totalTax: Double = 0.0,
    val grandTotal: Double = 0.0,
    val scenario: TaxScenario = TaxScenario.INTRA,
    val priceMode: PriceMode = PriceMode.TAX_EXCLUSIVE,
    val overallDiscountMode: OverallDiscountMode = OverallDiscountMode.POST_TAX_REDUCTION,
    val itemCount: Int = 0,
)

private fun ratePct(p: Double): String {
    val whole = p % 1.0 == 0.0
    return if (whole) "${p.toInt()}%" else "$p%"
}

/** Sticky GST totals + price-mode toggle + overall-discount control (design screens 8 + 2). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceTotalsPanel(
    totals: TotalsUi,
    overallDiscountKind: DiscountKind,
    overallDiscountAmount: Double,
    onPriceMode: (PriceMode) -> Unit,
    onOverallDiscount: (DiscountKind, Double) -> Unit,
    onOverallDiscountMode: (OverallDiscountMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val mono = FontFamily.Monospace
    val preTax = totals.overallDiscountMode == OverallDiscountMode.PRE_TAX_APPORTIONED
    val intra = totals.scenario == TaxScenario.INTRA

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = cs.surfaceContainerLow,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp,
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Price mode", style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp)) {
                val opts = listOf(PriceMode.TAX_EXCLUSIVE to "Tax-exclusive", PriceMode.TAX_INCLUSIVE to "Tax-inclusive")
                opts.forEachIndexed { i, (mode, label) ->
                    SegmentedButton(
                        selected = totals.priceMode == mode,
                        onClick = { onPriceMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(i, opts.size),
                    ) { Text(label, maxLines = 1) }
                }
            }

            TotRow("Subtotal · ${totals.itemCount} items", totals.subtotalGross.toDecimal(), mono, cs.onSurfaceVariant)
            if (totals.lineDiscountTotal > 0.0) {
                TotRow("Line discounts", "− ${totals.lineDiscountTotal.toDecimal()}", mono, cs.secondary)
            }

            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Overall discount", style = MaterialTheme.typography.bodyMedium, color = cs.onSurface)
                    Text(
                        if (preTax) "apportioned pre-tax" else "post-tax reduction",
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.onSurfaceVariant,
                        modifier = Modifier.clickable { onOverallDiscountMode(if (preTax) OverallDiscountMode.POST_TAX_REDUCTION else OverallDiscountMode.PRE_TAX_APPORTIONED) },
                    )
                }
                SingleChoiceSegmentedButtonRow {
                    listOf(DiscountKind.PERCENT to "%", DiscountKind.FLAT to "₹").forEachIndexed { i, (k, l) ->
                        SegmentedButton(
                            selected = overallDiscountKind == k,
                            onClick = { onOverallDiscount(k, overallDiscountAmount) },
                            shape = SegmentedButtonDefaults.itemShape(i, 2),
                        ) { Text(l) }
                    }
                }
                OutlinedTextField(
                    value = if (overallDiscountAmount == 0.0) "" else overallDiscountAmount.toString(),
                    onValueChange = { onOverallDiscount(overallDiscountKind, it.toDoubleOrNull() ?: 0.0) },
                    placeholder = { Text("0") },
                    singleLine = true,
                    modifier = Modifier.padding(start = 8.dp).width(96.dp),
                )
            }
            if (preTax && totals.overallDiscountValue > 0.0) {
                TotRow("— apportioned across lines", "− ${totals.overallDiscountValue.toDecimal()}", mono, cs.secondary)
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = cs.outlineVariant)
            TotRow("Taxable value", totals.taxableSubtotal.toDecimal(), mono, cs.onSurface)

            if (totals.taxGroups.isEmpty()) {
                TotRow("GST", 0.0.toDecimal(), mono, cs.onSurface)
            }
            totals.taxGroups.forEach { g ->
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(g.name, style = MaterialTheme.typography.bodyMedium, color = cs.onSurface)
                    Surface(
                        color = cs.surfaceContainerHigh,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(start = 7.dp),
                    ) {
                        Text(
                            ratePct(g.percentage),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = mono,
                            color = cs.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                        )
                    }
                    Text(
                        g.amount.toDecimal(),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End,
                        fontFamily = mono,
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurface,
                    )
                }
            }
            TotRow(
                if (intra) "Total CGST + SGST" else "Total IGST",
                totals.totalTax.toDecimal(), mono, cs.onSurface,
            )

            if (!preTax && totals.overallDiscountValue > 0.0) {
                TotRow("Overall discount (post-tax)", "− ${totals.overallDiscountValue.toDecimal()}", mono, cs.secondary)
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = cs.outlineVariant)
            Row(Modifier.fillMaxWidth().padding(top = 2.dp), verticalAlignment = Alignment.Bottom) {
                Text("Grand total", style = MaterialTheme.typography.titleMedium, color = cs.onSurface, modifier = Modifier.weight(1f))
                Text(totals.grandTotal.toDecimal(), style = MaterialTheme.typography.titleLarge, fontFamily = mono, color = cs.onSurface)
            }

            Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = cs.primary, modifier = Modifier.size(15.dp))
                Text("  Reconciles · taxable + GST = grand total", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
            }
            Surface(color = cs.tertiaryContainer, shape = RoundedCornerShape(50), modifier = Modifier.padding(top = 10.dp)) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (intra) Icons.Filled.Home else Icons.Filled.LocalShipping,
                        contentDescription = null, tint = cs.onTertiaryContainer, modifier = Modifier.size(14.dp),
                    )
                    Text(
                        if (intra) "  Intra-state (CGST + SGST)" else "  Inter-state (IGST)",
                        style = MaterialTheme.typography.labelSmall, color = cs.onTertiaryContainer,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun TotRow(label: String, value: String, mono: FontFamily, valueColor: androidx.compose.ui.graphics.Color) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontFamily = mono, color = valueColor, maxLines = 1)
    }
}
