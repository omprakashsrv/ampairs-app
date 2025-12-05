package com.ampairs.tax.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import Route
import com.ampairs.tax.ui.calculator.TaxCalculatorScreen
import com.ampairs.tax.ui.list.MyTaxCodesScreen
import com.ampairs.tax.ui.search.TaxCodeSearchScreen
import com.ampairs.tax.ui.detail.TaxCodeDetailScreen

// Tax Navigation Routes
@Serializable
object TaxListRoute

@Serializable
object TaxCalculatorRoute

@Serializable
object MyTaxCodesRoute

@Serializable
data class TaxCodeSearchRoute(val initialQuery: String = "")

@Serializable
data class TaxCodeDetailRoute(val taxCodeId: String)

fun NavGraphBuilder.taxNavigation(
    navController: NavHostController
) {
    navigation<Route.Tax>(startDestination = TaxListRoute) {
        // Main Tax Module Landing Screen
        composable<TaxListRoute> {
            TaxModuleScreen(
                onNavigateToCalculator = {
                    navController.navigate(TaxCalculatorRoute)
                },
                onNavigateToMyTaxCodes = {
                    navController.navigate(MyTaxCodesRoute)
                },
                onNavigateToSearch = {
                    navController.navigate(TaxCodeSearchRoute())
                },
                modifier = Modifier
            )
        }

        // Tax Calculator Screen
        composable<TaxCalculatorRoute> {
            TaxCalculatorScreen(
                modifier = Modifier
            )
        }

        // My Tax Codes List Screen
        composable<MyTaxCodesRoute> {
            MyTaxCodesScreen(
                onNavigateToEdit = { codeId ->
                    navController.navigate(TaxCodeDetailRoute(taxCodeId = codeId))
                },
                onNavigateToSearch = {
                    navController.navigate(TaxCodeSearchRoute())
                },
                modifier = Modifier
            )
        }

        // Tax Code Search Screen
        composable<TaxCodeSearchRoute> {
            val route = it.toRoute<TaxCodeSearchRoute>()
            TaxCodeSearchScreen(
                onCodeSelected = { codeId ->
                    navController.navigate(TaxCodeDetailRoute(taxCodeId = codeId))
                },
                modifier = Modifier
            )
        }

        // Tax Code Detail Screen
        composable<TaxCodeDetailRoute> {
            val route = it.toRoute<TaxCodeDetailRoute>()
            TaxCodeDetailScreen(
                taxCodeId = route.taxCodeId,
                onNavigateBack = { navController.popBackStack() },
                modifier = Modifier
            )
        }
    }
}

@Composable
fun TaxScreen(
    modifier: Modifier = Modifier
) {
    TaxModuleScreen(
        onNavigateToCalculator = {},
        onNavigateToMyTaxCodes = {},
        onNavigateToSearch = {},
        modifier = modifier
    )
}
