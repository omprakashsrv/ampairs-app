package com.ampairs.product.ui.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass
import coil3.compose.AsyncImage
import com.ampairs.product.domain.Product
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import org.jetbrains.compose.resources.stringResource
import ampairsapp.feature.product.generated.resources.Res
import ampairsapp.feature.product.generated.resources.prod_details_title
import ampairsapp.feature.product.generated.resources.prod_details_cd_edit
import ampairsapp.feature.product.generated.resources.prod_details_cd_delete
import ampairsapp.feature.product.generated.resources.prod_details_not_found
import ampairsapp.feature.product.generated.resources.prod_details_delete_title
import ampairsapp.feature.product.generated.resources.prod_details_delete_confirm
import ampairsapp.feature.product.generated.resources.prod_details_delete_btn
import ampairsapp.feature.product.generated.resources.prod_details_total_stock_label
import ampairsapp.feature.product.generated.resources.prod_details_variants_count_label
import ampairsapp.feature.product.generated.resources.prod_details_inactive_alert
import ampairsapp.feature.product.generated.resources.prod_details_low_stock_alert
import ampairsapp.feature.product.generated.resources.prod_details_variants_low_stock
import ampairsapp.feature.product.generated.resources.prod_details_and_more
import ampairsapp.feature.product.generated.resources.prod_details_no_variants
import ampairsapp.feature.product.generated.resources.prod_section_basic
import ampairsapp.feature.product.generated.resources.prod_section_pricing
import ampairsapp.feature.product.generated.resources.prod_section_classification
import ampairsapp.feature.product.generated.resources.prod_section_stock_info
import ampairsapp.feature.product.generated.resources.prod_section_category_brand
import ampairsapp.feature.product.generated.resources.prod_section_unit
import ampairsapp.feature.product.generated.resources.prod_section_variants
import ampairsapp.feature.product.generated.resources.prod_section_images
import ampairsapp.feature.product.generated.resources.prod_label_name
import ampairsapp.feature.product.generated.resources.prod_label_code
import ampairsapp.feature.product.generated.resources.prod_label_status
import ampairsapp.feature.product.generated.resources.prod_label_description
import ampairsapp.feature.product.generated.resources.prod_label_tax_code
import ampairsapp.feature.product.generated.resources.prod_label_mrp
import ampairsapp.feature.product.generated.resources.prod_label_dealer_price
import ampairsapp.feature.product.generated.resources.prod_label_selling_price
import ampairsapp.feature.product.generated.resources.prod_label_discount
import ampairsapp.feature.product.generated.resources.prod_label_product_type
import ampairsapp.feature.product.generated.resources.prod_label_service_type
import ampairsapp.feature.product.generated.resources.prod_label_current_stock
import ampairsapp.feature.product.generated.resources.prod_label_low_stock_alert
import ampairsapp.feature.product.generated.resources.prod_label_category
import ampairsapp.feature.product.generated.resources.prod_label_brand
import ampairsapp.feature.product.generated.resources.prod_label_group
import ampairsapp.feature.product.generated.resources.prod_label_sub_category
import ampairsapp.feature.product.generated.resources.prod_label_base_unit
import ampairsapp.feature.product.generated.resources.prod_active
import ampairsapp.feature.product.generated.resources.prod_inactive
import ampairsapp.feature.product.generated.resources.prod_out_of_stock
import ampairsapp.feature.product.generated.resources.prod_list_in_stock_format
import ampairsapp.feature.product.generated.resources.prod_list_cd_product_image
import ampairsapp.feature.product.generated.resources.prod_manage_variants
import ampairsapp.feature.product.generated.resources.prod_stock_format
import ampairsapp.feature.product.generated.resources.prod_sku_format
import ampairsapp.feature.product.generated.resources.prod_load_error_title
import ampairsapp.feature.product.generated.resources.prod_retry
import ampairsapp.feature.product.generated.resources.prod_cancel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailsScreen(
    productId: String,
    onNavigateBack: () -> Unit,
    onEditProduct: (String) -> Unit,
    onManageVariants: ((String, String) -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: ProductDetailsViewModel = assistedMetroViewModel<ProductDetailsViewModel, ProductDetailsViewModel.Factory> { create(productId) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val isExpanded = windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(uiState.product?.name ?: stringResource(Res.string.prod_details_title)) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                }
            },
            actions = {
                if (uiState.product != null) {
                    IconButton(onClick = { onEditProduct(productId) }) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(Res.string.prod_details_cd_edit))
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.prod_details_cd_delete))
                    }
                }
            }
        )

        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            uiState.error != null -> {
                ErrorMessage(error = uiState.error!!, onRetry = {}, modifier = Modifier.fillMaxSize())
            }

            uiState.product != null -> {
                val product = uiState.product!!
                if (isExpanded) {
                    ProductDetailsExpanded(product = product, uiState = uiState, primaryImageUrl = uiState.primaryImageUrl, onManageVariants = onManageVariants, onEdit = { onEditProduct(productId) }, modifier = Modifier.fillMaxSize())
                } else {
                    ProductDetailsMobile(product = product, uiState = uiState, primaryImageUrl = uiState.primaryImageUrl, onManageVariants = onManageVariants, modifier = Modifier.fillMaxSize())
                }
            }

            else -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(Res.string.prod_details_not_found))
                }
            }
        }
    }

    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            productName = uiState.product?.name ?: "",
            onConfirm = {
                showDeleteDialog = false
                viewModel.deleteProduct { onNavigateBack() }
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

// ─── Mobile layout ────────────────────────────────────────────────────────────

@Composable
private fun ProductDetailsMobile(
    product: Product,
    uiState: ProductDetailsUiState,
    primaryImageUrl: String?,
    onManageVariants: ((String, String) -> Unit)?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        ProductHeroSection(product = product, primaryImageUrl = primaryImageUrl)
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ProductInfoContent(product = product, uiState = uiState, onManageVariants = onManageVariants)
        }
    }
}

