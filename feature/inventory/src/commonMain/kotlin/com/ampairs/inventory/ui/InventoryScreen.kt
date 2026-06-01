package com.ampairs.inventory.ui

import ampairsapp.feature.inventory.generated.resources.Res
import ampairsapp.feature.inventory.generated.resources.inv_form_edit_title
import ampairsapp.feature.inventory.generated.resources.inv_form_label_buying_price
import ampairsapp.feature.inventory.generated.resources.inv_form_label_dp
import ampairsapp.feature.inventory.generated.resources.inv_form_label_mrp
import ampairsapp.feature.inventory.generated.resources.inv_form_label_name
import ampairsapp.feature.inventory.generated.resources.inv_form_label_selling_price
import ampairsapp.feature.inventory.generated.resources.inv_form_label_stock
import ampairsapp.feature.inventory.generated.resources.inv_form_new_title
import ampairsapp.feature.inventory.generated.resources.inv_form_save_new
import ampairsapp.feature.inventory.generated.resources.inv_form_save_update
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ampairs.inventory.domain.Inventory
import com.ampairs.inventory.viewmodel.InventoryViewModel
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import org.jetbrains.compose.resources.stringResource

@Composable
fun InventoryScreen(
    modifier: Modifier = Modifier,
    id: String?,
    onInventoryUpdate: (String) -> Unit
) {
    val viewModel = assistedMetroViewModel<InventoryViewModel, InventoryViewModel.Factory>(key = id.orEmpty()) { create(id) }

    if (id.isNullOrEmpty()) {
        viewModel.inventory = InventoryState(Inventory())
    }

    val inventoryState = viewModel.inventory

    if (!id.isNullOrEmpty() && inventoryState.inventory.id.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        viewModel.reSyncInventory(id)
    } else {
        InventoryFormContent(
            modifier = modifier,
            isNew = id.isNullOrEmpty(),
            inventoryState = inventoryState,
            isLoading = viewModel.loading,
            onSave = {
                val updatedId = viewModel.updateInventory()
                onInventoryUpdate(updatedId)
            }
        )
    }
}

@Composable
private fun InventoryFormContent(
    modifier: Modifier = Modifier,
    isNew: Boolean,
    inventoryState: InventoryState,
    isLoading: Boolean,
    onSave: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Section header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (isNew) stringResource(Res.string.inv_form_new_title) else stringResource(Res.string.inv_form_edit_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            )
        }

        // Form fields
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(Res.string.inv_form_label_name)) },
                value = inventoryState.description,
                maxLines = 1,
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text),
                onValueChange = { inventoryState.description = it },
                shape = MaterialTheme.shapes.medium
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(Res.string.inv_form_label_mrp)) },
                value = formatDecimalInput(inventoryState.mrp, inventoryState.mrpFraction),
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                onValueChange = {
                    inventoryState.mrp = parseDecimalInput(it, inventoryState.mrpFraction) { inventoryState.mrp }
                    inventoryState.mrpFraction = it.contains(".")
                },
                maxLines = 1,
                shape = MaterialTheme.shapes.medium
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(Res.string.inv_form_label_dp)) },
                value = formatDecimalInput(inventoryState.dp, inventoryState.dpFraction),
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                onValueChange = {
                    inventoryState.dp = parseDecimalInput(it, inventoryState.dpFraction) { inventoryState.dp }
                    inventoryState.dpFraction = it.contains(".")
                },
                maxLines = 1,
                shape = MaterialTheme.shapes.medium
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(Res.string.inv_form_label_selling_price)) },
                value = formatDecimalInput(inventoryState.sellingPrice, inventoryState.sellingPriceFraction),
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                onValueChange = {
                    inventoryState.sellingPrice = parseDecimalInput(it, inventoryState.sellingPriceFraction) { inventoryState.sellingPrice }
                    inventoryState.sellingPriceFraction = it.contains(".")
                },
                maxLines = 1,
                shape = MaterialTheme.shapes.medium
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(Res.string.inv_form_label_buying_price)) },
                value = formatDecimalInput(inventoryState.buyingPrice, inventoryState.buyingPriceFraction),
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                onValueChange = {
                    inventoryState.buyingPrice = parseDecimalInput(it, inventoryState.buyingPriceFraction) { inventoryState.buyingPrice }
                    inventoryState.buyingPriceFraction = it.contains(".")
                },
                maxLines = 1,
                shape = MaterialTheme.shapes.medium
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(Res.string.inv_form_label_stock)) },
                value = formatDecimalInput(inventoryState.stock, inventoryState.stockFraction),
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                onValueChange = {
                    inventoryState.stock = parseDecimalInput(it, inventoryState.stockFraction) { inventoryState.stock }
                    inventoryState.stockFraction = it.contains(".")
                },
                maxLines = 1,
                shape = MaterialTheme.shapes.medium
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onSave,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        if (isNew) stringResource(Res.string.inv_form_save_new)
                        else stringResource(Res.string.inv_form_save_update)
                    )
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}

private fun formatDecimalInput(value: Double, hasFraction: Boolean): String {
    if (value == 0.0) return ""
    return if (value - value.toInt() > 0 || hasFraction) "$value" else "${value.toInt()}"
}

private fun parseDecimalInput(input: String, hasFraction: Boolean, current: () -> Double): Double {
    if (input.isEmpty()) return 0.0
    return try {
        if (hasFraction && !input.contains(".")) current().toInt().toDouble()
        else input.toDouble()
    } catch (e: Exception) {
        current()
    }
}
