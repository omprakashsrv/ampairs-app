package com.ampairs.business

import BusinessRoute
import Route
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation

/**
 * Business Management Module Navigation.
 *
 * Maps backend routes to mobile screens:
 * - /business/overview → BusinessRoute.Overview (default dashboard)
 * - /business/profile → BusinessRoute.Profile (profile & registration)
 * - /business/operations → BusinessRoute.Operations (operational settings)
 * - /business/tax → BusinessRoute.TaxConfig (tax configuration)
 */
fun NavGraphBuilder.businessNavigation(
    navController: NavHostController
) {
    navigation<Route.Business>(startDestination = BusinessRoute.Overview) {

        // Overview Screen (Default - Dashboard)
        composable<BusinessRoute.Overview> {
            com.ampairs.business.ui.BusinessOverviewScreen(
                onNavigateToProfile = {
                    navController.navigate(BusinessRoute.Profile)
                },
                onNavigateToOperations = {
                    navController.navigate(BusinessRoute.Operations)
                },
                onNavigateToTax = {
                    navController.navigate(BusinessRoute.TaxConfig)
                },
                onNavigateToCustomAttributes = {
                    navController.navigate(BusinessRoute.CustomAttributes)
                },
                onNavigateToFormConfig = {
                    navController.navigate(Route.FormConfig("business"))
                },
                onNavigateToImages = {
                    navController.navigate(BusinessRoute.Images)
                },
                modifier = Modifier
            )
        }

        // Profile & Registration Screen
        composable<BusinessRoute.Profile> {
            com.ampairs.business.ui.BusinessProfileFormScreen(
                modifier = Modifier
            )
        }

        // Operations Settings Screen
        composable<BusinessRoute.Operations> {
            com.ampairs.business.ui.BusinessOperationsScreen(
                modifier = Modifier
            )
        }

        // Tax Configuration Screen
        composable<BusinessRoute.TaxConfig> {
            com.ampairs.business.ui.BusinessTaxConfigScreen(
                modifier = Modifier
            )
        }

        // Custom Attributes Screen
        composable<BusinessRoute.CustomAttributes> {
            com.ampairs.business.ui.BusinessCustomAttributesScreen(
                modifier = Modifier
            )
        }

        // Images Screen (Logo & Gallery)
        composable<BusinessRoute.Images> {
            com.ampairs.business.ui.BusinessImagesScreen(
                modifier = Modifier
            )
        }
    }
}
