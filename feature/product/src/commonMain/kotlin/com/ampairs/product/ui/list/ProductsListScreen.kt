package com.ampairs.product.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass
import coil3.compose.AsyncImage
import com.ampairs.common.components.FilterCategory
import com.ampairs.common.components.MultiSelectFilterSheet
import com.ampairs.common.locale.LocalAppLocale
import com.ampairs.common.locale.formatMoney
import com.ampairs.product.domain.ProductListItem
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.stringResource
import ampairsapp.feature.product.generated.resources.Res
import ampairsapp.feature.product.generated.resources.prod_list_title
import ampairsapp.feature.product.generated.resources.prod_list_count
import ampairsapp.feature.product.generated.resources.prod_list_new_product
import ampairsapp.feature.product.generated.resources.prod_list_cd_add
import ampairsapp.feature.product.generated.resources.prod_list_search_placeholder
import ampairsapp.feature.product.generated.resources.prod_list_cd_product_image
import ampairsapp.feature.product.generated.resources.prod_list_refreshing
import ampairsapp.feature.product.generated.resources.prod_list_empty_title
import ampairsapp.feature.product.generated.resources.prod_list_empty_desc
import ampairsapp.feature.product.generated.resources.prod_list_in_stock_format
import ampairsapp.feature.product.generated.resources.prod_list_col_name
import ampairsapp.feature.product.generated.resources.prod_list_col_code
import ampairsapp.feature.product.generated.resources.prod_list_col_category
import ampairsapp.feature.product.generated.resources.prod_list_col_price
import ampairsapp.feature.product.generated.resources.prod_list_col_stock
import ampairsapp.feature.product.generated.resources.prod_error_title
import ampairsapp.feature.product.generated.resources.prod_out_of_stock
import ampairsapp.feature.product.generated.resources.prod_inactive
import ampairsapp.feature.product.generated.resources.prod_retry
import ampairsapp.feature.product.generated.resources.prod_catalog_brands
import ampairsapp.feature.product.generated.resources.prod_catalog_categories
import ampairsapp.feature.product.generated.resources.prod_catalog_sub_categories
import ampairsapp.feature.product.generated.resources.prod_catalog_groups
import ampairsapp.feature.product.generated.resources.prod_master_data
import ampairsapp.feature.product.generated.resources.prod_filter_cd
import ampairsapp.feature.product.generated.resources.prod_filter_brand
import ampairsapp.feature.product.generated.resources.prod_filter_category
import ampairsapp.feature.product.generated.resources.prod_filter_sub_category
import ampairsapp.feature.product.generated.resources.prod_filter_group

