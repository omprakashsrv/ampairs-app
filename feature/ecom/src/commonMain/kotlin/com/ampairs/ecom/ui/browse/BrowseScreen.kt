package com.ampairs.ecom.ui.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ampairsapp.feature.ecom.generated.resources.Res
import ampairsapp.feature.ecom.generated.resources.ecom_search_placeholder
import ampairsapp.feature.ecom.generated.resources.ecom_shop_by_brand
import ampairsapp.feature.ecom.generated.resources.ecom_shop_by_category
import ampairsapp.feature.ecom.generated.resources.ecom_popular
import ampairsapp.feature.ecom.generated.resources.ecom_shop_empty_body
import ampairsapp.feature.ecom.generated.resources.ecom_shop_empty_title
import ampairsapp.feature.ecom.generated.resources.ecom_shop_offline_body
import ampairsapp.feature.ecom.generated.resources.ecom_shop_offline_title
import ampairsapp.feature.ecom.generated.resources.ecom_shop_retry
import ampairsapp.feature.ecom.generated.resources.ecom_view_cart
import com.ampairs.ecom.domain.firstImageUrl
import com.ampairs.ecom.ui.components.BrandChip
import com.ampairs.ecom.ui.components.CategoryTile
import com.ampairs.ecom.ui.components.EcomSearchBar
import com.ampairs.ecom.ui.components.ProductGridCard
import com.ampairs.ecom.ui.components.SectionTitle
import com.ampairs.ecom.ui.components.StickyCartBar
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.stringResource

@Composable
fun BrowseScreen(
    snackbar: SnackbarHostState,
    onOpenSearch: () -> Unit,
    onOpenCart: () -> Unit,
    onOpenCategory: (String) -> Unit,
    onOpenBrand: (String) -> Unit,
    onOpenProduct: (String) -> Unit,
    viewModel: BrowseViewModel = metroViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbar.showSnackbar(it) }
    }

    Column(Modifier.fillMaxWidth()) {
        // Top app bar: store name + cart
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                state.storeName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onOpenCart) {
                BadgedBox(badge = { if (state.cartCount > 0) Badge { Text("${state.cartCount}") } }) {
                    Icon(Icons.Filled.ShoppingCart, contentDescription = "Cart")
                }
            }
        }

        // Nothing cached yet → show a loading / offline-retry / empty state instead of blank sections.
        if (state.isEmpty) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                when {
                    state.loadFailed -> ShopStateMessage(
                        icon = Icons.Filled.CloudOff,
                        title = stringResource(Res.string.ecom_shop_offline_title),
                        body = stringResource(Res.string.ecom_shop_offline_body),
                        onRetry = viewModel::refresh,
                    )
                    !state.settled -> CircularProgressIndicator()
                    else -> ShopStateMessage(
                        icon = Icons.Filled.Inventory2,
                        title = stringResource(Res.string.ecom_shop_empty_title),
                        body = stringResource(Res.string.ecom_shop_empty_body),
                        onRetry = viewModel::refresh,
                    )
                }
            }
        } else {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            EcomSearchBar(
                placeholder = stringResource(Res.string.ecom_search_placeholder),
                modifier = Modifier.padding(horizontal = 16.dp),
                onClick = onOpenSearch,
            )

            // Shop by category
            SectionTitle(stringResource(Res.string.ecom_shop_by_category))
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxWidth().heightIn(max = 1000.dp).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                userScrollEnabled = false,
            ) {
                items(state.categories, key = { it.uid }) { cat ->
                    CategoryTile(cat.name, cat.product_count, cat.image_url, onClick = { onOpenCategory(cat.name) })
                }
            }

            // Shop by brand
            if (state.brands.isNotEmpty()) {
                SectionTitle(stringResource(Res.string.ecom_shop_by_brand))
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(state.brands, key = { it.uid }) { brand ->
                        BrandChip(brand.name, onClick = { onOpenBrand(brand.name) })
                    }
                }
            }

            // Popular
            SectionTitle(stringResource(Res.string.ecom_popular))
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 170.dp),
                modifier = Modifier.fillMaxWidth().heightIn(max = 2000.dp).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                userScrollEnabled = false,
            ) {
                items(state.popular, key = { it.product.uid }) { row ->
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
            Box(Modifier.height(16.dp))
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
private fun ShopStateMessage(
    icon: ImageVector,
    title: String,
    body: String,
    onRetry: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
        Button(onClick = onRetry, modifier = Modifier.padding(top = 20.dp)) {
            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(stringResource(Res.string.ecom_shop_retry), modifier = Modifier.padding(start = 8.dp))
        }
    }
}
