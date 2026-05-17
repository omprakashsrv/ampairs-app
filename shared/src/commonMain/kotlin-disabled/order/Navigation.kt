package com.ampairs.order

import OrderRoute
import Route
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navOptions
import androidx.navigation.navigation
import androidx.navigation.toRoute
import com.ampairs.order.ui.OrderPaneScreen
import com.ampairs.order.ui.OrderScreen
import com.ampairs.order.ui.OrderViewScreen

fun NavGraphBuilder.orderNavigation(navigator: NavController, onSuccess: () -> Unit) {

    navigation<Route.Order>(startDestination = OrderRoute.Root()) {
        composable<OrderRoute.Root> { backStackEntry ->
            val orderRoute = backStackEntry.toRoute<OrderRoute.Root>()
            OrderScreen(orderRoute.fromCustomer, orderRoute.toCustomer, orderRoute.id) { orderId ->
                navigator.navigate(
                    OrderRoute.OrderView(id = orderId),
                    navOptions = navOptions {
                        popUpTo<OrderRoute.Root> {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                )
            }
        }
        composable<OrderRoute.OrderView> { backStackEntry ->
            val orderViewRoute = backStackEntry.toRoute<OrderRoute.OrderView>()
            OrderViewScreen(orderViewRoute.id) {
                navigator.popBackStack()
            }
        }
        composable<OrderRoute.Orders> {
            OrderPaneScreen { orderId ->
                navigator.navigate(OrderRoute.Root(id = orderId ?: ""))
            }
        }
    }
}