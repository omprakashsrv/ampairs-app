package com.ampairs.customer.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import Route
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
object CustomerListRoute

@Serializable
data class CustomerDetailsRoute(val customerId: String)

@Serializable
data class CustomerCreateRoute(val customerId: String? = null)

@Serializable
object StateListRoute

@Serializable
object CustomerTypeListRoute

@Serializable
data class CustomerTypeCreateRoute(val customerTypeId: String? = null)

@Serializable
object CustomerGroupListRoute

@Serializable
data class CustomerGroupCreateRoute(val customerGroupId: String? = null)


fun NavGraphBuilder.customerNavigation(navController: NavHostController) {
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

    composable<CustomerCreateRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<CustomerCreateRoute>()
        CustomerFormScreen(
            customerId = route.customerId,
            onSaveSuccess = { navController.popBackStack() },
            modifier = Modifier
        )
    }

    composable<StateListRoute> {
        StateListScreen(
            onStateClick = { /* Handle state click if needed */ },
            onImportStates = { /* Handle state import */ },
            modifier = Modifier
        )
    }

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

    composable<CustomerTypeCreateRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<CustomerTypeCreateRoute>()
        CustomerTypeFormScreen(
            customerTypeId = route.customerTypeId,
            onSaveSuccess = { navController.popBackStack() },
            modifier = Modifier
        )
    }

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

    composable<CustomerGroupCreateRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<CustomerGroupCreateRoute>()
        CustomerGroupFormScreen(
            customerGroupId = route.customerGroupId,
            onSaveSuccess = { navController.popBackStack() },
            modifier = Modifier
        )
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
