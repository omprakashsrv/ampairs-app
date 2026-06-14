package com.ampairs.invoice.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.ampairs.invoice.editor.DocEditorActions
import com.ampairs.invoice.editor.DocEditorScaffold
import com.ampairs.invoice.editor.DocEditorState
import com.ampairs.invoice.viewmodel.InvoiceViewModel
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel

/**
 * Invoice editor — the v2 "fast entry" surface (spec 010). All behavior lives in the shared
 * editor (`com.ampairs.invoice.editor`) + [InvoiceViewModel]; this screen only binds them.
 */
@Composable
fun InvoiceScreen(
    customerId: String?,
    id: String?,
    onInvoiceSaved: (String) -> Unit,
    onOpenSettings: () -> Unit = {},
    // Legacy slot kept for source compatibility; the composer replaced the embedded picker.
    productPickerSlot: @Composable (onProductClick: (String) -> Unit) -> Unit = {},
    viewModel: InvoiceViewModel = assistedMetroViewModel<InvoiceViewModel, InvoiceViewModel.Factory> { create(customerId, id) }
) {
    val state = DocEditorState(
        isInvoice = true,
        customer = viewModel.customerUi,
        dateLabel = viewModel.dateLabel,
        priceMode = viewModel.priceMode,
        numberPreview = viewModel.numberPreview,
        lines = viewModel.lineUis,
        totals = viewModel.totals,
        composer = viewModel.composer,
        overallDiscountKind = viewModel.overallDiscountKind,
        overallDiscountAmount = viewModel.overallDiscountAmount,
        showDiscount = viewModel.showDiscount,
        sync = viewModel.syncUi,
        saving = viewModel.savingInvoice,
        unitChoices = viewModel.unitChoices,
        variantChoices = viewModel.variantChoices,
        customerResults = viewModel.customerResults,
        sellerStateCode = viewModel.sellerStateCode,
        productResults = viewModel.productResults,
        productRatePercents = viewModel.productRatePercents,
        baseUnits = viewModel.baseUnits,
        createHsnRatePercent = viewModel.createHsnRatePercent,
    )
    val actions = remember(viewModel, onInvoiceSaved, onOpenSettings) {
        DocEditorActions(
            composerQueryChanged = viewModel::composerQueryChanged,
            composerMoveHighlight = viewModel::composerMoveHighlight,
            composerCommitHighlighted = viewModel::composerCommitHighlighted,
            composerCommitAt = viewModel::composerCommitAt,
            lineQuantity = viewModel::setLineQuantity,
            lineUnitPrice = viewModel::setLineUnitPrice,
            lineDiscount = viewModel::setLineDiscount,
            lineRemove = viewModel::removeLine,
            loadUnitChoices = viewModel::loadUnitChoicesFor,
            loadVariantChoices = viewModel::loadVariantChoicesFor,
            selectUnit = viewModel::selectUnitFor,
            selectVariant = viewModel::selectVariantFor,
            changeLineProduct = viewModel::changeLineProduct,
            searchProducts = viewModel::searchProducts,
            searchCustomers = viewModel::searchCustomers,
            selectCustomer = viewModel::selectCustomer,
            useWalkIn = viewModel::useWalkInCustomer,
            overallDiscount = viewModel::setOverallDiscount,
            resolveCreateHsn = viewModel::resolveCreateHsn,
            createProduct = viewModel::createProductInline,
            save = { viewModel.saveInvoice(onInvoiceSaved) },
            retrySync = viewModel::retrySync,
            openSettings = onOpenSettings,
        )
    }
    DocEditorScaffold(state = state, actions = actions, modifier = Modifier)
}
