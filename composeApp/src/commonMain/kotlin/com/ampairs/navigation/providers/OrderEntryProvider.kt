package com.ampairs.navigation.providers

import OrderRoute
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.ampairs.order.ui.OrderPaneScreen
import com.ampairs.order.ui.OrderScreen
import com.ampairs.order.ui.OrderViewScreen

/**
 * Entry provider for Order module routes in Navigation 3.
 * Returns NavEntry for order routes or null if route doesn't match.
 */
fun orderEntryProvider(
    key: NavKey,
    backStack: MutableList<NavKey>
): NavEntry<NavKey>? = when (key) {
    is OrderRoute.Root -> NavEntry(key) {
        OrderScreen(
            fromCustomerId = key.fromCustomer.ifEmpty { null },
            toCustomerId = key.toCustomer.ifEmpty { null },
            id = key.id.ifEmpty { null },
            onOrderSaved = { orderId ->
                backStack.add(OrderRoute.OrderView(id = orderId))
            }
        )
    }

    is OrderRoute.OrderView -> NavEntry(key) {
        OrderViewScreen(
            orderId = key.id,
            onNavigateBack = { orderId ->
                if (!orderId.isNullOrEmpty()) {
                    backStack.add(OrderRoute.Root(id = orderId))
                } else {
                    backStack.removeLastOrNull()
                }
            }
        )
    }

    is OrderRoute.Orders -> NavEntry(key) {
        OrderPaneScreen(
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
