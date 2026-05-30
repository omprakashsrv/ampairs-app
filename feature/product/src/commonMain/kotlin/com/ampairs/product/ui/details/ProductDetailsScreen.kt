package com.ampairs.product.ui.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.ampairs.product.domain.Product
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import org.jetbrains.compose.resources.stringResource
import ampairsapp.feature.product.generated.resources.Res
import ampairsapp.feature.product.generated.resources.*

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
    val productDetailsTitle = stringResource(Res.string.prod_details_title)

    LaunchedEffect(productId) {
        // Product is observed reactively — no explicit reload needed
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(uiState.product?.name ?: productDetailsTitle) },
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
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.error != null -> {
                val errorMessage = uiState.error ?: return@Column
                ErrorMessage(
                    error = errorMessage,
                    onRetry = {},
                    modifier = Modifier.fillMaxSize()
                )
            }

            uiState.product != null -> {
                val currentProduct = uiState.product ?: return@Column
                ProductDetailsContent(
                    product = currentProduct,
                    onManageVariants = onManageVariants,
                    modifier = Modifier.fillMaxSize()
                )
            }

            else -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
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
                viewModel.deleteProduct {
                    onNavigateBack()
                }
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

@Composable
private fun ProductDetailsContent(
    product: Product,
    onManageVariants: ((String, String) -> Unit)?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!product.images.isNullOrEmpty()) {
            ProductImagesSection(product = product)
        }

        if (!product.active || product.isLowStock) {
            StatusAlertsSection(product = product)
        }

        InfoSection(title = stringResource(Res.string.prod_section_basic)) {
            InfoRow(label = stringResource(Res.string.prod_label_name), value = product.name)
            InfoRow(label = stringResource(Res.string.prod_label_code), value = product.code)
            InfoRow(
                label = stringResource(Res.string.prod_label_status),
                value = if (product.active) stringResource(Res.string.prod_active) else stringResource(Res.string.prod_inactive)
            )
            if (product.description.isNotEmpty()) {
                InfoRow(label = stringResource(Res.string.prod_label_description), value = product.description)
            }
            if (product.taxCode.isNotEmpty()) {
                InfoRow(label = stringResource(Res.string.prod_label_tax_code), value = product.taxCode)
            }
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
                product.productType?.let { type ->
                    InfoRow(label = stringResource(Res.string.prod_label_product_type), value = type.displayName)
                }
                product.serviceType?.let { type ->
                    InfoRow(label = stringResource(Res.string.prod_label_service_type), value = type.displayName)
                }
            }
        }

        if (product.hasVariants) {
            VariantsSection(
                product = product,
                onManageVariants = onManageVariants
            )
        }

        if (product.stockQuantity != null) {
            InfoSection(title = stringResource(Res.string.prod_section_stock_info)) {
                InfoRow(label = stringResource(Res.string.prod_label_current_stock), value = "${product.stockQuantity!!.toInt()} units")
                if (product.lowStockAlert != null) {
                    InfoRow(label = stringResource(Res.string.prod_label_low_stock_alert), value = "${product.lowStockAlert!!.toInt()} units")
                }
            }
        }

        if (!product.categoryName.isNullOrBlank() || !product.brandName.isNullOrBlank() ||
            product.categoryId.isNotEmpty() || product.brandId.isNotEmpty()) {
            InfoSection(title = stringResource(Res.string.prod_section_category_brand)) {
                product.categoryName?.let { categoryName ->
                    InfoRow(label = stringResource(Res.string.prod_label_category), value = categoryName)
                }
                product.brandName?.let { brandName ->
                    InfoRow(label = stringResource(Res.string.prod_label_brand), value = brandName)
                }
                if (product.groupId.isNotEmpty()) {
                    InfoRow(label = stringResource(Res.string.prod_label_group_id), value = product.groupId)
                }
                if (product.subCategoryId.isNotEmpty()) {
                    InfoRow(label = stringResource(Res.string.prod_label_sub_category_id), value = product.subCategoryId)
                }
            }
        }

        product.baseUnitId?.let { baseUnitId ->
            InfoSection(title = stringResource(Res.string.prod_section_unit)) {
                InfoRow(label = stringResource(Res.string.prod_label_base_unit_id), value = baseUnitId)
            }
        }
    }
}

@Composable
private fun ProductImagesSection(
    product: Product,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(Res.string.prod_section_images),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(product.images ?: emptyList()) { productImage ->
                    ProductImageItem(
                        imageUrl = productImage.image.url,
                        modifier = Modifier.size(120.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductImageItem(
    imageUrl: String?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = stringResource(Res.string.prod_list_cd_product_image),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                Icons.Default.Category,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatusAlertsSection(
    product: Product,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!product.active) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = stringResource(Res.string.prod_details_inactive_alert),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        if (product.isLowStock) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = stringResource(Res.string.prod_details_low_stock_alert, product.stockQuantity?.toInt() ?: 0),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(2f)
        )
    }
}

@Composable
private fun ErrorMessage(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(Res.string.prod_load_error_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error
        )

        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onRetry) {
            Text(stringResource(Res.string.prod_retry))
        }
    }
}

@Composable
private fun VariantsSection(
    product: Product,
    onManageVariants: ((String, String) -> Unit)?,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.prod_section_variants),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    Icons.Default.Inventory,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(Res.string.prod_details_total_stock_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = product.totalStock.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(Res.string.prod_details_variants_count_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${product.variants?.size ?: 0}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (product.hasLowStockVariants) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(Res.string.prod_details_variants_low_stock),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            val variantsToShow = product.variants?.take(3) ?: emptyList()
            if (variantsToShow.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    variantsToShow.forEach { variant ->
                        VariantListItem(variant = variant)
                    }
                }

                if ((product.variants?.size ?: 0) > 3) {
                    Text(
                        text = stringResource(Res.string.prod_details_and_more, (product.variants?.size ?: 0) - 3),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            } else {
                Text(
                    text = stringResource(Res.string.prod_details_no_variants),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (onManageVariants != null) {
                Button(
                    onClick = { onManageVariants(product.id, product.name) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(Res.string.prod_manage_variants))
                }
            }
        }
    }
}

@Composable
private fun VariantListItem(
    variant: com.ampairs.product.domain.ProductVariant,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = variant.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = stringResource(Res.string.prod_stock_format, variant.stockQuantity),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (variant.isLowStock) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            Text(
                text = stringResource(Res.string.prod_sku_format, variant.sku),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DeleteConfirmationDialog(
    productName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.prod_details_delete_title)) },
        text = { Text(stringResource(Res.string.prod_details_delete_confirm, productName)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(Res.string.prod_details_delete_btn))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.prod_cancel))
            }
        }
    )
}
