package com.ampairs.navigation.providers

import ProductRoute
import Route
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
    // Product Group management (for organizing products into groups)
    is ProductRoute.Group -> NavEntry(key) {
        // TODO: Implement ProductGroupScreen when available
        PlaceholderScreen(
            title = "Product Groups",
            message = "Product group management coming soon"
        )
    }

    // Products within a specific group
    is ProductRoute.Product -> NavEntry(key) {
        // Redirect to products list filtered by group
        ProductsListScreen(
            onProductClick = { productId ->
                backStack.add(ProductRoute.ProductDetails(productId = productId))
            },
            onCreateProduct = {
                backStack.add(ProductRoute.ProductForm())
            },
            onFormConfig = {
                backStack.add(Route.FormConfig("product"))
            },
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
            onFormConfig = {
                backStack.add(Route.FormConfig("product"))
            },
            modifier = Modifier
        )
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
