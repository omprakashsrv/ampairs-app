package com.ampairs.invoice.ui

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.ampairs.invoice.viewmodel.InvoiceViewViewModel
import com.ampairs.invoice.viewmodel.InvoicesViewModel
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun InvoicePaneScreen(
    onInvoiceEdit: (invoiceId: String?) -> Unit,
    onOpenOrder: (orderId: String) -> Unit = {},
    invoicesViewModel: InvoicesViewModel = metroViewModel(),
) {
    val navigator = rememberListDetailPaneScaffoldNavigator<String>()
    val scope = rememberCoroutineScope()

//    BackHandler(navigator.canNavigateBack()) {
//        navigator.navigateBack()
//    }

    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            AnimatedPane(Modifier) {
                InvoicesScreen(
                    viewModel = invoicesViewModel,
                    onInvoiceSelected = { selectedInvoiceId ->
                        scope.launch {
                            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, selectedInvoiceId)
                        }
                    },
                    onCreateInvoice = { onInvoiceEdit(null) },
                    selectedInvoiceId = navigator.currentDestination?.contentKey,
                    expanded = navigator.scaffoldDirective.maxHorizontalPartitions > 1,
                )
            }
        },
        detailPane = {
            AnimatedPane(Modifier) {
                val invoiceId = navigator.currentDestination?.contentKey ?: ""
                InvoiceViewScreen(
                    invoiceId = invoiceId,
                    // key by id — without it the pane reuses the first document's ViewModel
                    // for every subsequent row selection
                    viewModel = assistedMetroViewModel<InvoiceViewViewModel, InvoiceViewViewModel.Factory>(key = invoiceId) { create(invoiceId) },
                    onOpenOrder = onOpenOrder,
                    onEdit = { id -> onInvoiceEdit(id) },
                    onNavigateBack = {
                        if (navigator.canNavigateBack()) {
                            scope.launch { navigator.navigateBack() }
                        }
                    }
                )
            }
        }
    )
}
