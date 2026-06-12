package com.ampairs.navigation.providers

import InvoiceRoute
import OrderRoute
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.ampairs.order.ui.OrderPaneScreen
import com.ampairs.order.ui.OrderScreen
import com.ampairs.order.ui.OrderViewScreen
import com.ampairs.store.ui.StoreSettingsRoute

/**
 * Entry provider for Order module routes in Navigation 3.
 * Returns NavEntry for order routes or null if route doesn't match.
 */
fun orderEntryProvider(
    key: NavKey,
    backStack: MutableList<NavKey>
): NavEntry<NavKey>? = when (key) {
    is OrderRoute.Root -> NavEntry(key) {
        val fromCustomerId = key.fromCustomer.ifEmpty { null }
        val toCustomerId = key.toCustomer.ifEmpty { null }
        val id = key.id.ifEmpty { null }
        OrderScreen(
            fromCustomerId = fromCustomerId,
            toCustomerId = toCustomerId,
            id = id,
            onOrderSaved = { orderId ->
                // Land on the order view without stacking duplicates: pop the editor and,
                // when this was an edit launched FROM the view, pop the stale view too so a
                // fresh entry (fresh ViewModel) shows the just-saved data.
                backStack.removeLastOrNull()
                if ((backStack.lastOrNull() as? OrderRoute.OrderView)?.id == orderId) {
                    backStack.removeLastOrNull()
                }
                backStack.add(OrderRoute.OrderView(id = orderId))
            },
            onOpenSettings = { backStack.add(StoreSettingsRoute) }
        )
    }

    is OrderRoute.OrderView -> NavEntry(key) {
        OrderViewScreen(
            orderId = key.id,
            onOpenInvoice = { invoiceId -> backStack.add(InvoiceRoute.InvoiceView(id = invoiceId)) },
            onEdit = { orderId -> backStack.add(OrderRoute.Root(id = orderId)) },
            onNavigateBack = { backStack.removeLastOrNull() }
        )
    }

    is OrderRoute.Orders -> NavEntry(key) {
        OrderPaneScreen(
            onOpenInvoice = { invoiceId -> backStack.add(InvoiceRoute.InvoiceView(id = invoiceId)) },
            onOrderEdit = { orderId ->
                if (!orderId.isNullOrEmpty()) {
                    backStack.add(OrderRoute.Root(id = orderId))
                } else {
                    backStack.add(OrderRoute.Root())
                }
            }
        )
    }

    else -> null
}
