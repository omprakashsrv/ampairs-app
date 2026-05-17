package com.ampairs.common.components

import ampairsapp.composeapp.generated.resources.Res
import ampairsapp.composeapp.generated.resources.ic_add
import ampairsapp.composeapp.generated.resources.ic_remove
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalResourceApi::class)
@Composable
fun CartItem(
    id: String,
    qty: Double,
    width: Dp = 30.dp,
    onQuantityUpdated: (Double) -> Unit,
) {
    var quantity by remember(id) { mutableDoubleStateOf(qty) }
    var hasFraction by remember(id) { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Display quantity and controls
        IconButton(onClick = {
            if (quantity >= 1) quantity--
            onQuantityUpdated(quantity)
        }) {
            Icon(
                painter = painterResource(Res.drawable.ic_remove),
                contentDescription = "Decrease Quantity",
                modifier = Modifier.width(16.dp).height(16.dp)
            )
        }
        BasicTextField(
            value = if (quantity == 0.0) "" else (if (quantity - quantity.toInt() > 0 || hasFraction) "$quantity" else "${quantity.toInt()}"),
            onValueChange = {
                quantity = if (it.isEmpty()) 0.0 else try {
                    if (hasFraction && !it.contains(".")) quantity.toInt()
                        .toDouble() else it.toDouble()
                } catch (e: Exception) {
                    quantity
                }
                hasFraction = it.contains(".")
                onQuantityUpdated(quantity)
            },
            maxLines = 1,
            textStyle = MaterialTheme.typography.labelSmall,
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Decimal),
            modifier = Modifier
                .requiredWidth(width)
                .width(IntrinsicSize.Min),
        )
        IconButton(onClick = {
            quantity++
            onQuantityUpdated(quantity)
        }) {
            Icon(
                painter = painterResource(Res.drawable.ic_add),
                contentDescription = "Increase Quantity",
                modifier = Modifier.width(16.dp).height(16.dp)
            )
        }
    }

}