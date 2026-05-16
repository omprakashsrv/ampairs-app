package com.ampairs.inventory.ui

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
fun InventoryPaneScreen() {

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
                InventoryListScreen(onNewInventory = {
                    scope.launch {
                        navigator.navigateTo(
                            ListDetailPaneScaffoldRole.Detail,
                            ""
                        )
                    }
                }, onInventorySelected = { inventoryId, _ ->
                    scope.launch {
                        navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, inventoryId)
                    }
                })
            }
        },
        detailPane = {
            AnimatedPane(Modifier) {
                val inventoryId = navigator.currentDestination?.contentKey ?: ""
                InventoryScreen(modifier = Modifier, inventoryId) {}
            }
        }
    )


}
