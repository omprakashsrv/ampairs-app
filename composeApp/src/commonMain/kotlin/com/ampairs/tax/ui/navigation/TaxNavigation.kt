package com.ampairs.tax.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.toRoute
import com.ampairs.common.ui.AppScreenWithHeader
import com.ampairs.tax.ui.calculator.TaxCalculatorScreen
import com.ampairs.tax.ui.hsn.HsnCodesListScreen
import com.ampairs.tax.ui.hsn.HsnCodeFormScreen
import com.ampairs.tax.ui.hsn.HsnCodeDetailsScreen
import com.ampairs.tax.ui.rates.TaxRatesListScreen
import com.ampairs.tax.ui.rates.TaxRateFormScreen
import com.ampairs.tax.ui.rates.TaxRateDetailsScreen
import kotlinx.serialization.Serializable
import Route

// Tax Navigation Routes
@Serializable
object TaxListRoute

@Serializable
object HsnCodesListRoute

@Serializable
data class HsnCodeDetailsRoute(val hsnCodeId: String)

@Serializable
data class HsnCodeFormRoute(val hsnCodeId: String? = null)

@Serializable
object TaxCalculatorRoute

@Serializable
object TaxRatesRoute

@Serializable
data class TaxRateDetailsRoute(val taxRateId: String)

@Serializable
data class TaxRateFormRoute(val taxRateId: String? = null)

fun NavGraphBuilder.taxNavigation(
    navController: NavHostController
) {
    navigation<Route.Tax>(startDestination = TaxListRoute) {
        // Main Tax Module Landing Screen
        composable<TaxListRoute> {
            AppScreenWithHeader(
                navController = navController,
                isWorkspaceSelection = false
            ) { paddingValues ->
                TaxModuleScreen(
                    onNavigateToHsnCodes = {
                        navController.navigate(HsnCodesListRoute)
                    },
                    onNavigateToTaxCalculator = {
                        navController.navigate(TaxCalculatorRoute)
                    },
                    onNavigateToTaxRates = {
                        navController.navigate(TaxRatesRoute)
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }

        // HSN Codes List Screen
        composable<HsnCodesListRoute> {
            AppScreenWithHeader(
                navController = navController,
                isWorkspaceSelection = false
            ) { paddingValues ->
                HsnCodesListScreen(
                    onHsnCodeClick = { hsnCodeId ->
                        navController.navigate(HsnCodeDetailsRoute(hsnCodeId))
                    },
                    onCreateHsnCode = {
                        navController.navigate(HsnCodeFormRoute())
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }

        // HSN Code Details Screen
        composable<HsnCodeDetailsRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<HsnCodeDetailsRoute>()
            AppScreenWithHeader(
                navController = navController,
                isWorkspaceSelection = false
            ) { paddingValues ->
                HsnCodeDetailsScreen(
                    hsnCodeId = route.hsnCodeId,
                    onNavigateBack = { navController.popBackStack() },
                    onEditHsnCode = { hsnCodeId ->
                        navController.navigate(HsnCodeFormRoute(hsnCodeId))
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }

        // HSN Code Form Screen (Create/Edit)
        composable<HsnCodeFormRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<HsnCodeFormRoute>()
            AppScreenWithHeader(
                navController = navController,
                isWorkspaceSelection = false
            ) { paddingValues ->
                HsnCodeFormScreen(
                    hsnCodeId = route.hsnCodeId,
                    onSaveSuccess = { navController.popBackStack() },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }

        // Tax Calculator Screen
        composable<TaxCalculatorRoute> {
            AppScreenWithHeader(
                navController = navController,
                isWorkspaceSelection = false
            ) { paddingValues ->
                TaxCalculatorScreen(
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }

        // Tax Rates List Screen
        composable<TaxRatesRoute> {
            AppScreenWithHeader(
                navController = navController,
                isWorkspaceSelection = false
            ) { paddingValues ->
                TaxRatesListScreen(
                    onTaxRateClick = { taxRateId ->
                        navController.navigate(TaxRateDetailsRoute(taxRateId))
                    },
                    onCreateTaxRate = {
                        navController.navigate(TaxRateFormRoute())
                    },
                    onFormConfig = {
                        navController.navigate(Route.FormConfig("tax_rate"))
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }

        // Tax Rate Details Screen
        composable<TaxRateDetailsRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<TaxRateDetailsRoute>()
            AppScreenWithHeader(
                navController = navController,
                isWorkspaceSelection = false
            ) { paddingValues ->
                TaxRateDetailsScreen(
                    taxRateId = route.taxRateId,
                    onNavigateBack = { navController.popBackStack() },
                    onEditTaxRate = { taxRateId ->
                        navController.navigate(TaxRateFormRoute(taxRateId))
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }

        // Tax Rate Form Screen (Create/Edit)
        composable<TaxRateFormRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<TaxRateFormRoute>()
            AppScreenWithHeader(
                navController = navController,
                isWorkspaceSelection = false
            ) { paddingValues ->
                TaxRateFormScreen(
                    taxRateId = route.taxRateId,
                    onSaveSuccess = { navController.popBackStack() },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
fun TaxScreen(
    onNavigateToHsnCodes: () -> Unit,
    onNavigateToTaxCalculator: () -> Unit,
    onNavigateToTaxRates: () -> Unit,
    modifier: Modifier = Modifier
) {
    TaxModuleScreen(
        onNavigateToHsnCodes = onNavigateToHsnCodes,
        onNavigateToTaxCalculator = onNavigateToTaxCalculator,
        onNavigateToTaxRates = onNavigateToTaxRates,
        modifier = modifier
    )
}