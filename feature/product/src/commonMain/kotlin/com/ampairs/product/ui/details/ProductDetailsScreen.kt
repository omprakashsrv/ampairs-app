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
import coil3.compose.AsyncImage
import com.ampairs.product.domain.Product
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
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
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(productId) {
        // Product is observed reactively — no explicit reload needed
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(uiState.product?.name ?: "Product Details") },
            actions = {
                if (uiState.product != null) {
                    IconButton(onClick = { onEditProduct(productId) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
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
                    Text("Product not found")
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
        // Product Images Section
        if (!product.images.isNullOrEmpty()) {
            ProductImagesSection(product = product)
        }

        // Status Alerts
        if (!product.active || product.isLowStock) {
            StatusAlertsSection(product = product)
        }

        // Basic Information
        InfoSection(title = "Basic Information") {
            InfoRow(label = "Name", value = product.name)
            InfoRow(label = "Code", value = product.code)
            InfoRow(label = "Status", value = if (product.active) "Active" else "Inactive")
            if (product.description.isNotEmpty()) {
                InfoRow(label = "Description", value = product.description)
            }
            if (product.taxCode.isNotEmpty()) {
                InfoRow(label = "Tax Code", value = product.taxCode)
            }
        }

        // Pricing Information
        InfoSection(title = "Pricing") {
            InfoRow(label = "MRP", value = "₹${product.mrp}")
            InfoRow(label = "Dealer Price", value = "₹${product.dp}")
            InfoRow(label = "Selling Price", value = "₹${product.sellingPrice}")

            if (product.mrp > product.sellingPrice) {
                val discount = ((product.mrp - product.sellingPrice) / product.mrp) * 100
                InfoRow(label = "Discount", value = "${discount.toInt()}%")
            }
        }

        // Classification
        if (product.productType != null || product.serviceType != null) {
            InfoSection(title = "Classification") {
                product.productType?.let { type ->
                    InfoRow(label = "Product Type", value = type.displayName)
                }
                product.serviceType?.let { type ->
                    InfoRow(label = "Service Type", value = type.displayName)
                }
            }
        }

        // Variants Section
        if (product.hasVariants) {
            VariantsSection(
                product = product,
                onManageVariants = onManageVariants
            )
        }

        // Stock Information
        if (product.stockQuantity != null) {
            InfoSection(title = "Stock Information") {
                InfoRow(label = "Current Stock", value = "${product.stockQuantity!!.toInt()} units")
                if (product.lowStockAlert != null) {
                    InfoRow(label = "Low Stock Alert", value = "${product.lowStockAlert!!.toInt()} units")
                }
            }
        }

        // Category Information
        if (!product.categoryName.isNullOrBlank() || !product.brandName.isNullOrBlank() ||
            product.categoryId.isNotEmpty() || product.brandId.isNotEmpty()) {
            InfoSection(title = "Category & Brand") {
                product.categoryName?.let { categoryName ->
                    InfoRow(label = "Category", value = categoryName)
                }
                product.brandName?.let { brandName ->
                    InfoRow(label = "Brand", value = brandName)
                }
                if (product.groupId.isNotEmpty()) {
                    InfoRow(label = "Group ID", value = product.groupId)
                }
                if (product.subCategoryId.isNotEmpty()) {
                    InfoRow(label = "Sub Category ID", value = product.subCategoryId)
                }
            }
        }

        // Additional Information
        product.baseUnitId?.let { baseUnitId ->
            InfoSection(title = "Unit Information") {
                InfoRow(label = "Base Unit ID", value = baseUnitId)
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
                text = "Product Images",
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
                contentDescription = "Product image",
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
                        text = "This product is currently inactive",
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
                        text = "Low stock alert: Only ${product.stockQuantity?.toInt()} units remaining",
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
            text = "Failed to load product",
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
            Text("Retry")
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
                    text = "Product Variants",
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

            // Summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Total Stock",
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
                        text = "Variants",
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

            // Low stock alert
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
                            text = "Some variants are low on stock",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            // First 3 variants preview
            val variantsToShow = product.variants?.take(3) ?: emptyList()
            if (variantsToShow.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    variantsToShow.forEach { variant ->
                        VariantListItem(variant = variant)
                    }
                }

                if ((product.variants?.size ?: 0) > 3) {
                    Text(
                        text = "And ${(product.variants?.size ?: 0) - 3} more...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            } else {
                Text(
                    text = "No variants added yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Manage button
            if (onManageVariants != null) {
                Button(
                    onClick = { onManageVariants(product.id, product.name) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Manage Variants")
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
                    text = "Stock: ${variant.stockQuantity}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (variant.isLowStock) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            Text(
                text = "SKU: ${variant.sku}",
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
        title = { Text("Delete Product") },
        text = {
            Text("Are you sure you want to delete $productName? This action cannot be undone.")
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}