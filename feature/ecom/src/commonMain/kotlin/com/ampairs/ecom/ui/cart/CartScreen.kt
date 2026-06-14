package com.ampairs.ecom.ui.cart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ampairsapp.feature.ecom.generated.resources.Res
import ampairsapp.feature.ecom.generated.resources.ecom_bill_details
import ampairsapp.feature.ecom.generated.resources.ecom_cart_empty
import ampairsapp.feature.ecom.generated.resources.ecom_cart_title
import ampairsapp.feature.ecom.generated.resources.ecom_checkout
import ampairsapp.feature.ecom.generated.resources.ecom_delivery
import ampairsapp.feature.ecom.generated.resources.ecom_delivery_subtitle
import ampairsapp.feature.ecom.generated.resources.ecom_delivery_title
import ampairsapp.feature.ecom.generated.resources.ecom_free
import ampairsapp.feature.ecom.generated.resources.ecom_item_total_mrp
import ampairsapp.feature.ecom.generated.resources.ecom_product_discount
import ampairsapp.feature.ecom.generated.resources.ecom_save_amount
import ampairsapp.feature.ecom.generated.resources.ecom_to_pay
import coil3.compose.AsyncImage
import com.ampairs.common.locale.LocalAppLocale
import com.ampairs.common.locale.formatMoney
import com.ampairs.common.navigation.ScreenBackButton
import com.ampairs.ecom.ui.components.BillRow
import com.ampairs.ecom.ui.components.DeliveryStrip
import com.ampairs.ecom.ui.components.EcomDimens
import com.ampairs.ecom.ui.components.QuickAddControl
import com.ampairs.ecom.ui.components.SavingsBanner
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.stringResource

@Composable
fun CartScreen(
    snackbar: SnackbarHostState,
    onBack: () -> Unit,
    onCheckout: () -> Unit,
    viewModel: CartViewModel = metroViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val locale = LocalAppLocale.current
    LaunchedEffect(Unit) { viewModel.messages.collect { snackbar.showSnackbar(it) } }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(end = 8.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            ScreenBackButton(onClick = onBack, contentDescription = "Back")
            Text(stringResource(Res.string.ecom_cart_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
        }

        if (state.isEmpty) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(Res.string.ecom_cart_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return
        }

        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
            item { DeliveryStrip(stringResource(Res.string.ecom_delivery_title), stringResource(Res.string.ecom_delivery_subtitle)) }

            items(state.items, key = { it.uid }) { line ->
                CartLineRow(
                    name = line.product_name,
                    brand = line.brand,
                    unit = line.unit,
                    unitPrice = line.unit_price,
                    quantity = line.quantity,
                    imageUrl = line.primary_image_url,
                    onIncrement = { viewModel.setQuantity(line.listed_product_id, line.quantity + 1) },
                    onDecrement = {
                        if (line.quantity <= 1) viewModel.removeItem(line.uid)
                        else viewModel.setQuantity(line.listed_product_id, line.quantity - 1)
                    },
                )
            }

            if (state.savings > 0) {
                item {
                    SavingsBanner(
                        stringResource(Res.string.ecom_save_amount, formatMoney(state.savings, locale)),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
            }

            item {
                Column(
                    Modifier.padding(16.dp)
                        .background(MaterialTheme.colorScheme.surface, EcomDimens.cornerMd)
                        .padding(16.dp),
                ) {
                    Text(stringResource(Res.string.ecom_bill_details), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 8.dp))
                    BillRow(stringResource(Res.string.ecom_item_total_mrp), formatMoney(state.itemTotalMrp, locale))
                    if (state.savings > 0) BillRow(stringResource(Res.string.ecom_product_discount), "- ${formatMoney(state.savings, locale)}", discount = true)
                    BillRow(stringResource(Res.string.ecom_delivery), stringResource(Res.string.ecom_free))
                    BillRow(stringResource(Res.string.ecom_to_pay), formatMoney(state.subtotal, locale), emphasize = true)
                }
            }
        }

        Surface(tonalElevation = 3.dp) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(Res.string.ecom_to_pay), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatMoney(state.subtotal, locale), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Button(onClick = onCheckout) { Text(stringResource(Res.string.ecom_checkout)) }
            }
        }
    }
}

@Composable
private fun CartLineRow(
    name: String,
    brand: String?,
    unit: String?,
    unitPrice: Double,
    quantity: Int,
    imageUrl: String?,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
) {
    val locale = LocalAppLocale.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(56.dp).background(MaterialTheme.colorScheme.surfaceContainerHighest, EcomDimens.cornerSm),
            contentAlignment = Alignment.Center,
        ) {
            if (imageUrl.isNullOrBlank()) Icon(Icons.Filled.Image, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(26.dp))
            else AsyncImage(model = imageUrl, contentDescription = name, modifier = Modifier.size(56.dp))
        }
        Column(Modifier.weight(1f)) {
            if (!brand.isNullOrBlank()) Text(brand.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            if (!unit.isNullOrBlank()) Text(unit, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatMoney(unitPrice * quantity, locale), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
        }
        QuickAddControl(quantity = quantity, enabled = true, onAdd = onIncrement, onIncrement = onIncrement, onDecrement = onDecrement)
    }
}
