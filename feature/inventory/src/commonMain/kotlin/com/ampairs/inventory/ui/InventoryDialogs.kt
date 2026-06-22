package com.ampairs.inventory.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ampairsapp.feature.inventory.generated.resources.Res
import ampairsapp.feature.inventory.generated.resources.cancel
import ampairsapp.feature.inventory.generated.resources.inv_adjust_apply
import ampairsapp.feature.inventory.generated.resources.inv_adjust_in
import ampairsapp.feature.inventory.generated.resources.inv_adjust_out
import ampairsapp.feature.inventory.generated.resources.inv_adjust_qty
import ampairsapp.feature.inventory.generated.resources.inv_adjust_reason
import ampairsapp.feature.inventory.generated.resources.inv_adjust_title
import ampairsapp.feature.inventory.generated.resources.inv_form_label_buying_price
import ampairsapp.feature.inventory.generated.resources.inv_form_label_name
import ampairsapp.feature.inventory.generated.resources.inv_form_label_reorder
import ampairsapp.feature.inventory.generated.resources.inv_form_label_selling_price
import ampairsapp.feature.inventory.generated.resources.inv_form_label_sku
import ampairsapp.feature.inventory.generated.resources.inv_form_label_stock
import ampairsapp.feature.inventory.generated.resources.inv_form_new_title
import ampairsapp.feature.inventory.generated.resources.save
import com.ampairs.inventory.domain.InventoryItem
import org.jetbrains.compose.resources.stringResource

@Composable
fun AddItemDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, sku: String, sellingPrice: Double, costPrice: Double, stock: Double, reorder: Double) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var sku by remember { mutableStateOf("") }
    var sellingPrice by remember { mutableStateOf("") }
    var costPrice by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("") }
    var reorder by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.inv_form_new_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text(stringResource(Res.string.inv_form_label_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(sku, { sku = it }, label = { Text(stringResource(Res.string.inv_form_label_sku)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(sellingPrice, { sellingPrice = it }, label = { Text(stringResource(Res.string.inv_form_label_selling_price)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(costPrice, { costPrice = it }, label = { Text(stringResource(Res.string.inv_form_label_buying_price)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(stock, { stock = it }, label = { Text(stringResource(Res.string.inv_form_label_stock)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(reorder, { reorder = it }, label = { Text(stringResource(Res.string.inv_form_label_reorder)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(
                        name.trim(),
                        sku.trim(),
                        sellingPrice.toDoubleOrNull() ?: 0.0,
                        costPrice.toDoubleOrNull() ?: 0.0,
                        stock.toDoubleOrNull() ?: 0.0,
                        reorder.toDoubleOrNull() ?: 0.0,
                    )
                },
            ) { Text(stringResource(Res.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } },
    )
}

@Composable
fun AdjustStockDialog(
    item: InventoryItem,
    onDismiss: () -> Unit,
    onApply: (quantity: Double, stockIn: Boolean, reason: String) -> Unit,
) {
    var quantity by remember { mutableStateOf("") }
    var stockIn by remember { mutableStateOf(true) }
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.inv_adjust_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(item.name)
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(if (stockIn) Res.string.inv_adjust_in else Res.string.inv_adjust_out), modifier = Modifier.weight(1f))
                    Switch(checked = stockIn, onCheckedChange = { stockIn = it })
                }
                OutlinedTextField(quantity, { quantity = it }, label = { Text(stringResource(Res.string.inv_adjust_qty)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(reason, { reason = it }, label = { Text(stringResource(Res.string.inv_adjust_reason)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                enabled = (quantity.toDoubleOrNull() ?: 0.0) > 0.0,
                onClick = { onApply(quantity.toDoubleOrNull() ?: 0.0, stockIn, reason.trim()) },
            ) { Text(stringResource(Res.string.inv_adjust_apply)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } },
    )
}
