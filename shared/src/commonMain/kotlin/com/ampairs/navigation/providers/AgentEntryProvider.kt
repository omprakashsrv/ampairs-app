package com.ampairs.navigation.providers

import InvoiceRoute
import OrderRoute
import ProductRoute
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.ampairs.agent.ui.ChatScreen
import com.ampairs.customer.ui.CustomerDetailsRoute
import Route

fun agentEntryProvider(
    key: NavKey,
    backStack: MutableList<NavKey>,
): NavEntry<NavKey>? = when (key) {
    is Route.Agent -> NavEntry(key) {
        ChatScreen(
            onNavigateToRoute = { routeData ->
                when {
                    routeData.containsKey("customerId") -> {
                        val id = routeData["customerId"] ?: return@ChatScreen
                        backStack.add(CustomerDetailsRoute(id))
                    }
                    routeData.containsKey("productId") -> {
                        val id = routeData["productId"] ?: return@ChatScreen
                        backStack.add(ProductRoute.ProductDetails(id))
                    }
                    routeData.containsKey("orderId") -> {
                        val id = routeData["orderId"] ?: return@ChatScreen
                        backStack.add(OrderRoute.OrderView(id))
                    }
                    routeData.containsKey("invoiceId") -> {
                        val id = routeData["invoiceId"] ?: return@ChatScreen
                        backStack.add(InvoiceRoute.InvoiceView(id))
                    }
                }
            },
            modifier = Modifier,
        )
    }
    else -> null
}
