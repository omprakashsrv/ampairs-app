package com.ampairs.navigation.providers

import ProductRoute
import Route
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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

    else -> null
}
