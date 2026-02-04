package com.ampairs.customer.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.toRoute
import Route
import androidx.navigation3.runtime.NavKey
import com.ampairs.customer.ui.create.CustomerFormScreen
import com.ampairs.customer.ui.customergroup.CustomerGroupFormScreen
import com.ampairs.customer.ui.customergroup.CustomerGroupListScreen
import com.ampairs.customer.ui.customertype.CustomerTypeFormScreen
import com.ampairs.customer.ui.customertype.CustomerTypeListScreen
import com.ampairs.customer.ui.details.CustomerDetailsScreen
import com.ampairs.customer.ui.list.CustomersListScreen
import com.ampairs.customer.ui.state.StateListScreen
import kotlinx.serialization.Serializable

@Serializable
object CustomerListRoute : NavKey

@Serializable
data class CustomerDetailsRoute(val customerId: String) : NavKey

@Serializable
data class CustomerCreateRoute(val customerId: String? = null) : NavKey

@Serializable
object StateListRoute : NavKey

@Serializable
object CustomerTypeListRoute : NavKey

@Serializable
data class CustomerTypeCreateRoute(val customerTypeId: String? = null) : NavKey

@Serializable
object CustomerGroupListRoute : NavKey

@Serializable
data class CustomerGroupCreateRoute(val customerGroupId: String? = null) : NavKey


fun NavGraphBuilder.customerNavigation(navController: NavHostController) {
    navigation<Route.Customer>(startDestination = CustomerListRoute) {
        // Main Customer List Screen
        composable<CustomerListRoute> {
            CustomersListScreen(
                onCustomerClick = { customerId ->
                    navController.navigate(CustomerDetailsRoute(customerId))
                },
                onCreateCustomer = {
                    navController.navigate(CustomerCreateRoute())
                },
                onFormConfig = {
                    println("CustomerNavigation: Navigating to FormConfig for customer")
                    navController.navigate(Route.FormConfig("customer"))
                },
                modifier = Modifier
            )
        }

        // Customer Details Screen
        composable<CustomerDetailsRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<CustomerDetailsRoute>()
            CustomerDetailsScreen(
                customerId = route.customerId,
                onNavigateBack = { navController.popBackStack() },
                onEditCustomer = { customerId ->
                    navController.navigate(CustomerCreateRoute(customerId))
                },
                modifier = Modifier
            )
        }

        // Customer Form Screen (Create/Edit)
        composable<CustomerCreateRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<CustomerCreateRoute>()
            CustomerFormScreen(
                customerId = route.customerId,
                onSaveSuccess = { navController.popBackStack() },
                modifier = Modifier
            )
        }

        // State List Screen
        composable<StateListRoute> {
            StateListScreen(
                onStateClick = { /* Handle state click if needed */ },
                onImportStates = { /* Handle state import */ },
                modifier = Modifier
            )
        }

        // Customer Type List Screen
        composable<CustomerTypeListRoute> {
            CustomerTypeListScreen(
                onCustomerTypeClick = { customerTypeId ->
                    navController.navigate(CustomerTypeCreateRoute(customerTypeId))
                },
                onAddCustomerType = {
                    navController.navigate(CustomerTypeCreateRoute())
                },
                modifier = Modifier
            )
        }

        // Customer Type Form Screen (Create/Edit)
        composable<CustomerTypeCreateRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<CustomerTypeCreateRoute>()
            CustomerTypeFormScreen(
                customerTypeId = route.customerTypeId,
                onSaveSuccess = { navController.popBackStack() },
                modifier = Modifier
            )
        }

        // Customer Group List Screen
        composable<CustomerGroupListRoute> {
            CustomerGroupListScreen(
                onCustomerGroupClick = { customerGroupId ->
                    navController.navigate(CustomerGroupCreateRoute(customerGroupId))
                },
                onAddCustomerGroup = {
                    navController.navigate(CustomerGroupCreateRoute())
                },
                modifier = Modifier
            )
        }

        // Customer Group Form Screen (Create/Edit)
        composable<CustomerGroupCreateRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<CustomerGroupCreateRoute>()
            CustomerGroupFormScreen(
                customerGroupId = route.customerGroupId,
                onSaveSuccess = { navController.popBackStack() },
                modifier = Modifier
            )
        }
    }
}

@Composable
fun CustomerScreen(
    onCustomerClick: (String) -> Unit,
    onCreateCustomer: () -> Unit,
    onFormConfig: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    CustomersListScreen(
        onCustomerClick = onCustomerClick,
        onCreateCustomer = onCreateCustomer,
        onFormConfig = onFormConfig,
        modifier = modifier
    )
}