// ─── Desktop/expanded layout ──────────────────────────────────────────────────

@Composable
private fun ProductDetailsExpanded(
    product: Product,
    uiState: ProductDetailsUiState,
    primaryImageUrl: String?,
    onManageVariants: ((String, String) -> Unit)?,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        // Left panel
        OutlinedCard(modifier = Modifier.width(260.dp).fillMaxHeight()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Product image
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    if (!primaryImageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = primaryImageUrl,
                            contentDescription = stringResource(Res.string.prod_list_cd_product_image),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Text(text = product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text(text = product.code, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                HorizontalDivider()

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PriceStatCard(
                        label = stringResource(Res.string.prod_label_selling_price),
                        value = "₹${product.sellingPrice}",
                        modifier = Modifier.weight(1f)
                    )
                    PriceStatCard(
                        label = stringResource(Res.string.prod_label_mrp),
                        value = "₹${product.mrp}",
                        modifier = Modifier.weight(1f),
                        strikethrough = product.mrp != product.sellingPrice
                    )
                }

                product.stockQuantity?.let { stock ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (stock > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (stock > 0) stringResource(Res.string.prod_list_in_stock_format, stock.toInt())
                                   else stringResource(Res.string.prod_out_of_stock),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (stock > 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                        )
                    }
                }

                if (!product.active) {
                    Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(Res.string.prod_inactive),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                        )
                    }
                }

                HorizontalDivider()
                Button(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(Res.string.prod_details_cd_edit))
                }
            }
        }

        // Right panel
        OutlinedCard(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ProductInfoContent(product = product, uiState = uiState, onManageVariants = onManageVariants)
            }
        }
    }
}

// ─── Hero section (mobile) ────────────────────────────────────────────────────

