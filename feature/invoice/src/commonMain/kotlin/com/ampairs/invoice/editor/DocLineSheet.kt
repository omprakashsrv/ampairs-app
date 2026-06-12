package com.ampairs.invoice.editor

import ampairsapp.feature.invoice.generated.resources.Res
import ampairsapp.feature.invoice.generated.resources.doc_cmd_hsn_gst
import ampairsapp.feature.invoice.generated.resources.doc_col_taxable
import ampairsapp.feature.invoice.generated.resources.doc_col_total
import ampairsapp.feature.invoice.generated.resources.doc_line_none
import ampairsapp.feature.invoice.generated.resources.doc_sheet_base_qty
import ampairsapp.feature.invoice.generated.resources.doc_sheet_change_product_cd
import ampairsapp.feature.invoice.generated.resources.doc_sheet_conv
import ampairsapp.feature.invoice.generated.resources.doc_sheet_conv_base
import ampairsapp.feature.invoice.generated.resources.doc_sheet_delete
import ampairsapp.feature.invoice.generated.resources.doc_sheet_discount
import ampairsapp.feature.invoice.generated.resources.doc_sheet_done
import ampairsapp.feature.invoice.generated.resources.doc_sheet_overridden
import ampairsapp.feature.invoice.generated.resources.doc_sheet_price
import ampairsapp.feature.invoice.generated.resources.doc_sheet_quantity
import ampairsapp.feature.invoice.generated.resources.doc_sheet_title
import ampairsapp.feature.invoice.generated.resources.doc_sheet_unit
import ampairsapp.feature.invoice.generated.resources.doc_sheet_variant
import ampairsapp.feature.invoice.generated.resources.doc_sheet_variant_none
import ampairsapp.feature.invoice.generated.resources.doc_tot_gst
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ampairs.common.format.toDecimal
import com.ampairs.product.domain.ProductSummary
import com.ampairs.common.format.toInr
import com.ampairs.tax.calculation.document.DiscountKind
import org.jetbrains.compose.resources.stringResource

