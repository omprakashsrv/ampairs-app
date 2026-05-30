package com.ampairs.navigation.providers

import Route
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.ampairs.customer.ui.CustomerCreateRoute
import com.ampairs.customer.ui.CustomerDetailsRoute
import com.ampairs.customer.ui.CustomerGroupCreateRoute
import com.ampairs.customer.ui.CustomerGroupListRoute
import com.ampairs.customer.ui.CustomerListRoute
import com.ampairs.customer.ui.CustomerTypeCreateRoute
import com.ampairs.customer.ui.CustomerTypeListRoute
import com.ampairs.customer.ui.StateListRoute
import com.ampairs.customer.ui.create.CustomerFormScreen
import com.ampairs.customer.ui.customergroup.CustomerGroupFormScreen
import com.ampairs.customer.ui.customergroup.CustomerGroupListScreen
import com.ampairs.customer.ui.customertype.CustomerTypeFormScreen
import com.ampairs.customer.ui.customertype.CustomerTypeListScreen
import com.ampairs.customer.ui.details.CustomerDetailsScreen
import com.ampairs.customer.ui.list.CustomersListScreen
import com.ampairs.customer.ui.state.StateListScreen

/**
 * Entry provider for Customer module routes in Navigation 3.
 * Returns NavEntry for customer routes or null if route doesn't match.
 */
fun customerEntryProvider(
    key: NavKey,
    backStack: MutableList<NavKey>
): NavEntry<NavKey>? = when (key) {
    is CustomerListRoute -> NavEntry(key) {
        CustomersListScreen(
            onCustomerClick = { customerId ->
                backStack.add(CustomerDetailsRoute(customerId))
            },
            onCreateCustomer = {
                backStack.add(CustomerCreateRoute())
            },
            onFormConfig = {
                backStack.add(Route.FormConfig("customer"))
            },
            modifier = Modifier
        )
    }

    is CustomerDetailsRoute -> NavEntry(key) {
        CustomerDetailsScreen(
            customerId = key.customerId,
            onNavigateBack = { backStack.removeLastOrNull() },
            onEditCustomer = { customerId ->
                backStack.add(CustomerCreateRoute(customerId))
            },
            modifier = Modifier
        )
    }

    is CustomerCreateRoute -> NavEntry(key) {
        CustomerFormScreen(
            customerId = key.customerId,
            onSaveSuccess = { backStack.removeLastOrNull() },
            modifier = Modifier
        )
    }

    is StateListRoute -> NavEntry(key) {
        StateListScreen(
            onStateClick = { },
            modifier = Modifier
        )
    }

    is CustomerTypeListRoute -> NavEntry(key) {
        CustomerTypeListScreen(
            onCustomerTypeClick = { customerTypeId ->
                backStack.add(CustomerTypeCreateRoute(customerTypeId))
            },
            onAddCustomerType = {
                backStack.add(CustomerTypeCreateRoute())
            },
            modifier = Modifier
        )
    }

    is CustomerTypeCreateRoute -> NavEntry(key) {
        CustomerTypeFormScreen(
            customerTypeId = key.customerTypeId,
            onSaveSuccess = { backStack.removeLastOrNull() },
            modifier = Modifier
        )
    }

    is CustomerGroupListRoute -> NavEntry(key) {
        CustomerGroupListScreen(
            onCustomerGroupClick = { customerGroupId ->
                backStack.add(CustomerGroupCreateRoute(customerGroupId))
            },
            onAddCustomerGroup = {
                backStack.add(CustomerGroupCreateRoute())
            },
            modifier = Modifier
        )
    }

    is CustomerGroupCreateRoute -> NavEntry(key) {
        CustomerGroupFormScreen(
            customerGroupId = key.customerGroupId,
            onSaveSuccess = { backStack.removeLastOrNull() },
            modifier = Modifier
        )
    }

    else -> null
}
