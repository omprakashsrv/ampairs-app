package com.ampairs.tally.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun TallyScreen() {
    val viewModel = koinInject<TallyViewModel>()

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
    ) { contentPadding ->
        // Screen content
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.align(Alignment.Center)) {
                Button(onClick = {
                    viewModel.syncLedgers {
                        showMessage(scope, snackbarHostState, 1)
                    }
                }, modifier = Modifier.padding(8.dp)) {
                    Text("Sync Ledgers")
                }

                Button(onClick = {
                    viewModel.syncUnits {
                        showMessage(scope, snackbarHostState, 1)
                    }
                }, modifier = Modifier.padding(8.dp)) {
                    Text("Sync Units")
                }

                Button(
                    onClick = {
                        viewModel.syncStockGroups {
                            showMessage(scope, snackbarHostState, 1)
                        }
                    },
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text("Sync Stock Groups")
                }

                Button(
                    onClick = {
                        viewModel.syncStockCategories {
                            showMessage(scope, snackbarHostState, 1)
                        }
                    },
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text("Sync Stock Category")
                }

                Button(
                    onClick = {
                        viewModel.syncStockItems {
                            showMessage(scope, snackbarHostState, 1)
                        }
                    },
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text("Sync StockItem")
                }

                Button(
                    onClick = {
                        viewModel.syncInventoryStock {
                            showMessage(scope, snackbarHostState, 1)
                        }
                    },
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text("Sync Inventory Stock")
                }
            }

        }
    }


}

private fun showMessage(
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    it: Int
) {
    scope.launch {
        snackbarHostState.showSnackbar(message = if (it == 0) "Error" else "Updated : " + it)
    }
}