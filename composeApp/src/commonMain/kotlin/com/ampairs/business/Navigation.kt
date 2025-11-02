package com.ampairs.business

import BusinessRoute
import Route
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.ampairs.common.ui.AppScreenWithHeader

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
            AppScreenWithHeader(
                navController = navController,
                isWorkspaceSelection = false
            ) { paddingValues ->
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
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }

        // Profile & Registration Screen
        composable<BusinessRoute.Profile> {
            AppScreenWithHeader(
                navController = navController,
                isWorkspaceSelection = false
            ) { paddingValues ->
                com.ampairs.business.ui.BusinessProfileFormScreen(
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }

        // Operations Settings Screen
        composable<BusinessRoute.Operations> {
            AppScreenWithHeader(
                navController = navController,
                isWorkspaceSelection = false
            ) { paddingValues ->
                com.ampairs.business.ui.BusinessOperationsScreen(
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }

        // Tax Configuration Screen
        composable<BusinessRoute.TaxConfig> {
            AppScreenWithHeader(
                navController = navController,
                isWorkspaceSelection = false
            ) { paddingValues ->
                com.ampairs.business.ui.BusinessTaxConfigScreen(
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }

        // Custom Attributes Screen
        composable<BusinessRoute.CustomAttributes> {
            AppScreenWithHeader(
                navController = navController,
                isWorkspaceSelection = false
            ) { paddingValues ->
                com.ampairs.business.ui.BusinessCustomAttributesScreen(
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}
