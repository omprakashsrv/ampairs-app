package com.ampairs.navigation.providers

import SubscriptionRoute
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.ampairs.subscription.ui.screens.DeviceManagementScreen
import com.ampairs.subscription.ui.screens.InvoiceDetailScreen
import com.ampairs.subscription.ui.screens.InvoiceListScreen
import com.ampairs.subscription.ui.screens.PaymentHistoryScreen
import com.ampairs.subscription.ui.screens.PaymentMethodsScreen
import com.ampairs.subscription.ui.screens.PlanComparisonScreen
import com.ampairs.subscription.ui.screens.SubscriptionScreen
import com.ampairs.subscription.ui.screens.UsageDetailsScreen
import com.ampairs.subscription.viewmodel.SubscriptionViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * Entry provider for Subscription module routes in Navigation 3.
 * Returns NavEntry for subscription routes or null if route doesn't match.
 */
fun subscriptionEntryProvider(
    key: NavKey,
    backStack: MutableList<NavKey>,
    onOpenCheckoutUrl: ((String) -> Unit)? = null
): NavEntry<NavKey>? = when (key) {
    is SubscriptionRoute.Root -> NavEntry(key) {
        val viewModel: SubscriptionViewModel = koinViewModel()
        SubscriptionScreen(
            viewModel = viewModel,
            onNavigateToPlanComparison = {
                backStack.add(SubscriptionRoute.Plans)
            },
            onNavigateToBillingHistory = {
                backStack.add(SubscriptionRoute.PaymentHistory)
            },
            onNavigateToDeviceManagement = {
                backStack.add(SubscriptionRoute.Devices)
            },
            onNavigateToUsageDetails = {
                backStack.add(SubscriptionRoute.Usage)
            },
            onNavigateToInvoices = {
                backStack.add(SubscriptionRoute.Invoices)
            },
            onNavigateToInvoiceDetail = { invoiceUid ->
                backStack.add(SubscriptionRoute.InvoiceDetail(invoiceUid))
            },
            onCheckoutUrl = { url ->
                onOpenCheckoutUrl?.invoke(url)
            }
        )
    }

    is SubscriptionRoute.Plans -> NavEntry(key) {
        val viewModel: SubscriptionViewModel = koinViewModel()
        PlanComparisonScreen(
            viewModel = viewModel,
            onNavigateBack = { backStack.removeLastOrNull() },
            onCheckoutUrl = { url ->
                onOpenCheckoutUrl?.invoke(url)
            }
        )
    }

    is SubscriptionRoute.Usage -> NavEntry(key) {
        val viewModel: SubscriptionViewModel = koinViewModel()
        UsageDetailsScreen(
            viewModel = viewModel,
            onNavigateBack = { backStack.removeLastOrNull() },
            onNavigateToPlans = {
                backStack.add(SubscriptionRoute.Plans)
            }
        )
    }

    is SubscriptionRoute.PaymentHistory -> NavEntry(key) {
        val viewModel: SubscriptionViewModel = koinViewModel()
        PaymentHistoryScreen(
            viewModel = viewModel,
            onNavigateBack = { backStack.removeLastOrNull() }
        )
    }

    is SubscriptionRoute.PaymentMethods -> NavEntry(key) {
        val viewModel: SubscriptionViewModel = koinViewModel()
        PaymentMethodsScreen(
            viewModel = viewModel,
            onNavigateBack = { backStack.removeLastOrNull() }
        )
    }

    is SubscriptionRoute.Devices -> NavEntry(key) {
        val viewModel: SubscriptionViewModel = koinViewModel()
        DeviceManagementScreen(
            viewModel = viewModel,
            onNavigateBack = { backStack.removeLastOrNull() },
            onNavigateToPlans = {
                backStack.add(SubscriptionRoute.Plans)
            }
        )
    }

    is SubscriptionRoute.Invoices -> NavEntry(key) {
        InvoiceListScreen(
            onNavigateToInvoiceDetail = { invoiceUid ->
                backStack.add(SubscriptionRoute.InvoiceDetail(invoiceUid))
            },
            onNavigateBack = { backStack.removeLastOrNull() }
        )
    }

    is SubscriptionRoute.InvoiceDetail -> NavEntry(key) {
        InvoiceDetailScreen(
            invoiceUid = key.invoiceUid,
            onNavigateBack = { backStack.removeLastOrNull() }
        )
    }

    else -> null
}
