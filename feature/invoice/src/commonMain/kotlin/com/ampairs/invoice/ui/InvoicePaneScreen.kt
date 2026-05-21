package com.ampairs.invoice.ui

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ampairs.invoice.db.InvoiceRepository
import com.ampairs.invoice.viewmodel.InvoiceViewViewModel
import com.ampairs.invoice.viewmodel.InvoicesViewModel
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun InvoicePaneScreen(
    invoiceRepository: InvoiceRepository,
    onInvoiceEdit: (invoiceId: String?) -> Unit,
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
                    }
                )
            }
        },
        detailPane = {
            AnimatedPane(Modifier) {
                val invoiceId = navigator.currentDestination?.contentKey ?: ""
                val invoiceViewViewModel = viewModel(key = invoiceId) {
                    InvoiceViewViewModel(invoiceId, invoiceRepository)
                }
                InvoiceViewScreen(
                    invoiceId = invoiceId,
                    viewModel = invoiceViewViewModel,
                    onNavigateBack = { navigatedInvoiceId ->
                        if (!navigatedInvoiceId.isNullOrEmpty()) {
                            onInvoiceEdit(navigatedInvoiceId)
                        } else {
                            if (navigator.canNavigateBack()) {
                                scope.launch {
                                    navigator.navigateBack()
                                }
                            }
                        }
                    }
                )
            }
        }
    )
}
