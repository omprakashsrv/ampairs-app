package com.ampairs.invoice.editor

import ampairsapp.feature.invoice.generated.resources.Res
import ampairsapp.feature.invoice.generated.resources.doc_col_disc
import ampairsapp.feature.invoice.generated.resources.doc_col_gst
import ampairsapp.feature.invoice.generated.resources.doc_col_price
import ampairsapp.feature.invoice.generated.resources.doc_col_product
import ampairsapp.feature.invoice.generated.resources.doc_col_qty
import ampairsapp.feature.invoice.generated.resources.doc_col_taxable
import ampairsapp.feature.invoice.generated.resources.doc_col_total
import ampairsapp.feature.invoice.generated.resources.doc_col_unit
import ampairsapp.feature.invoice.generated.resources.doc_col_variant
import ampairsapp.feature.invoice.generated.resources.doc_grid_footer_hint
import ampairsapp.feature.invoice.generated.resources.doc_line_exempt
import ampairsapp.feature.invoice.generated.resources.doc_line_hsn
import ampairsapp.feature.invoice.generated.resources.doc_line_none
import ampairsapp.feature.invoice.generated.resources.doc_line_remove_cd
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import com.ampairs.common.format.toDecimal
import com.ampairs.common.format.toInr
import com.ampairs.tax.calculation.document.DiscountKind
import org.jetbrains.compose.resources.stringResource

/**
 * Expanded (tablet/desktop) editable line grid (spec 010 v2). Qty/price/discount edit inline;
 * Unit and Variant cells open pickers anchored to the cell; lines are added from the command bar.
 */