/**
 * Compact per-line editor (spec 010 v2): ModalBottomSheet with product / variant / unit / qty /
 * overridable price / discount, a conversion hint and a live Taxable/GST/Line-total breakdown.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocLineSheet(
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
    productResults: List<ProductSummary>,
    productRatePercents: Map<String, Double?>,
    onSearchProducts: (String) -> Unit,
    onPickProduct: (lineId: String, productId: String) -> Unit,
    onCreateProduct: (String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    var productMenu by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(Res.string.doc_sheet_title), style = MaterialTheme.typography.titleMedium)

            // Product row → type-ahead popover anchored to the row (no takeover)
            Box {
                Surface(
                    color = cs.surfaceContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().clickable { productMenu = true },
                ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Inventory2, contentDescription = null, tint = cs.tertiary, modifier = Modifier.size(20.dp))
                    Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                        Text(line.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(
                            stringResource(
                                Res.string.doc_cmd_hsn_gst,
                                line.hsn.ifBlank { stringResource(Res.string.doc_line_none) },
                                ratePctLabel(line.gstRatePercent ?: 0.0),
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = cs.onSurfaceVariant,
                        )
                    }
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = stringResource(Res.string.doc_sheet_change_product_cd),
                        tint = cs.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
                }
                DocProductPickerPopover(
                    expanded = productMenu,
                    results = productResults,
                    ratePercents = productRatePercents,
                    onSearch = onSearchProducts,
                    onPick = { productId -> productMenu = false; onPickProduct(line.id, productId) },
                    onCreate = { typed -> productMenu = false; onCreateProduct(typed) },
                    onDismiss = { productMenu = false },
                )
            }

            // Variant
            if (line.hasVariants) {
                var variantMenu by remember { mutableStateOf(false) }
                FieldLabel(stringResource(Res.string.doc_sheet_variant))
                Box {
                    PickerField(
                        value = line.variantSku ?: stringResource(Res.string.doc_sheet_variant_none),
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
                }
            }

            // Unit + quantity
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    FieldLabel(stringResource(Res.string.doc_sheet_unit))
                    var unitMenu by remember { mutableStateOf(false) }
                    Box {
                        PickerField(
                            value = line.unitName.ifBlank { stringResource(Res.string.doc_line_none) },
                            mono = true,
                            onClick = { onLoadUnitChoices(line.id); unitMenu = true },
                        )
                        DropdownMenu(expanded = unitMenu, onDismissRequest = { unitMenu = false }) {
                            unitChoices.forEach { u ->
                                DropdownMenuItem(
                                    text = { UnitMenuRow(u, selected = u.name == line.unitName) },
                                    onClick = { unitMenu = false; onSelectUnit(line.id, u.unitId) },
                                )
                            }
                        }
                    }
                }
                Column(Modifier.weight(1f)) {
                    FieldLabel(stringResource(Res.string.doc_sheet_quantity))
                    NumberCell(value = line.quantity, onCommit = { onQuantity(line.id, it) }, modifier = Modifier.fillMaxWidth())
                }
            }

            // Conversion hint
            Surface(color = cs.surfaceContainer, shape = RoundedCornerShape(10.dp)) {
                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Straighten, contentDescription = null, tint = cs.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    Text(
                        "  " + conversionHint(line) + " " + stringResource(
                            Res.string.doc_sheet_base_qty,
                            line.baseQuantity.toDecimal(),
                            line.baseUnitName.ifBlank { line.unitName },
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.onSurfaceVariant,
                    )
                }
            }

            // Price + discount
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    FieldLabel(stringResource(Res.string.doc_sheet_price))
                    NumberCell(value = line.unitPrice, onCommit = { onUnitPrice(line.id, it) }, modifier = Modifier.fillMaxWidth())
                }
                Column(Modifier.weight(1f)) {
                    FieldLabel(stringResource(Res.string.doc_sheet_discount))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SingleChoiceSegmentedButtonRow {
                            listOf(DiscountKind.PERCENT to "%", DiscountKind.FLAT to "₹").forEachIndexed { i, (k, l) ->
                                SegmentedButton(
                                    selected = line.discountKind == k,
                                    onClick = { onDiscount(line.id, k, line.discountAmount) },
                                    shape = SegmentedButtonDefaults.itemShape(i, 2),
                                ) { Text(l) }
                            }
                        }
                        NumberCell(
                            value = line.discountAmount,
                            emptyWhenZero = true,
                            onCommit = { onDiscount(line.id, line.discountKind, it) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            if (line.priceOverridden) {
                Text(
                    stringResource(Res.string.doc_sheet_overridden, line.catalogUnitPrice.toInr()),
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.secondary,
                )
            }

            // Mini breakdown
            Surface(color = cs.surfaceContainerLow, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    BreakdownRow(stringResource(Res.string.doc_col_taxable), line.taxable.toInr())
                    BreakdownRow(
                        stringResource(Res.string.doc_tot_gst) + " " + ratePctLabel(line.gstRatePercent ?: 0.0),
                        line.totalTax.toInr(),
                    )
                    HorizontalDivider(Modifier.padding(vertical = 6.dp), color = cs.outlineVariant)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(Res.string.doc_col_total), style = MaterialTheme.typography.titleSmall)
                        Text(
                            line.lineTotal.toInr(),
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
            }

            // Footer actions
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = null, tint = cs.error, modifier = Modifier.size(18.dp))
                    Text("  " + stringResource(Res.string.doc_sheet_delete), color = cs.error)
                }
                Box(Modifier.weight(1f))
                Button(onClick = onDismiss) {
                    Text(stringResource(Res.string.doc_sheet_done))
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp).padding(start = 2.dp))
                }
            }
        }
    }
}

@Composable
private fun conversionHint(line: DocLineUi): String =
    if (line.unitMultiplier == 1.0) {
        stringResource(Res.string.doc_sheet_conv_base, line.decimalPlaces)
    } else {
        stringResource(
            Res.string.doc_sheet_conv,
            line.unitName,
            line.unitMultiplier.toDecimal(),
            line.baseUnitName.ifBlank { "×1" },
            line.unitPrice.toInr(),
            line.decimalPlaces,
        )
    }

@Composable
private fun FieldLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 4.dp),
    )
}

@Composable
private fun PickerField(value: String, onClick: () -> Unit, mono: Boolean = false) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = cs.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, cs.outlineVariant),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp).heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                value,
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = if (mono) FontFamily.Monospace else null,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            Icon(Icons.Filled.ExpandMore, contentDescription = null, tint = cs.onSurfaceVariant)
        }
    }
}

@Composable
private fun BreakdownRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
    }
}
