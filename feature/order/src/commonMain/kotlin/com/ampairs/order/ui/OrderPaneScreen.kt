package com.ampairs.order.ui

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun OrderPaneScreen(onOrderEdit: (orderId: String?) -> Unit) {
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
                OrdersScreen { selectedOrderId ->
                    scope.launch {
                        navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, selectedOrderId)
                    }
                }
            }
        },
        detailPane = {
            AnimatedPane(Modifier) {
                val orderId = navigator.currentDestination?.contentKey ?: ""
                OrderViewScreen(orderId) { orderId ->
                    if (!orderId.isNullOrEmpty()) {
                        onOrderEdit(orderId)
                    } else {
                        if (navigator.canNavigateBack()) {
                            scope.launch {
                                navigator.navigateBack()
                            }
                        }
                    }
                }
            }
        }
    )
}