@Composable
fun DocLineGrid(
    lines: List<DocLineUi>,
    unitChoices: List<DocUnitChoiceUi>,
    variantChoices: List<DocVariantChoiceUi>,
    onLoadUnitChoices: (String) -> Unit,
    onLoadVariantChoices: (String) -> Unit,
    onSelectUnit: (String, String) -> Unit,
    onSelectVariant: (String, String) -> Unit,
    onQuantity: (String, Double) -> Unit,
    onUnitPrice: (String, Double) -> Unit,
    onDiscount: (String, DiscountKind, Double) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val hScroll = rememberScrollState()
    Column(modifier = modifier.fillMaxWidth().horizontalScroll(hScroll)) {
        Row(
            Modifier.widthIn(min = 860.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Header(stringResource(Res.string.doc_col_product), Modifier.weight(1f), TextAlign.Start)
            Header(stringResource(Res.string.doc_col_variant), Modifier.width(92.dp))
            Header(stringResource(Res.string.doc_col_unit), Modifier.width(92.dp))
            Header(stringResource(Res.string.doc_col_qty), Modifier.width(64.dp))
            Header(stringResource(Res.string.doc_col_price), Modifier.width(96.dp))
            Header(stringResource(Res.string.doc_col_disc), Modifier.width(110.dp))
            Header(stringResource(Res.string.doc_col_taxable), Modifier.width(92.dp), TextAlign.End)
            Header(stringResource(Res.string.doc_col_gst), Modifier.width(60.dp))
            Header(stringResource(Res.string.doc_col_total), Modifier.width(100.dp), TextAlign.End)
            Box(Modifier.width(40.dp))
        }
        HorizontalDivider(color = cs.outlineVariant)
        LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
            items(lines.size, key = { lines[it].id }) { i ->
                GridRow(
                    line = lines[i],
                    unitChoices = unitChoices,
                    variantChoices = variantChoices,
                    onLoadUnitChoices = onLoadUnitChoices,
                    onLoadVariantChoices = onLoadVariantChoices,
                    onSelectUnit = onSelectUnit,
                    onSelectVariant = onSelectVariant,
                    onQuantity = onQuantity,
                    onUnitPrice = onUnitPrice,
                    onDiscount = onDiscount,
                    onRemove = onRemove,
                )
            }
            item(key = "footer-hint") {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Keyboard, contentDescription = null, tint = cs.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    Text(
                        "  " + stringResource(Res.string.doc_grid_footer_hint),
                        style = MaterialTheme.typography.labelMedium,
                        color = cs.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun Header(text: String, modifier: Modifier, align: TextAlign = TextAlign.Start) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = align,
        maxLines = 1, overflow = TextOverflow.Ellipsis,
        modifier = modifier.padding(vertical = 8.dp, horizontal = 4.dp),
    )
}

@Composable
private fun GridRow(
    line: DocLineUi,
    unitChoices: List<DocUnitChoiceUi>,
    variantChoices: List<DocVariantChoiceUi>,
    onLoadUnitChoices: (String) -> Unit,
    onLoadVariantChoices: (String) -> Unit,
    onSelectUnit: (String, String) -> Unit,
    onSelectVariant: (String, String) -> Unit,
    onQuantity: (String, Double) -> Unit,
    onUnitPrice: (String, Double) -> Unit,
    onDiscount: (String, DiscountKind, Double) -> Unit,
    onRemove: (String) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    var unitMenu by remember { mutableStateOf(false) }
    var variantMenu by remember { mutableStateOf(false) }

    Column {
        Row(
            Modifier.widthIn(min = 860.dp).padding(horizontal = 8.dp).heightIn(min = 52.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Product
            Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
                Text(
                    line.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stringResource(Res.string.doc_line_hsn, line.hsn.ifBlank { stringResource(Res.string.doc_line_none) }),
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            // Variant
            Box(Modifier.width(92.dp)) {
                if (line.hasVariants) {
                    CellButton(
                        label = line.variantSku ?: stringResource(Res.string.doc_line_none),
                        onClick = { onLoadVariantChoices(line.id); variantMenu = true },
                    )
                    DropdownMenu(expanded = variantMenu, onDismissRequest = { variantMenu = false }) {
                        variantChoices.forEach { v ->
                            DropdownMenuItem(
                                text = { VariantMenuRow(v, selected = v.sku == line.variantSku) },
                                onClick = { variantMenu = false; onSelectVariant(line.id, v.sku) },
                            )
                        }
                    }
                } else {
                    Text(
                        stringResource(Res.string.doc_line_none),
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }
            // Unit
            Box(Modifier.width(92.dp)) {
                CellButton(
                    label = line.unitName.ifBlank { stringResource(Res.string.doc_line_none) },
                    mono = true,
                    onClick = { onLoadUnitChoices(line.id); unitMenu = true },
                )
                DropdownMenu(expanded = unitMenu, onDismissRequest = { unitMenu = false }) {
                    unitChoices.forEach { u ->
                        DropdownMenuItem(
                            text = { UnitMenuRow(u, selected = u.unitId == lineUnitKey(line)) },
                            onClick = { unitMenu = false; onSelectUnit(line.id, u.unitId) },
                        )
                    }
                }
            }
            // Qty
            NumberCell(
                value = line.quantity,
                modifier = Modifier.width(64.dp),
                onCommit = { onQuantity(line.id, it) },
            )
            // Unit price
            NumberCell(
                value = line.unitPrice,
                modifier = Modifier.width(96.dp),
                onCommit = { onUnitPrice(line.id, it) },
            )
            // Discount: %/₹ toggle + value
            Row(Modifier.width(110.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = cs.surfaceContainerHigh,
                    modifier = Modifier.clickable {
                        onDiscount(
                            line.id,
                            if (line.discountKind == DiscountKind.PERCENT) DiscountKind.FLAT else DiscountKind.PERCENT,
                            line.discountAmount,
                        )
                    },
                ) {
                    Text(
                        if (line.discountKind == DiscountKind.PERCENT) "%" else "₹",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    )
                }
                NumberCell(
                    value = line.discountAmount,
                    emptyWhenZero = true,
                    modifier = Modifier.weight(1f),
                    onCommit = { onDiscount(line.id, line.discountKind, it) },
                )
            }
            // Taxable
            MonoCell(line.taxable.toInr(), Modifier.width(92.dp))
            // GST chip
            Box(Modifier.width(60.dp), contentAlignment = Alignment.Center) {
                Surface(color = cs.surfaceContainerHigh, shape = RoundedCornerShape(50)) {
                    Text(
                        if (line.exempt) stringResource(Res.string.doc_line_exempt) else ratePctLabel(line.gstRatePercent ?: 0.0),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        maxLines = 1,
                    )
                }
            }
            // Line total
            MonoCell(line.lineTotal.toInr(), Modifier.width(100.dp), bold = true)
            // Remove
            IconButton(onClick = { onRemove(line.id) }, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(Res.string.doc_line_remove_cd),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        HorizontalDivider(color = cs.outlineVariant.copy(alpha = 0.6f))
    }
}

/** The unit key currently shown on a line — by name because unitId may be blank for legacy rows. */
private fun lineUnitKey(line: DocLineUi): String = line.unitName

@Composable
internal fun UnitMenuRow(u: DocUnitChoiceUi, selected: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            u.name + if (u.isBase) "  (base)" else "",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else null,
            modifier = Modifier.weight(1f),
        )
        Text(
            "×${u.multiplier.toDecimal()} · ${u.priceAtUnit.toInr()}",
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}

@Composable
internal fun VariantMenuRow(v: DocVariantChoiceUi, selected: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            v.label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else null,
            modifier = Modifier.weight(1f),
        )
        Text(
            v.price.toInr(),
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}

@Composable
private fun CellButton(label: String, onClick: () -> Unit, mono: Boolean = false) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = cs.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, cs.outlineVariant),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = if (mono) FontFamily.Monospace else null,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Icon(Icons.Filled.ExpandMore, contentDescription = null, modifier = Modifier.size(16.dp), tint = cs.onSurfaceVariant)
        }
    }
}

@Composable
private fun MonoCell(text: String, modifier: Modifier, bold: Boolean = false) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        fontFamily = FontFamily.Monospace,
        fontWeight = if (bold) FontWeight.Medium else null,
        textAlign = TextAlign.End,
        maxLines = 1,
        modifier = modifier.padding(horizontal = 4.dp),
    )
}

/**
 * Inline numeric grid cell — local text while focused (so recalc doesn't fight typing),
 * external value re-synced when focus leaves.
 */
@Composable
internal fun NumberCell(
    value: Double,
    onCommit: (Double) -> Unit,
    modifier: Modifier = Modifier,
    emptyWhenZero: Boolean = false,
) {
    val cs = MaterialTheme.colorScheme
    fun fmt(v: Double) = if (emptyWhenZero && v == 0.0) "" else v.toDecimal()
    var text by remember { mutableStateOf(fmt(value)) }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(value, focused) { if (!focused) text = fmt(value) }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = cs.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (focused) cs.primary else cs.outlineVariant),
        modifier = modifier.padding(horizontal = 2.dp),
    ) {
        BasicTextField(
            value = text,
            onValueChange = { raw ->
                val filtered = raw.filter { it.isDigit() || it == '.' }
                text = filtered
                filtered.toDoubleOrNull()?.let(onCommit) ?: if (filtered.isEmpty()) onCommit(0.0) else Unit
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = cs.onSurface,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.End,
            ),
            cursorBrush = SolidColor(cs.primary),
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 7.dp)
                .onFocusChanged { focused = it.isFocused },
        )
    }
}
