package com.ampairs.navigation.providers

import InvoiceRoute
import OrderRoute
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.ampairs.invoice.ui.InvoicePaneScreen
import com.ampairs.invoice.ui.InvoiceScreen
import com.ampairs.invoice.ui.InvoiceViewScreen
import com.ampairs.store.ui.StoreSettingsRoute

/**
 * Entry provider for Invoice module routes in Navigation 3.
 * Returns NavEntry for invoice routes or null if route doesn't match.
 */
fun invoiceEntryProvider(
    key: NavKey,
    backStack: MutableList<NavKey>
): NavEntry<NavKey>? = when (key) {
    is InvoiceRoute.Root -> NavEntry(key) {
        InvoiceScreen(
            fromCustomerId = key.fromCustomer.ifEmpty { null },
            toCustomerId = key.toCustomer.ifEmpty { null },
            id = key.id.ifEmpty { null },
            onInvoiceSaved = { invoiceId ->
                // Land on the invoice view without stacking duplicates (see OrderEntryProvider).
                backStack.removeLastOrNull()
                if ((backStack.lastOrNull() as? InvoiceRoute.InvoiceView)?.id == invoiceId) {
                    backStack.removeLastOrNull()
                }
                backStack.add(InvoiceRoute.InvoiceView(id = invoiceId))
            },
            onOpenSettings = { backStack.add(StoreSettingsRoute) }
        )
    }

    is InvoiceRoute.InvoiceView -> NavEntry(key) {
        InvoiceViewScreen(
            invoiceId = key.id,
            onOpenOrder = { orderId -> backStack.add(OrderRoute.OrderView(id = orderId)) },
            onEdit = { invoiceId -> backStack.add(InvoiceRoute.Root(id = invoiceId)) },
            onNavigateBack = { backStack.removeLastOrNull() }
        )
    }

    is InvoiceRoute.Invoices -> NavEntry(key) {
        InvoicePaneScreen(
            onOpenOrder = { orderId -> backStack.add(OrderRoute.OrderView(id = orderId)) },
            onInvoiceEdit = { invoiceId ->
                if (!invoiceId.isNullOrEmpty()) {
                    backStack.add(InvoiceRoute.Root(id = invoiceId))
                } else {
                    backStack.add(InvoiceRoute.Root())
                }
            }
        )
    }

    else -> null
}
