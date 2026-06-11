package com.ampairs.invoice.editor

import ampairsapp.feature.invoice.generated.resources.Res
import ampairsapp.feature.invoice.generated.resources.doc_save
import ampairsapp.feature.invoice.generated.resources.doc_tot_expand_cd
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.ampairs.common.format.toInr
import com.ampairs.customer.domain.CustomerListItem
import com.ampairs.invoice.ui.TotalsUi
import com.ampairs.product.domain.ProductSummary
import com.ampairs.tax.calculation.document.DiscountKind
import com.ampairs.tax.calculation.document.PriceMode
import org.jetbrains.compose.resources.stringResource

/** Everything the shared editor renders; assembled per recomposition from the ViewModel. */
data class DocEditorState(
    val isInvoice: Boolean,
    val customer: DocCustomerUi?,
    val dateLabel: String,
    val priceMode: PriceMode,
    val numberPreview: String?,
    val lines: List<DocLineUi>,
    val totals: TotalsUi,
    val composer: ComposerUiState,
    val overallDiscountKind: DiscountKind,
    val overallDiscountAmount: Double,
    val showDiscount: Boolean,
    val sync: DocSyncUi,
    val saving: Boolean,
    val unitChoices: List<DocUnitChoiceUi>,
    val variantChoices: List<DocVariantChoiceUi>,
    val customerResults: List<CustomerListItem>,
    val sellerStateCode: Int?,
    val productResults: List<ProductSummary>,
    val productRatePercents: Map<String, Double?>,
    val baseUnits: List<DocBaseUnitChoice>,
    val createHsnRatePercent: Double?,
)

/** All editor intents; the order/invoice screens bind these to their ViewModels. */
data class DocEditorActions(
    val composerQueryChanged: (String) -> Unit,
    val composerMoveHighlight: (Int) -> Unit,
    val composerCommitHighlighted: () -> Boolean,
    val composerCommitAt: (Int) -> Unit,
    val lineQuantity: (String, Double) -> Unit,
    val lineUnitPrice: (String, Double) -> Unit,
    val lineDiscount: (String, DiscountKind, Double) -> Unit,
    val lineRemove: (String) -> Unit,
    val loadUnitChoices: (String) -> Unit,
    val loadVariantChoices: (String) -> Unit,
    val selectUnit: (String, String) -> Unit,
    val selectVariant: (String, String) -> Unit,
    val changeLineProduct: (lineId: String, productId: String) -> Unit,
    val searchProducts: (String) -> Unit,
    val searchCustomers: (String) -> Unit,
    val selectCustomer: (String) -> Unit,
    val useWalkIn: (name: String, phone: String, gstin: String) -> Unit,
    val overallDiscount: (DiscountKind, Double) -> Unit,
    val resolveCreateHsn: (String) -> Unit,
    val createProduct: (name: String, price: Double, hsn: String, baseUnitId: String?) -> Unit,
    val save: () -> Unit,
    val retrySync: () -> Unit,
    val openSettings: () -> Unit,
)

private const val EXPANDED_MIN_WIDTH_DP = 840

