package com.ampairs.order.ui

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.ampairs.order.viewmodel.OrderViewViewModel
import com.ampairs.order.viewmodel.OrdersViewModel
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun OrderPaneScreen(
    onOrderEdit: (orderId: String?) -> Unit,
    onOpenInvoice: (invoiceId: String) -> Unit = {},
    ordersViewModel: OrdersViewModel = metroViewModel(),
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
                OrdersScreen(
                    onOrderSelected = { selectedOrderId ->
                        scope.launch {
                            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, selectedOrderId)
                        }
                    },
                    onCreateOrder = { onOrderEdit(null) },
                    selectedOrderId = navigator.currentDestination?.contentKey,
                    viewModel = ordersViewModel
                )
            }
        },
        detailPane = {
            AnimatedPane(Modifier) {
                val orderId = navigator.currentDestination?.contentKey ?: ""
                OrderViewScreen(
                    orderId = orderId,
                    onOpenInvoice = onOpenInvoice,
                    onEdit = { id -> onOrderEdit(id) },
                    onNavigateBack = {
                        if (navigator.canNavigateBack()) {
                            scope.launch { navigator.navigateBack() }
                        }
                    },
                    // key by id — without it the pane reuses the first document's ViewModel
                    // for every subsequent row selection
                    viewModel = assistedMetroViewModel<OrderViewViewModel, OrderViewViewModel.Factory>(key = orderId) { create(orderId) }
                )
            }
        }
    )
}
