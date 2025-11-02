package com.ampairs.inventory.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ampairs.inventory.domain.Inventory
import com.ampairs.inventory.viewmodel.InventoryViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf


@Composable
fun InventoryScreen(
    modifier: Modifier = Modifier,
    id: String?,
    onInventoryUpdate: (String) -> Unit
) {
    val viewModel: InventoryViewModel = koinInject { parametersOf(id) }

    if (id.isNullOrEmpty()) {
        viewModel.inventory = InventoryState(Inventory())
    }
    val inventoryState = viewModel.inventory
    if (!id.isNullOrEmpty() && inventoryState.inventory.id.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
        }
        viewModel.reSyncInventory(id)
    } else {
        val inputModifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp).fillMaxWidth()
        Column(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LazyVerticalStaggeredGrid(
                modifier = Modifier.weight(1f),
                columns = StaggeredGridCells.Adaptive(320.dp),
                verticalItemSpacing = 4.dp,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                content = {
                    items(1) { index ->
                        when (index) {
                            0 -> {
                                Column(
                                    modifier = Modifier.padding(2.dp).fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        modifier = inputModifier,
                                        label = { Text("name") },
                                        value = inventoryState.description,
                                        maxLines = 1,
                                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text),
                                        onValueChange = {
                                            inventoryState.description = it
                                        },
                                    )
                                    OutlinedTextField(
                                        modifier = inputModifier,
                                        label = { Text("MRP") },
                                        value = if (inventoryState.mrp == 0.0) "" else (if (inventoryState.mrp - inventoryState.mrp.toInt() > 0 || inventoryState.mrpFraction) "${inventoryState.mrp}" else "${inventoryState.mrp.toInt()}"),
                                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                                        onValueChange = {
                                            inventoryState.mrp = if (it.isEmpty()) 0.0 else try {
                                                if (inventoryState.mrpFraction && !it.contains(".")) inventoryState.mrp.toInt()
                                                    .toDouble() else it.toDouble()
                                            } catch (e: Exception) {
                                                inventoryState.mrp
                                            }
                                            inventoryState.mrpFraction = it.contains(".")
                                        },
                                        maxLines = 1,
                                    )
                                    OutlinedTextField(
                                        modifier = inputModifier,
                                        label = { Text("DP") },
                                        value = if (inventoryState.dp == 0.0) "" else (if (inventoryState.dp - inventoryState.dp.toInt() > 0 || inventoryState.dpFraction) "${inventoryState.dp}" else "${inventoryState.dp.toInt()}"),
                                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                                        onValueChange = {
                                            inventoryState.dp = if (it.isEmpty()) 0.0 else try {
                                                if (inventoryState.dpFraction && !it.contains(".")) inventoryState.dp.toInt()
                                                    .toDouble() else it.toDouble()
                                            } catch (e: Exception) {
                                                inventoryState.dp
                                            }
                                            inventoryState.dpFraction = it.contains(".")
                                        },
                                        maxLines = 1,
                                    )
                                    OutlinedTextField(
                                        modifier = inputModifier,
                                        label = { Text("Selling Price") },
                                        value = if (inventoryState.sellingPrice == 0.0) "" else (if (inventoryState.sellingPrice - inventoryState.sellingPrice.toInt() > 0 || inventoryState.sellingPriceFraction) "${inventoryState.sellingPrice}" else "${inventoryState.sellingPrice.toInt()}"),
                                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                                        onValueChange = {
                                            inventoryState.sellingPrice =
                                                if (it.isEmpty()) 0.0 else try {
                                                    if (inventoryState.sellingPriceFraction && !it.contains(
                                                            "."
                                                        )
                                                    ) inventoryState.sellingPrice.toInt()
                                                        .toDouble() else it.toDouble()
                                                } catch (e: Exception) {
                                                    inventoryState.sellingPrice
                                                }
                                            inventoryState.sellingPriceFraction = it.contains(".")
                                        },
                                        maxLines = 1,
                                    )
                                    OutlinedTextField(
                                        modifier = inputModifier,
                                        label = { Text("Buying Price") },
                                        value = if (inventoryState.buyingPrice == 0.0) "" else (if (inventoryState.buyingPrice - inventoryState.buyingPrice.toInt() > 0 || inventoryState.buyingPriceFraction) "${inventoryState.buyingPrice}" else "${inventoryState.buyingPrice.toInt()}"),
                                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                                        onValueChange = {
                                            inventoryState.buyingPrice =
                                                if (it.isEmpty()) 0.0 else try {
                                                    if (inventoryState.buyingPriceFraction && !it.contains(
                                                            "."
                                                        )
                                                    ) inventoryState.buyingPrice.toInt()
                                                        .toDouble() else it.toDouble()
                                                } catch (e: Exception) {
                                                    inventoryState.buyingPrice
                                                }
                                            inventoryState.buyingPriceFraction = it.contains(".")
                                        },
                                        maxLines = 1,
                                    )
                                    OutlinedTextField(
                                        modifier = inputModifier,
                                        label = { Text("Stock") },
                                        value = if (inventoryState.stock == 0.0) "" else (if (inventoryState.stock - inventoryState.stock.toInt() > 0 || inventoryState.stockFraction) "${inventoryState.stock}" else "${inventoryState.stock.toInt()}"),
                                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                                        onValueChange = {
                                            inventoryState.stock = if (it.isEmpty()) 0.0 else try {
                                                if (inventoryState.stockFraction && !it.contains(".")) inventoryState.stock.toInt()
                                                    .toDouble() else it.toDouble()
                                            } catch (e: Exception) {
                                                inventoryState.stock
                                            }
                                            inventoryState.stockFraction = it.contains(".")
                                        },
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                },
            )

            Column(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ElevatedButton(onClick = {
                    val updatedInventoryId = viewModel.updateInventory()
                    onInventoryUpdate(updatedInventoryId)
                }) {
                    if (viewModel.loading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .progressSemantics()
                                .size(24.dp)
                        )
                    } else {
                        Text(if (viewModel.id.isNullOrEmpty()) "Create New" else "Update")
                    }
                }
            }
        }
    }
}