private const val KEY_BRAND = "brand"
private const val KEY_CATEGORY = "category"
private const val KEY_SUB_CATEGORY = "sub_category"
private const val KEY_GROUP = "group"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsListScreen(
    onProductClick: (String) -> Unit,
    onCreateProduct: () -> Unit,
    onNavigateToBrands: () -> Unit = {},
    onNavigateToCategories: () -> Unit = {},
    onNavigateToSubCategories: () -> Unit = {},
    onNavigateToGroups: () -> Unit = {},
    /** Launch bulk web image auto-match for the given products (those missing images). */
    onAutoMatchImages: (List<ProductListItem>) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ProductsListViewModel = metroViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val isExpanded = windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)

    var showFilter by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.syncProducts()
    }

    if (showFilter) {
        val brandLabel = stringResource(Res.string.prod_filter_brand)
        val categoryLabel = stringResource(Res.string.prod_filter_category)
        val subCategoryLabel = stringResource(Res.string.prod_filter_sub_category)
        val groupLabel = stringResource(Res.string.prod_filter_group)
        MultiSelectFilterSheet(
            categories = listOf(
                FilterCategory(KEY_BRAND, brandLabel, uiState.brandOptions),
                FilterCategory(KEY_CATEGORY, categoryLabel, uiState.categoryOptions),
                FilterCategory(KEY_SUB_CATEGORY, subCategoryLabel, uiState.subCategoryOptions),
                FilterCategory(KEY_GROUP, groupLabel, uiState.groupOptions),
            ),
            selected = mapOf(
                KEY_BRAND to uiState.filter.brands,
                KEY_CATEGORY to uiState.filter.categories,
                KEY_SUB_CATEGORY to uiState.filter.subCategories,
                KEY_GROUP to uiState.filter.groups,
            ),
            onApply = { selection ->
                viewModel.applyFilter(
                    ProductFilter(
                        brands = selection[KEY_BRAND].orEmpty(),
                        categories = selection[KEY_CATEGORY].orEmpty(),
                        subCategories = selection[KEY_SUB_CATEGORY].orEmpty(),
                        groups = selection[KEY_GROUP].orEmpty(),
                    )
                )
            },
            onDismiss = { showFilter = false },
        )
    }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            if (!isExpanded) {
                ExtendedFloatingActionButton(
                    onClick = onCreateProduct,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(stringResource(Res.string.prod_list_new_product)) }
                )
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Header
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(Res.string.prod_list_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (uiState.products.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(Res.string.prod_list_count, uiState.products.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        if (isExpanded) {
                            IconButton(onClick = onCreateProduct) {
                                Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.prod_list_cd_add))
                            }
                        }
                        IconButton(onClick = {
                            viewModel.onFilterOpened()
                            showFilter = true
                        }) {
                            BadgedBox(
                                badge = {
                                    if (uiState.filter.activeCount > 0) {
                                        Badge { Text("${uiState.filter.activeCount}") }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.FilterList, contentDescription = stringResource(Res.string.prod_filter_cd))
                            }
                        }
                        // Catalog master-data shortcuts collapsed into an overflow menu to keep the
                        // header compact so the list can use the maximum vertical space.
                        Box {
                            var masterMenuOpen by remember { mutableStateOf(false) }
                            IconButton(onClick = { masterMenuOpen = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = stringResource(Res.string.prod_master_data))
                            }
                            DropdownMenu(
                                expanded = masterMenuOpen,
                                onDismissRequest = { masterMenuOpen = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(Res.string.prod_catalog_brands)) },
                                    onClick = { masterMenuOpen = false; onNavigateToBrands() }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(Res.string.prod_catalog_categories)) },
                                    onClick = { masterMenuOpen = false; onNavigateToCategories() }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(Res.string.prod_catalog_sub_categories)) },
                                    onClick = { masterMenuOpen = false; onNavigateToSubCategories() }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(Res.string.prod_catalog_groups)) },
                                    onClick = { masterMenuOpen = false; onNavigateToGroups() }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(Res.string.prod_auto_match_images)) },
                                    onClick = { masterMenuOpen = false; onAutoMatchImages(uiState.products) }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = viewModel::updateSearchQuery,
                        placeholder = { Text(stringResource(Res.string.prod_list_search_placeholder)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 8.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    )
                }
            }

            when {
                uiState.isLoading && uiState.products.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                uiState.error != null && uiState.products.isEmpty() -> {
                    ErrorMessage(
                        error = uiState.error!!,
                        onRetry = viewModel::syncProducts,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                uiState.products.isEmpty() -> {
                    EmptyState(onCreateProduct = onCreateProduct, modifier = Modifier.fillMaxSize())
                }

                else -> {
                    if (isExpanded) {
                        ProductTable(
                            products = uiState.products,
                            onProductClick = onProductClick,
                            isRefreshing = uiState.isRefreshing,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        ProductsList(
                            products = uiState.products,
                            onProductClick = onProductClick,
                            isRefreshing = uiState.isRefreshing,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductsList(
    products: List<ProductListItem>,
    onProductClick: (String) -> Unit,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (isRefreshing) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text(
                        text = stringResource(Res.string.prod_list_refreshing),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        items(products, key = { it.id }) { product ->
            ProductCard(product = product, onClick = { onProductClick(product.id) })
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun ProductCard(
    product: ProductListItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = LocalAppLocale.current
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                if (!product.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = product.imageUrl,
                        contentDescription = stringResource(Res.string.prod_list_cd_product_image),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.Category,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val subtitle = listOfNotNull(
                    product.code.takeIf { it.isNotBlank() },
                    product.categoryName,
                    product.brandName
                ).firstOrNull()
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                if (product.mrp != product.sellingPrice && product.mrp > 0) {
                    Text(
                        text = formatMoney(product.mrp, locale),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        textDecoration = TextDecoration.LineThrough
                    )
                }
                Text(
                    text = formatMoney(product.sellingPrice, locale),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                product.stockQuantity?.let { stock ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (stock > 0) MaterialTheme.colorScheme.primaryContainer
                               else MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = if (stock > 0) stringResource(Res.string.prod_list_in_stock_format, stock.toInt())
                                   else stringResource(Res.string.prod_out_of_stock),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (stock > 0) MaterialTheme.colorScheme.onPrimaryContainer
                                   else MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                if (!product.active) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.prod_inactive),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductTable(
    products: List<ProductListItem>,
    onProductClick: (String) -> Unit,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier
) {
    val locale = LocalAppLocale.current
    LazyColumn(modifier = modifier) {
        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(Res.string.prod_list_col_name), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(2.5f))
                    Text(stringResource(Res.string.prod_list_col_code), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1.5f))
                    Text(stringResource(Res.string.prod_list_col_category), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1.5f))
                    Text(stringResource(Res.string.prod_list_col_price), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Text(stringResource(Res.string.prod_list_col_stock), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                }
            }
        }

        if (isRefreshing) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    Text(text = stringResource(Res.string.prod_list_refreshing), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        items(products, key = { it.id }) { product ->
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onProductClick(product.id) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(modifier = Modifier.weight(2.5f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!product.imageUrl.isNullOrBlank()) {
                                AsyncImage(model = product.imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            } else {
                                Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Text(text = product.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text(text = product.code, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1.5f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = product.categoryName ?: "—", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1.5f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (product.mrp != product.sellingPrice && product.mrp > 0) {
                            Text(
                                text = formatMoney(product.mrp, locale),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                                textDecoration = TextDecoration.LineThrough,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = formatMoney(product.sellingPrice, locale),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = product.stockQuantity?.let { "${it.toInt()}" } ?: "—",
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            product.stockQuantity == null -> MaterialTheme.colorScheme.onSurfaceVariant
                            product.stockQuantity!! <= 0 -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
private fun EmptyState(
    onCreateProduct: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = stringResource(Res.string.prod_list_empty_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = stringResource(Res.string.prod_list_empty_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(24.dp))
        androidx.compose.material3.Button(onClick = onCreateProduct) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(Res.string.prod_list_cd_add))
        }
    }
}

@Composable
private fun ErrorMessage(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(text = stringResource(Res.string.prod_error_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
        Text(text = error, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))
        androidx.compose.material3.Button(onClick = onRetry) { Text(stringResource(Res.string.prod_retry)) }
    }
}
