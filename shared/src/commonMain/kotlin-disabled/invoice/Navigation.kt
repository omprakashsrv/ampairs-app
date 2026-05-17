package com.ampairs.invoice

import InvoiceRoute
import Route
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navOptions
import androidx.navigation.navigation
import androidx.navigation.toRoute
import com.ampairs.invoice.ui.InvoicePaneScreen
import com.ampairs.invoice.ui.InvoiceScreen
import com.ampairs.invoice.ui.InvoiceViewScreen

fun NavGraphBuilder.invoiceNavigation(navigator: NavController, onSuccess: () -> Unit) {

    navigation<Route.Invoice>(startDestination = InvoiceRoute.Root()) {
        composable<InvoiceRoute.Root> { backStackEntry ->
            val invoiceRoute = backStackEntry.toRoute<InvoiceRoute.Root>()
            InvoiceScreen(invoiceRoute.fromCustomer, invoiceRoute.toCustomer, invoiceRoute.id) { invoiceId ->
                navigator.navigate(
                    route = InvoiceRoute.InvoiceView(id = invoiceId),
                    navOptions = navOptions {
                        popUpTo<InvoiceRoute.Root> {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                )
            }
        }
        composable<InvoiceRoute.InvoiceView> { backStackEntry ->
            val invoiceViewRoute = backStackEntry.toRoute<InvoiceRoute.InvoiceView>()
            InvoiceViewScreen(invoiceViewRoute.id) {
                navigator.popBackStack()
            }
        }
        composable<InvoiceRoute.Invoices> {
            InvoicePaneScreen { invoiceId ->
                navigator.navigate(InvoiceRoute.Root(id = invoiceId ?: ""))
            }
        }
    }
}