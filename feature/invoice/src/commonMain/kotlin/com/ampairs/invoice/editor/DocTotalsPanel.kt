package com.ampairs.invoice.editor

import ampairsapp.feature.invoice.generated.resources.Res
import ampairsapp.feature.invoice.generated.resources.doc_tot_apportioned
import ampairsapp.feature.invoice.generated.resources.doc_tot_gst
import ampairsapp.feature.invoice.generated.resources.doc_tot_grand
import ampairsapp.feature.invoice.generated.resources.doc_tot_grand_a11y
import ampairsapp.feature.invoice.generated.resources.doc_tot_inter_chip
import ampairsapp.feature.invoice.generated.resources.doc_tot_intra_chip
import ampairsapp.feature.invoice.generated.resources.doc_tot_line_discounts
import ampairsapp.feature.invoice.generated.resources.doc_tot_mode_post
import ampairsapp.feature.invoice.generated.resources.doc_tot_mode_pre
import ampairsapp.feature.invoice.generated.resources.doc_tot_overall
import ampairsapp.feature.invoice.generated.resources.doc_tot_overall_post
import ampairsapp.feature.invoice.generated.resources.doc_tot_reconciles
import ampairsapp.feature.invoice.generated.resources.doc_tot_reconciles_post
import ampairsapp.feature.invoice.generated.resources.doc_tot_subtotal
import ampairsapp.feature.invoice.generated.resources.doc_tot_tax_row_a11y
import ampairsapp.feature.invoice.generated.resources.doc_tot_taxable
import ampairsapp.feature.invoice.generated.resources.doc_tot_total_inter
import ampairsapp.feature.invoice.generated.resources.doc_tot_total_intra
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ampairs.common.format.toDecimal
import com.ampairs.common.locale.LocalAppLocale
import com.ampairs.common.locale.formatMoney
import com.ampairs.invoice.ui.TotalsUi
import com.ampairs.tax.calculation.document.DiscountKind
import com.ampairs.tax.calculation.document.OverallDiscountMode
import com.ampairs.tax.calculation.document.TaxScenario
import org.jetbrains.compose.resources.stringResource

/**
 * The always-reconciling GST breakdown / totals panel (spec 010 v2, screen "totals").
 * Pinned right rail on expanded, bottom-sheet content on compact. No price-mode toggle here —
 * the price mode is a workspace setting (design v2), surfaced read-only in the header.
 */
@Composable
fun DocTotalsPanel(
    totals: TotalsUi,
    overallDiscountKind: DiscountKind,
    overallDiscountAmount: Double,
    showOverallDiscount: Boolean,
    onOverallDiscount: (DiscountKind, Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = LocalAppLocale.current
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
            TotRow(stringResource(Res.string.doc_tot_subtotal, totals.itemCount), formatMoney(totals.subtotalGross, locale), cs.onSurfaceVariant)
            if (totals.lineDiscountTotal > 0.0) {
                TotRow(stringResource(Res.string.doc_tot_line_discounts), "− ${formatMoney(totals.lineDiscountTotal, locale)}", cs.secondary)
            }

            if (showOverallDiscount) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(Res.string.doc_tot_overall), style = MaterialTheme.typography.bodyMedium, color = cs.onSurface)
                        Text(
                            stringResource(if (preTax) Res.string.doc_tot_mode_pre else Res.string.doc_tot_mode_post),
                            style = MaterialTheme.typography.labelSmall,
                            color = cs.onSurfaceVariant,
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
                    DiscountValueField(
                        amount = overallDiscountAmount,
                        onAmount = { onOverallDiscount(overallDiscountKind, it) },
                        modifier = Modifier.padding(start = 8.dp).width(92.dp),
                    )
                }
                if (preTax && totals.overallDiscountValue > 0.0) {
                    TotRow(stringResource(Res.string.doc_tot_apportioned), "− ${formatMoney(totals.overallDiscountValue, locale)}", cs.secondary)
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = cs.outlineVariant)
            TotRow(stringResource(Res.string.doc_tot_taxable), formatMoney(totals.taxableSubtotal, locale), cs.onSurface)

            if (totals.taxGroups.isEmpty()) {
                TotRow(stringResource(Res.string.doc_tot_gst), formatMoney(0.0, locale), cs.onSurface)
            }
            totals.taxGroups.forEach { g ->
                val a11y = stringResource(Res.string.doc_tot_tax_row_a11y, g.name, g.percentage.toDecimal(), formatMoney(g.amount, locale))
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 5.dp).semantics { contentDescription = a11y },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(g.name, style = MaterialTheme.typography.bodyMedium, color = cs.onSurface)
                    Surface(
                        color = cs.surfaceContainerHigh,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(start = 7.dp),
                    ) {
                        Text(
                            ratePctLabel(g.percentage),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = mono,
                            color = cs.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                        )
                    }
                    Text(
                        formatMoney(g.amount, locale),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End,
                        fontFamily = mono,
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurface,
                    )
                }
            }
            TotRow(
                stringResource(if (intra) Res.string.doc_tot_total_intra else Res.string.doc_tot_total_inter),
                formatMoney(totals.totalTax, locale), cs.onSurface,
            )

            if (!preTax && totals.overallDiscountValue > 0.0) {
                TotRow(stringResource(Res.string.doc_tot_overall_post), "− ${formatMoney(totals.overallDiscountValue, locale)}", cs.secondary)
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = cs.outlineVariant)
            val grandA11y = stringResource(Res.string.doc_tot_grand_a11y, formatMoney(totals.grandTotal, locale))
            Row(
                Modifier.fillMaxWidth().padding(top = 2.dp).semantics { contentDescription = grandA11y },
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    stringResource(Res.string.doc_tot_grand),
                    style = MaterialTheme.typography.titleMedium,
                    color = cs.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    formatMoney(totals.grandTotal, locale),
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = mono,
                    color = cs.onSurface,
                    maxLines = 1,
                )
            }

            Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = cs.primary, modifier = Modifier.size(15.dp))
                Text(
                    "  " + stringResource(
                        if (!preTax && totals.overallDiscountValue > 0.0) Res.string.doc_tot_reconciles_post
                        else Res.string.doc_tot_reconciles
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant,
                )
            }
            Surface(
                color = cs.tertiaryContainer,
                shape = RoundedCornerShape(50),
                modifier = Modifier.padding(top = 10.dp),
            ) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (intra) Icons.Filled.Home else Icons.Filled.LocalShipping,
                        contentDescription = null, tint = cs.onTertiaryContainer, modifier = Modifier.size(14.dp),
                    )
                    Text(
                        "  " + stringResource(if (intra) Res.string.doc_tot_intra_chip else Res.string.doc_tot_inter_chip),
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.onTertiaryContainer,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** Numeric field that doesn't fight the user mid-keystroke (external value syncs when unfocused). */
@Composable
internal fun DiscountValueField(
    amount: Double,
    onAmount: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember { mutableStateOf(if (amount == 0.0) "" else amount.toDecimal()) }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(amount, focused) {
        if (!focused) text = if (amount == 0.0) "" else amount.toDecimal()
    }
    OutlinedTextField(
        value = text,
        onValueChange = { raw ->
            val filtered = raw.filter { it.isDigit() || it == '.' }
            text = filtered
            onAmount(filtered.toDoubleOrNull() ?: 0.0)
        },
        placeholder = { Text("0") },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, textAlign = TextAlign.End),
        modifier = modifier.onFocusChanged { focused = it.isFocused },
    )
}

@Composable
private fun TotRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            color = valueColor,
            maxLines = 1,
        )
    }
}
