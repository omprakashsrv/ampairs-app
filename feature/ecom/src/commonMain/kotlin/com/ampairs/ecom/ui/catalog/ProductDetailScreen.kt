package com.ampairs.ecom.ui.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ampairsapp.feature.ecom.generated.resources.Res
import ampairsapp.feature.ecom.generated.resources.ecom_add_to_cart
import ampairsapp.feature.ecom.generated.resources.ecom_out_of_stock
import ampairsapp.feature.ecom.generated.resources.ecom_save_amount
import ampairsapp.feature.ecom.generated.resources.ecom_view_cart
import coil3.compose.AsyncImage
import com.ampairs.ecom.api.model.StockStatus
import com.ampairs.ecom.domain.asRupee
import com.ampairs.ecom.domain.firstImageUrl
import com.ampairs.ecom.domain.savingsOrNull
import com.ampairs.ecom.ui.components.QuickAddControl
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import org.jetbrains.compose.resources.stringResource

@Composable
fun ProductDetailScreen(
    productId: String,
    snackbar: SnackbarHostState,
    onBack: () -> Unit,
    onOpenCart: () -> Unit,
    viewModel: ProductDetailViewModel =
        assistedMetroViewModel<ProductDetailViewModel, ProductDetailViewModel.Factory>(key = productId) { create(productId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.messages.collect { snackbar.showSnackbar(it) } }

    val product = state.product
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(end = 8.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text(product?.name.orEmpty(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), maxLines = 1)
        }

        if (product == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("…") }
            return
        }

        val outOfStock = product.stock_status == StockStatus.OUT_OF_STOCK.name

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Box(
                modifier = Modifier.fillMaxWidth().aspectRatio(1.1f).background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                val img = product.image_urls.firstImageUrl()
                if (img.isNullOrBlank()) Icon(Icons.Filled.Image, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(72.dp))
                else AsyncImage(model = img, contentDescription = product.name, modifier = Modifier.fillMaxWidth().aspectRatio(1.1f))
            }

            Column(Modifier.padding(16.dp)) {
                if (!product.brand.isNullOrBlank()) {
                    Text(product.brand.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(product.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                if (!product.unit.isNullOrBlank()) {
                    Text(product.unit, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(product.price.asRupee(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    if (product.mrp != null && product.mrp > product.price) {
                        Text(product.mrp.asRupee(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textDecoration = TextDecoration.LineThrough)
                    }
                }
                savingsOrNull(product.price, product.mrp)?.let {
                    Text(stringResource(Res.string.ecom_save_amount, it.asRupee()), style = MaterialTheme.typography.labelLarge, color = com.ampairs.ecom.ui.components.EcomColors.OnSuccessGreen, modifier = Modifier.padding(top = 4.dp))
                }

                if (!product.description.isNullOrBlank()) {
                    HorizontalDivider(Modifier.padding(vertical = 16.dp))
                    Text(product.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Sticky action bar
        Surface(tonalElevation = 3.dp) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(product.price.asRupee(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (outOfStock) Text(stringResource(Res.string.ecom_out_of_stock), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                }
                if (state.cartCount > 0) {
                    Button(onClick = onOpenCart) { Text(stringResource(Res.string.ecom_view_cart)) }
                }
                if (state.qtyInCart > 0) {
                    QuickAddControl(
                        quantity = state.qtyInCart,
                        enabled = !outOfStock,
                        onAdd = { viewModel.setQuantity(1) },
                        onIncrement = { viewModel.setQuantity(state.qtyInCart + 1) },
                        onDecrement = { viewModel.setQuantity(state.qtyInCart - 1) },
                    )
                } else {
                    Button(onClick = { viewModel.setQuantity(1) }, enabled = !outOfStock) {
                        Text(stringResource(Res.string.ecom_add_to_cart))
                    }
                }
            }
        }
    }
}