@Composable
private fun ProductHeroSection(product: Product, primaryImageUrl: String?, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                if (!primaryImageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = primaryImageUrl,
                        contentDescription = stringResource(Res.string.prod_list_cd_product_image),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(text = product.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(text = product.code, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Row(
                    modifier = Modifier.padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "₹${product.sellingPrice}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    if (product.mrp != product.sellingPrice && product.mrp > 0) {
                        Text(text = "₹${product.mrp}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, textDecoration = TextDecoration.LineThrough)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                    product.stockQuantity?.let { stock ->
                        Surface(shape = RoundedCornerShape(10.dp), color = if (stock > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer) {
                            Text(
                                text = if (stock > 0) stringResource(Res.string.prod_list_in_stock_format, stock.toInt()) else stringResource(Res.string.prod_out_of_stock),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (stock > 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                    if (!product.active) {
                        Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.errorContainer) {
                            Text(text = stringResource(Res.string.prod_inactive), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PriceStatCard(label: String, value: String, modifier: Modifier = Modifier, strikethrough: Boolean = false) {
    Surface(modifier = modifier, shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                textDecoration = if (strikethrough) TextDecoration.LineThrough else TextDecoration.None,
                color = if (strikethrough) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
            )
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

// ─── Shared detail content ────────────────────────────────────────────────────

@Composable
private fun ProductInfoContent(
    product: Product,
    uiState: ProductDetailsUiState,
    onManageVariants: ((String, String) -> Unit)?
) {
    if (!uiState.primaryImageUrl.isNullOrBlank()) {
        ProductImagesSection(primaryImageUrl = uiState.primaryImageUrl!!)
    }

    if (!product.active || product.isLowStock) {
        StatusAlertsSection(product = product)
    }

    InfoSection(title = stringResource(Res.string.prod_section_basic)) {
        InfoRow(label = stringResource(Res.string.prod_label_name), value = product.name)
        InfoRow(label = stringResource(Res.string.prod_label_code), value = product.code)
        InfoRow(label = stringResource(Res.string.prod_label_status), value = if (product.active) stringResource(Res.string.prod_active) else stringResource(Res.string.prod_inactive))
        if (product.description.isNotEmpty()) InfoRow(label = stringResource(Res.string.prod_label_description), value = product.description)
        if (product.taxCode.isNotEmpty()) InfoRow(label = stringResource(Res.string.prod_label_tax_code), value = product.taxCode)
    }

    InfoSection(title = stringResource(Res.string.prod_section_pricing)) {
        InfoRow(label = stringResource(Res.string.prod_label_mrp), value = "₹${product.mrp}")
        InfoRow(label = stringResource(Res.string.prod_label_dealer_price), value = "₹${product.dp}")
        InfoRow(label = stringResource(Res.string.prod_label_selling_price), value = "₹${product.sellingPrice}")
        if (product.mrp > product.sellingPrice) {
            val discount = ((product.mrp - product.sellingPrice) / product.mrp) * 100
            InfoRow(label = stringResource(Res.string.prod_label_discount), value = "${discount.toInt()}%")
        }
    }

    if (product.productType != null || product.serviceType != null) {
        InfoSection(title = stringResource(Res.string.prod_section_classification)) {
            product.productType?.let { InfoRow(label = stringResource(Res.string.prod_label_product_type), value = it.displayName) }
            product.serviceType?.let { InfoRow(label = stringResource(Res.string.prod_label_service_type), value = it.displayName) }
        }
    }

    if (product.hasVariants) {
        VariantsSection(product = product, onManageVariants = onManageVariants)
    }

    if (product.stockQuantity != null) {
        InfoSection(title = stringResource(Res.string.prod_section_stock_info)) {
            InfoRow(label = stringResource(Res.string.prod_label_current_stock), value = "${product.stockQuantity!!.toInt()} units")
            product.lowStockAlert?.let { InfoRow(label = stringResource(Res.string.prod_label_low_stock_alert), value = "${it.toInt()} units") }
        }
    }

    val hasCategoryBrandSection = uiState.categoryName.isNotBlank() || uiState.brandName.isNotBlank()
        || uiState.groupName.isNotBlank() || uiState.subCategoryName.isNotBlank()
    if (hasCategoryBrandSection) {
        InfoSection(title = stringResource(Res.string.prod_section_category_brand)) {
            if (uiState.categoryName.isNotBlank()) InfoRow(label = stringResource(Res.string.prod_label_category), value = uiState.categoryName)
            if (uiState.brandName.isNotBlank()) InfoRow(label = stringResource(Res.string.prod_label_brand), value = uiState.brandName)
            if (uiState.groupName.isNotBlank()) InfoRow(label = stringResource(Res.string.prod_label_group), value = uiState.groupName)
            if (uiState.subCategoryName.isNotBlank()) InfoRow(label = stringResource(Res.string.prod_label_sub_category), value = uiState.subCategoryName)
        }
    }

    if (uiState.baseUnitName.isNotBlank()) {
        InfoSection(title = stringResource(Res.string.prod_section_unit)) {
            InfoRow(label = stringResource(Res.string.prod_label_base_unit), value = uiState.baseUnitName)
        }
    }
}

// ─── Sub-components ───────────────────────────────────────────────────────────

@Composable
private fun ProductImagesSection(primaryImageUrl: String, modifier: Modifier = Modifier) {
    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = stringResource(Res.string.prod_section_images), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            Box(
                modifier = Modifier.size(100.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = primaryImageUrl,
                    contentDescription = stringResource(Res.string.prod_list_cd_product_image),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}

@Composable
private fun StatusAlertsSection(product: Product, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!product.active) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                    Text(text = stringResource(Res.string.prod_details_inactive_alert), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }
        if (product.isLowStock) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                    Text(text = stringResource(Res.string.prod_details_low_stock_alert, product.stockQuantity?.toInt() ?: 0), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }
        }
    }
}

@Composable
private fun InfoSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(2f))
    }
}

@Composable
private fun VariantsSection(product: Product, onManageVariants: ((String, String) -> Unit)?, modifier: Modifier = Modifier) {
    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = stringResource(Res.string.prod_section_variants), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                Icon(Icons.Default.Inventory, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(text = stringResource(Res.string.prod_details_total_stock_label), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = product.totalStock.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = stringResource(Res.string.prod_details_variants_count_label), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "${product.variants?.size ?: 0}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
            if (product.hasLowStockVariants) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(16.dp))
                        Text(text = stringResource(Res.string.prod_details_variants_low_stock), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                }
            }
            val variantsToShow = product.variants?.take(3) ?: emptyList()
            if (variantsToShow.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    variantsToShow.forEach { variant ->
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)) {
                            Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = variant.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    Text(text = stringResource(Res.string.prod_stock_format, variant.stockQuantity), style = MaterialTheme.typography.bodySmall, color = if (variant.isLowStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(text = stringResource(Res.string.prod_sku_format, variant.sku), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                if ((product.variants?.size ?: 0) > 3) {
                    Text(text = stringResource(Res.string.prod_details_and_more, (product.variants?.size ?: 0) - 3), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))
                }
            } else {
                Text(text = stringResource(Res.string.prod_details_no_variants), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (onManageVariants != null) {
                Button(onClick = { onManageVariants(product.id, product.name) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(Res.string.prod_manage_variants))
                }
            }
        }
    }
}

@Composable
private fun ErrorMessage(error: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(text = stringResource(Res.string.prod_load_error_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
        Text(text = error, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) { Text(stringResource(Res.string.prod_retry)) }
    }
}

@Composable
private fun DeleteConfirmationDialog(productName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.prod_details_delete_title)) },
        text = { Text(stringResource(Res.string.prod_details_delete_confirm, productName)) },
        confirmButton = {
            TextButton(onClick = onConfirm, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                Text(stringResource(Res.string.prod_details_delete_btn))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.prod_cancel)) } }
    )
}