/**
 * The adaptive v2 fast-entry editor (spec 010): command bar pinned above the line area on every
 * form factor; expanded shows the editable grid + a pinned right totals rail; compact shows line
 * cards + a collapsed bottom bar expanding to the full GST breakdown. Ctrl/Cmd+S saves.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocEditorScaffold(
    state: DocEditorState,
    actions: DocEditorActions,
    modifier: Modifier = Modifier,
) {
    var showCustomerPicker by remember { mutableStateOf(false) }
    var createPrefill by remember { mutableStateOf<String?>(null) }
    var editingLineId by remember { mutableStateOf<String?>(null) }
    var changingProductForLine by remember { mutableStateOf<String?>(null) }
    var totalsExpanded by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .onPreviewKeyEvent { e ->
                if (e.type == KeyEventType.KeyDown && e.key == Key.S && (e.isCtrlPressed || e.isMetaPressed)) {
                    actions.save(); true
                } else false
            },
    ) {
        val compact = maxWidth < EXPANDED_MIN_WIDTH_DP.dp

        if (compact) {
            Column(Modifier.fillMaxSize()) {
                DocEditorHeader(
                    customer = state.customer,
                    dateLabel = state.dateLabel,
                    priceMode = state.priceMode,
                    numberPreview = null,
                    onCustomerClick = { showCustomerPicker = true },
                    onOpenSettings = actions.openSettings,
                    compact = true,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
                DocCommandBar(
                    state = state.composer,
                    onQueryChange = actions.composerQueryChanged,
                    onMoveHighlight = actions.composerMoveHighlight,
                    onCommitHighlighted = { if (!actions.composerCommitHighlighted()) createPrefill = state.composer.typedWords },
                    onCommitAt = actions.composerCommitAt,
                    onCreate = { createPrefill = it },
                    compact = true,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                DocLineCards(
                    lines = state.lines,
                    onQuantity = actions.lineQuantity,
                    onEdit = { editingLineId = it },
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 8.dp),
                )
                CompactTotalsBar(
                    state = state,
                    onExpand = { totalsExpanded = true },
                    onSave = actions.save,
                    onRetry = actions.retrySync,
                )
            }
        } else {
            Row(Modifier.fillMaxSize()) {
                Column(Modifier.weight(1f)) {
                    DocEditorHeader(
                        customer = state.customer,
                        dateLabel = state.dateLabel,
                        priceMode = state.priceMode,
                        numberPreview = state.numberPreview,
                        onCustomerClick = { showCustomerPicker = true },
                        onOpenSettings = actions.openSettings,
                        compact = false,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                    DocCommandBar(
                        state = state.composer,
                        onQueryChange = actions.composerQueryChanged,
                        onMoveHighlight = actions.composerMoveHighlight,
                        onCommitHighlighted = { if (!actions.composerCommitHighlighted()) createPrefill = state.composer.typedWords },
                        onCommitAt = actions.composerCommitAt,
                        onCreate = { createPrefill = it },
                        compact = false,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    DocLineGrid(
                        lines = state.lines,
                        unitChoices = state.unitChoices,
                        variantChoices = state.variantChoices,
                        onLoadUnitChoices = actions.loadUnitChoices,
                        onLoadVariantChoices = actions.loadVariantChoices,
                        onSelectUnit = actions.selectUnit,
                        onSelectVariant = actions.selectVariant,
                        onQuantity = actions.lineQuantity,
                        onUnitPrice = actions.lineUnitPrice,
                        onDiscount = actions.lineDiscount,
                        onRemove = actions.lineRemove,
                        onChangeProduct = { changingProductForLine = it },
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp, vertical = 8.dp),
                    )
                }
                // pinned right rail (~330dp)
                Column(
                    Modifier
                        .width(330.dp)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    DocTotalsPanel(
                        totals = state.totals,
                        overallDiscountKind = state.overallDiscountKind,
                        overallDiscountAmount = state.overallDiscountAmount,
                        showOverallDiscount = state.showDiscount,
                        onOverallDiscount = actions.overallDiscount,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DocSyncChip(state.sync, onRetry = actions.retrySync)
                        Box(Modifier.weight(1f))
                        SaveButton(saving = state.saving, onSave = actions.save)
                    }
                }
            }
        }

        // ── overlays ──
        if (showCustomerPicker) {
            DocCustomerPicker(
                results = state.customerResults,
                sellerStateCode = state.sellerStateCode,
                onSearch = actions.searchCustomers,
                onSelect = { actions.selectCustomer(it); showCustomerPicker = false },
                onWalkIn = { n, p, g -> actions.useWalkIn(n, p, g); showCustomerPicker = false },
                onDismiss = { showCustomerPicker = false },
            )
        }
        createPrefill?.let { prefill ->
            DocCreateProduct(
                initialName = prefill,
                baseUnits = state.baseUnits,
                hsnRatePercent = state.createHsnRatePercent,
                onHsnChanged = actions.resolveCreateHsn,
                onCreate = { n, p, h, u -> actions.createProduct(n, p, h, u); createPrefill = null },
                onDismiss = { createPrefill = null },
                compact = compact,
            )
        }
        editingLineId?.let { lineId ->
            val line = state.lines.firstOrNull { it.id == lineId }
            if (line == null) {
                editingLineId = null
            } else {
                DocLineSheet(
                    line = line,
                    unitChoices = state.unitChoices,
                    variantChoices = state.variantChoices,
                    onLoadUnitChoices = actions.loadUnitChoices,
                    onLoadVariantChoices = actions.loadVariantChoices,
                    onSelectUnit = actions.selectUnit,
                    onSelectVariant = actions.selectVariant,
                    onQuantity = actions.lineQuantity,
                    onUnitPrice = actions.lineUnitPrice,
                    onDiscount = actions.lineDiscount,
                    onChangeProduct = { changingProductForLine = lineId },
                    onDelete = { actions.lineRemove(lineId); editingLineId = null },
                    onDismiss = { editingLineId = null },
                )
            }
        }
        changingProductForLine?.let { lineId ->
            DocProductPicker(
                results = state.productResults,
                ratePercents = state.productRatePercents,
                onSearch = actions.searchProducts,
                onPick = { productId ->
                    actions.changeLineProduct(lineId, productId)
                    changingProductForLine = null
                },
                onCreate = { typed ->
                    changingProductForLine = null
                    createPrefill = typed
                },
                onDismiss = { changingProductForLine = null },
            )
        }
        if (totalsExpanded) {
            ModalBottomSheet(onDismissRequest = { totalsExpanded = false }) {
                Column(Modifier.padding(horizontal = 12.dp).padding(bottom = 16.dp)) {
                    DocTotalsPanel(
                        totals = state.totals,
                        overallDiscountKind = state.overallDiscountKind,
                        overallDiscountAmount = state.overallDiscountAmount,
                        showOverallDiscount = state.showDiscount,
                        onOverallDiscount = actions.overallDiscount,
                    )
                }
            }
        }
    }
}

/** Compact collapsed totals bar: grand total + Save + sync chip; tap ▴ for the full breakdown. */
@Composable
private fun CompactTotalsBar(
    state: DocEditorState,
    onExpand: () -> Unit,
    onSave: () -> Unit,
    onRetry: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Surface(color = cs.surfaceContainer, tonalElevation = 3.dp) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp).heightIn(min = 56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onExpand) {
                Icon(Icons.Filled.ExpandLess, contentDescription = stringResource(Res.string.doc_tot_expand_cd))
            }
            Column(Modifier.weight(1f).clickable(onClick = onExpand)) {
                Text(
                    state.totals.grandTotal.toInr(),
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                )
                DocSyncChip(state.sync, onRetry = onRetry)
            }
            SaveButton(saving = state.saving, onSave = onSave)
        }
    }
}

@Composable
private fun SaveButton(saving: Boolean, onSave: () -> Unit) {
    Button(onClick = onSave, enabled = !saving) {
        if (saving) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Text(stringResource(Res.string.doc_save))
        }
    }
}
