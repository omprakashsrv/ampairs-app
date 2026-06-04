package com.ampairs.ecom.ui.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
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
import ampairsapp.feature.ecom.generated.resources.ecom_result_count
import ampairsapp.feature.ecom.generated.resources.ecom_view_cart
import com.ampairs.ecom.domain.firstImageUrl
import com.ampairs.ecom.ui.components.ProductGridCard
import com.ampairs.ecom.ui.components.StickyCartBar
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import org.jetbrains.compose.resources.stringResource

@Composable
fun DrillDownScreen(
    args: DrillDownArgs,
    snackbar: SnackbarHostState,
    onBack: () -> Unit,
    onOpenProduct: (String) -> Unit,
    onOpenCart: () -> Unit,
    viewModel: DrillDownViewModel =
        assistedMetroViewModel<DrillDownViewModel, DrillDownViewModel.Factory>(key = args.key) { create(args) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.messages.collect { snackbar.showSnackbar(it) } }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(end = 8.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(state.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        }

        Text(
            stringResource(Res.string.ecom_result_count, state.products.size),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
        )

        if (state.refineOptions.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.refineOptions, key = { it }) { option ->
                    RefineChip(option, selected = state.activeRefine == option, onClick = { viewModel.toggleRefine(option) })
                }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(state.products, key = { it.product.uid }) { row ->
                val p = row.product
                ProductGridCard(
                    name = p.name,
                    brand = p.brand,
                    unit = p.unit,
                    price = p.price,
                    mrp = p.mrp,
                    stockStatus = p.stock_status,
                    imageUrl = p.image_urls.firstImageUrl(),
                    quantityInCart = row.qtyInCart,
                    onOpen = { onOpenProduct(p.uid) },
                    onAdd = { viewModel.setQuantity(p.uid, 1) },
                    onIncrement = { viewModel.setQuantity(p.uid, row.qtyInCart + 1) },
                    onDecrement = { viewModel.setQuantity(p.uid, row.qtyInCart - 1) },
                )
            }
        }

        if (state.cartCount > 0) {
            StickyCartBar(
                itemCount = state.cartCount,
                total = state.cartTotal,
                viewCartLabel = stringResource(Res.string.ecom_view_cart),
                onClick = onOpenCart,
            )
        }
    }
}

@Composable
private fun RefineChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow
    val fg = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        color = fg,
        modifier = Modifier
            .background(bg, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    )
}
