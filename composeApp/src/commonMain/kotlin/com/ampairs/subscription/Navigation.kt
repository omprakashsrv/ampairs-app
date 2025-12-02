package com.ampairs.subscription

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.toRoute
import com.ampairs.subscription.ui.screens.*
import com.ampairs.subscription.viewmodel.SubscriptionViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * Subscription module navigation graph
 */
fun NavGraphBuilder.subscriptionNavigation(
    navController: NavHostController,
    onOpenCheckoutUrl: ((String) -> Unit)? = null
) {
    navigation<Route.Subscription>(startDestination = SubscriptionRoute.Root) {
        // Main subscription screen
        composable<SubscriptionRoute.Root> {
            val viewModel: SubscriptionViewModel = koinViewModel()
            SubscriptionScreen(
                viewModel = viewModel,
                onNavigateToPlanComparison = {
                    navController.navigate(SubscriptionRoute.Plans)
                },
                onNavigateToBillingHistory = {
                    navController.navigate(SubscriptionRoute.PaymentHistory)
                },
                onNavigateToDeviceManagement = {
                    navController.navigate(SubscriptionRoute.Devices)
                },
                onNavigateToUsageDetails = {
                    navController.navigate(SubscriptionRoute.Usage)
                },
                onNavigateToInvoices = {
                    navController.navigate(SubscriptionRoute.Invoices)
                },
                onNavigateToInvoiceDetail = { invoiceUid ->
                    navController.navigate(SubscriptionRoute.InvoiceDetail(invoiceUid))
                },
                onCheckoutUrl = { url ->
                    onOpenCheckoutUrl?.invoke(url)
                }
            )
        }

        // Plan comparison screen
        composable<SubscriptionRoute.Plans> {
            val viewModel: SubscriptionViewModel = koinViewModel()
            PlanComparisonScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onCheckoutUrl = { url ->
                    onOpenCheckoutUrl?.invoke(url)
                }
            )
        }

        // Usage details screen
        composable<SubscriptionRoute.Usage> {
            val viewModel: SubscriptionViewModel = koinViewModel()
            UsageDetailsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPlans = {
                    navController.navigate(SubscriptionRoute.Plans)
                }
            )
        }

        // Payment history screen
        composable<SubscriptionRoute.PaymentHistory> {
            val viewModel: SubscriptionViewModel = koinViewModel()
            PaymentHistoryScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Payment methods screen
        composable<SubscriptionRoute.PaymentMethods> {
            val viewModel: SubscriptionViewModel = koinViewModel()
            PaymentMethodsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Device management screen
        composable<SubscriptionRoute.Devices> {
            val viewModel: SubscriptionViewModel = koinViewModel()
            DeviceManagementScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPlans = {
                    navController.navigate(SubscriptionRoute.Plans)
                }
            )
        }

        // Invoice list screen
        composable<SubscriptionRoute.Invoices> {
            InvoiceListScreen(
                onNavigateToInvoiceDetail = { invoiceUid ->
                    navController.navigate(SubscriptionRoute.InvoiceDetail(invoiceUid))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Invoice detail screen
        composable<SubscriptionRoute.InvoiceDetail> { backStackEntry ->
            val route = backStackEntry.toRoute<SubscriptionRoute.InvoiceDetail>()
            InvoiceDetailScreen(
                invoiceUid = route.invoiceUid,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
