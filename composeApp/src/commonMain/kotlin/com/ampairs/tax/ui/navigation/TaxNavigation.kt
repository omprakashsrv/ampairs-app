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

// Tax Navigation Routes
@Serializable
object TaxListRoute

fun NavGraphBuilder.taxNavigation(
    navController: NavHostController
) {
    navigation<Route.Tax>(startDestination = TaxListRoute) {
        // Main Tax Module Landing Screen
        composable<TaxListRoute> {
            TaxModuleScreen(
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
        modifier = modifier
    )
}
