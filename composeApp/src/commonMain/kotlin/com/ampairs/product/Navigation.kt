package com.ampairs.product

import ProductRoute
import Route
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.toRoute
import com.ampairs.common.ui.AppScreenWithHeader
import com.ampairs.product.ui.create.ProductFormScreen
import com.ampairs.product.ui.details.ProductDetailsScreen
import com.ampairs.product.ui.list.ProductsListScreen

fun NavGraphBuilder.productNavigation(
    navController: NavHostController
) {
    navigation<Route.Product>(startDestination = ProductRoute.Products) {
        // Main Product List Screen
        composable<ProductRoute.Products> {
            AppScreenWithHeader(
                navController = navController,
                isWorkspaceSelection = false
            ) { paddingValues ->
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
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }

        // Product Details Screen
        composable<ProductRoute.ProductDetails> { backStackEntry ->
            val productDetailsRoute = backStackEntry.toRoute<ProductRoute.ProductDetails>()
            AppScreenWithHeader(
                navController = navController,
                isWorkspaceSelection = false
            ) { paddingValues ->
                ProductDetailsScreen(
                    productId = productDetailsRoute.productId,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onEditProduct = { productId ->
                        navController.navigate(ProductRoute.ProductForm(productId = productId))
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }

        // Product Form Screen (Create/Edit)
        composable<ProductRoute.ProductForm> { backStackEntry ->
            val productFormRoute = backStackEntry.toRoute<ProductRoute.ProductForm>()
            AppScreenWithHeader(
                navController = navController,
                isWorkspaceSelection = false
            ) { paddingValues ->
                ProductFormScreen(
                    productId = productFormRoute.productId,
                    onSaveSuccess = {
                        navController.popBackStack()
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }

        // Legacy routes - commented out as these UI components don't exist yet
        // composable<ProductRoute.Group> { backStackEntry ->
        //     val groupRoute = backStackEntry.toRoute<ProductRoute.Group>()
        //     val groupType = GroupType.valueOf(groupRoute.type)
        //     val editMode = groupRoute.edit
        //     if (editMode) {
        //         ProductGroupEditScreen(groupType) {}
        //     } else {
        //         val productViewModel: ProductViewModel = koinInject()
        //         ProductGroupScreen(productViewModel, groupType) { group ->
        //             navigator.navigate(ProductRoute.Product(groupId = group.id))
        //         }
        //     }
        // }

        // composable<ProductRoute.Product> { backStackEntry ->
        //     val productRoute = backStackEntry.toRoute<ProductRoute.Product>()
        //     val productViewModel: ProductViewModel = koinInject()
        //     ProductScreen(productRoute.groupId, productViewModel, onProductClick = { id ->
        //         navigator.navigate(ProductRoute.ProductEdit(productId = id))
        //     }) {
        //         navigator.popBackStack()
        //     }
        // }

        // composable<ProductRoute.ProductEdit> { backStackEntry ->
        //     val productEditRoute = backStackEntry.toRoute<ProductRoute.ProductEdit>()
        //     ProductEditScreen(Modifier, productEditRoute.productId) {
        //         navigator.popBackStack()
        //     }
        // }

        // composable<ProductRoute.TaxInfo> {
        //     TaxInfoPane()
        // }

        // composable<ProductRoute.TaxCode> {
        //     TaxCodePaneScreen()
        // }
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