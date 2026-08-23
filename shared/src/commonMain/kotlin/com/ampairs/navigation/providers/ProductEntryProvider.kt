package com.ampairs.navigation.providers

import ProductRoute
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.ampairs.imagesearch.BulkTarget
import com.ampairs.imagesearch.ImageSearchRoute
import com.ampairs.product.catalog.CatalogFormScreen
import com.ampairs.product.domain.ProductListItem
import com.ampairs.product.catalog.ProductCatalogListScreen
import com.ampairs.product.catalog.ProductCatalogType
import com.ampairs.product.ui.create.ProductFormScreen
import com.ampairs.product.ui.details.ProductDetailsScreen
import com.ampairs.product.ui.list.ProductsListScreen
import com.ampairs.product.ui.variant.VariantFormScreen
import com.ampairs.product.ui.variant.VariantManagementScreen

/**
 * Entry provider for Product module routes in Navigation 3.
 * Returns NavEntry for product routes or null if route doesn't match.
 */
fun productEntryProvider(
    key: NavKey,
    backStack: MutableList<NavKey>
): NavEntry<NavKey>? = when (key) {
    // Product Group management
    is ProductRoute.Group -> NavEntry(key) {
        ProductCatalogListScreen(
            catalogType = ProductCatalogType.GROUPS,
            title = "Product Groups",
            onItemClick = { id -> backStack.add(ProductRoute.CatalogItemForm("GROUPS", id)) },
            onCreateItem = { backStack.add(ProductRoute.CatalogItemForm("GROUPS")) },
        )
    }

    // Products within a specific group
    is ProductRoute.Product -> NavEntry(key) {
        ProductsListScreen(
            onProductClick = { productId ->
                backStack.add(ProductRoute.ProductDetails(productId = productId))
            },
            onCreateProduct = {
                backStack.add(ProductRoute.ProductForm())
            },
            onNavigateToBrands = { backStack.add(ProductRoute.Brands) },
            onNavigateToCategories = { backStack.add(ProductRoute.Categories) },
            onNavigateToSubCategories = { backStack.add(ProductRoute.SubCategories) },
            onNavigateToGroups = { backStack.add(ProductRoute.Group()) },
            onAutoMatchImages = { products -> bulkImageMatchRoute(products)?.let(backStack::add) },
            modifier = Modifier
        )
    }

    // Edit product (redirects to ProductForm with productId)
    is ProductRoute.ProductEdit -> NavEntry(key) {
        ProductFormScreen(
            productId = key.productId,
            onSaveSuccess = {
                backStack.removeLastOrNull()
            },
            onManageVariants = { productId, productName ->
                backStack.add(
                    ProductRoute.VariantManagement(
                        productId = productId,
                        productName = productName
                    )
                )
            },
            onSearchWebImages = { entityUid, keywords ->
                backStack.add(
                    ImageSearchRoute.Search(
                        entityType = "PRODUCT",
                        entityUid = entityUid,
                        keywords = keywords,
                    )
                )
            },
            modifier = Modifier
        )
    }

    is ProductRoute.Products -> NavEntry(key) {
        ProductsListScreen(
            onProductClick = { productId ->
                backStack.add(ProductRoute.ProductDetails(productId = productId))
            },
            onCreateProduct = {
                backStack.add(ProductRoute.ProductForm())
            },
            onNavigateToBrands = { backStack.add(ProductRoute.Brands) },
            onNavigateToCategories = { backStack.add(ProductRoute.Categories) },
            onNavigateToSubCategories = { backStack.add(ProductRoute.SubCategories) },
            onNavigateToGroups = { backStack.add(ProductRoute.Group()) },
            onAutoMatchImages = { products -> bulkImageMatchRoute(products)?.let(backStack::add) },
            modifier = Modifier
        )
    }

    is ProductRoute.Brands -> NavEntry(key) {
        ProductCatalogListScreen(
            catalogType = ProductCatalogType.BRANDS,
            title = "Brands",
            onItemClick = { id -> backStack.add(ProductRoute.CatalogItemForm("BRANDS", id)) },
            onCreateItem = { backStack.add(ProductRoute.CatalogItemForm("BRANDS")) },
        )
    }

    is ProductRoute.Categories -> NavEntry(key) {
        ProductCatalogListScreen(
            catalogType = ProductCatalogType.CATEGORIES,
            title = "Categories",
            onItemClick = { id -> backStack.add(ProductRoute.CatalogItemForm("CATEGORIES", id)) },
            onCreateItem = { backStack.add(ProductRoute.CatalogItemForm("CATEGORIES")) },
        )
    }

    is ProductRoute.SubCategories -> NavEntry(key) {
        ProductCatalogListScreen(
            catalogType = ProductCatalogType.SUB_CATEGORIES,
            title = "Sub-categories",
            onItemClick = { id -> backStack.add(ProductRoute.CatalogItemForm("SUB_CATEGORIES", id)) },
            onCreateItem = { backStack.add(ProductRoute.CatalogItemForm("SUB_CATEGORIES")) },
        )
    }

    is ProductRoute.CatalogItemForm -> {
        val type = ProductCatalogType.valueOf(key.catalogType)
        NavEntry(key) {
            CatalogFormScreen(
                catalogType = type,
                itemId = key.itemId,
                onSaveSuccess = { backStack.removeLastOrNull() },
            )
        }
    }

    is ProductRoute.ProductDetails -> NavEntry(key) {
        ProductDetailsScreen(
            productId = key.productId,
            onNavigateBack = {
                backStack.removeLastOrNull()
            },
            onEditProduct = { productId ->
                backStack.add(ProductRoute.ProductForm(productId = productId))
            },
            onManageVariants = { productId, productName ->
                backStack.add(
                    ProductRoute.VariantManagement(
                        productId = productId,
                        productName = productName
                    )
                )
            },
            modifier = Modifier
        )
    }

    is ProductRoute.ProductForm -> NavEntry(key) {
        ProductFormScreen(
            productId = key.productId,
            onSaveSuccess = {
                backStack.removeLastOrNull()
            },
            onManageVariants = { productId, productName ->
                backStack.add(
                    ProductRoute.VariantManagement(
                        productId = productId,
                        productName = productName
                    )
                )
            },
            onSearchWebImages = { entityUid, keywords ->
                backStack.add(
                    ImageSearchRoute.Search(
                        entityType = "PRODUCT",
                        entityUid = entityUid,
                        keywords = keywords,
                    )
                )
            },
            modifier = Modifier
        )
    }

    is ProductRoute.VariantManagement -> NavEntry(key) {
        VariantManagementScreen(
            productId = key.productId,
            productName = key.productName,
            onNavigateToForm = { variantId ->
                backStack.add(
                    ProductRoute.VariantForm(
                        productId = key.productId,
                        variantId = variantId
                    )
                )
            },
            modifier = Modifier
        )
    }

    is ProductRoute.VariantForm -> NavEntry(key) {
        VariantFormScreen(
            productId = key.productId,
            variantId = key.variantId,
            onSaveSuccess = {
                backStack.removeLastOrNull()
            },
            modifier = Modifier
        )
    }

    // Tax information for products
    is ProductRoute.TaxInfo -> NavEntry(key) {
        PlaceholderScreen(
            title = "Product Tax Info",
            message = "Tax information configuration coming soon"
        )
    }

    // Tax code selection for products
    is ProductRoute.TaxCode -> NavEntry(key) {
        PlaceholderScreen(
            title = "Product Tax Codes",
            message = "Tax code selection coming soon"
        )
    }

    else -> null
}

/**
 * Build a bulk web image-match route for the products that are missing an image. Keywords per product
 * are name + brand + category. Capped so the sequential scrape stays bounded. Null when none qualify.
 */
private fun bulkImageMatchRoute(products: List<ProductListItem>): ImageSearchRoute.BulkMatch? {
    val targets = products
        .filter { it.imageUrl.isNullOrBlank() && it.name.isNotBlank() }
        .take(40)
        .map { product ->
            BulkTarget(
                entityUid = product.id,
                name = product.name,
                keywords = listOfNotNull(
                    product.name.takeIf { it.isNotBlank() },
                    product.brandName?.takeIf { it.isNotBlank() },
                    product.categoryName?.takeIf { it.isNotBlank() },
                ),
            )
        }
    return if (targets.isEmpty()) null else ImageSearchRoute.BulkMatch(entityType = "PRODUCT", targets = targets)
}

/**
 * Placeholder screen for routes that are defined but not yet implemented.
 */
@Composable
private fun PlaceholderScreen(
    title: String,
    message: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
