package com.ampairs.navigation.providers

import SubscriptionRoute
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
import dev.zacsweers.metrox.viewmodel.metroViewModel

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
        SubscriptionScreen(
            viewModel = metroViewModel<SubscriptionViewModel>(),
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
        PlanComparisonScreen(
            viewModel = metroViewModel<SubscriptionViewModel>(),
            onNavigateBack = { backStack.removeLastOrNull() },
            onCheckoutUrl = { url ->
                onOpenCheckoutUrl?.invoke(url)
            }
        )
    }

    is SubscriptionRoute.Usage -> NavEntry(key) {
        UsageDetailsScreen(
            viewModel = metroViewModel<SubscriptionViewModel>(),
            onNavigateBack = { backStack.removeLastOrNull() },
            onNavigateToPlans = {
                backStack.add(SubscriptionRoute.Plans)
            }
        )
    }

    is SubscriptionRoute.PaymentHistory -> NavEntry(key) {
        PaymentHistoryScreen(
            viewModel = metroViewModel<SubscriptionViewModel>(),
            onNavigateBack = { backStack.removeLastOrNull() }
        )
    }

    is SubscriptionRoute.PaymentMethods -> NavEntry(key) {
        PaymentMethodsScreen(
            viewModel = metroViewModel<SubscriptionViewModel>(),
            onNavigateBack = { backStack.removeLastOrNull() }
        )
    }

    is SubscriptionRoute.Devices -> NavEntry(key) {
        DeviceManagementScreen(
            viewModel = metroViewModel<SubscriptionViewModel>(),
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
            onNavigateBack = { backStack.removeLastOrNull() },
        )
    }

    is SubscriptionRoute.InvoiceDetail -> NavEntry(key) {
        InvoiceDetailScreen(
            invoiceUid = key.invoiceUid,
            onNavigateBack = { backStack.removeLastOrNull() },
        )
    }

    // Plan details for a specific subscription plan
    is SubscriptionRoute.PlanDetails -> NavEntry(key) {
        // Show plan comparison with specific plan highlighted
        PlanComparisonScreen(
            viewModel = metroViewModel<SubscriptionViewModel>(),
            onNavigateBack = { backStack.removeLastOrNull() },
            onCheckoutUrl = { url ->
                onOpenCheckoutUrl?.invoke(url)
            }
        )
    }

    // Checkout flow for subscription purchase
    is SubscriptionRoute.Checkout -> NavEntry(key) {
        SubscriptionCheckoutPlaceholder(
            planCode = key.planCode,
            billingCycle = key.billingCycle,
            onNavigateBack = { backStack.removeLastOrNull() }
        )
    }

    else -> null
}

/**
 * Placeholder for checkout screen until full implementation.
 */
@Composable
private fun SubscriptionCheckoutPlaceholder(
    planCode: String,
    billingCycle: String,
    onNavigateBack: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Checkout",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "Plan: $planCode",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Billing: $billingCycle",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Checkout flow coming soon",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
