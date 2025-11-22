package com.ampairs.product

import ProductRoute
import Route
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.toRoute
import com.ampairs.product.ui.create.ProductFormScreen
import com.ampairs.product.ui.details.ProductDetailsScreen
import com.ampairs.product.ui.list.ProductsListScreen

fun NavGraphBuilder.productNavigation(
    navController: NavHostController
) {
    navigation<Route.Product>(startDestination = ProductRoute.Products) {
        // Main Product List Screen
        composable<ProductRoute.Products> {
            ProductsListScreen(
                onProductClick = { productId ->
                    navController.navigate(ProductRoute.ProductDetails(productId = productId))
                },
                onCreateProduct = {
                    navController.navigate(ProductRoute.ProductForm())
                },
                onFormConfig = {
                    println("ProductNavigation: Navigating to FormConfig for product")
                    navController.navigate(Route.FormConfig("product"))
                },
                modifier = Modifier
            )
        }

        // Product Details Screen
        composable<ProductRoute.ProductDetails> { backStackEntry ->
            val productDetailsRoute = backStackEntry.toRoute<ProductRoute.ProductDetails>()
            ProductDetailsScreen(
                productId = productDetailsRoute.productId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onEditProduct = { productId ->
                    navController.navigate(ProductRoute.ProductForm(productId = productId))
                },
                modifier = Modifier
            )
        }

        // Product Form Screen (Create/Edit)
        composable<ProductRoute.ProductForm> { backStackEntry ->
            val productFormRoute = backStackEntry.toRoute<ProductRoute.ProductForm>()
            ProductFormScreen(
                productId = productFormRoute.productId,
                onSaveSuccess = {
                    navController.popBackStack()
                },
                modifier = Modifier
            )
        }
    }
}

@Composable
fun ProductScreen(
    onProductClick: (String) -> Unit,
    onCreateProduct: () -> Unit,
    onFormConfig: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    ProductsListScreen(
        onProductClick = onProductClick,
        onCreateProduct = onCreateProduct,
        onFormConfig = onFormConfig,
        modifier = modifier
    )
}
