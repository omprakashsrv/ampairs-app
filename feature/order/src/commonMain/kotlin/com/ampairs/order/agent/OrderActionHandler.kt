package com.ampairs.order.agent

import com.ampairs.common.agent.ActionDescriptor
import com.ampairs.common.agent.ActionHandler
import com.ampairs.common.agent.ActionParameter
import com.ampairs.common.agent.ActionResult
import com.ampairs.common.agent.ActionType
import com.ampairs.common.agent.AgentAction
import com.ampairs.common.agent.NavigationTarget
import com.ampairs.common.agent.ParameterType
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.Inject
import com.ampairs.order.db.OrderRepository

@Inject
class OrderActionHandler(
    private val orderRepository: OrderRepository,
) : ActionHandler {

    override val moduleName = "order"

    override val supportedActions: List<ActionDescriptor> get() = ACTIONS

    override suspend fun execute(action: AgentAction): ActionResult = when (action.actionType) {
        ActionType.SEARCH -> searchOrders(action.params)
        ActionType.READ -> getOrder(action.params)
        ActionType.COUNT -> countOrders()
        ActionType.LIST -> listOrders(action.params)
        else -> ActionResult.Error("Unsupported action: ${action.actionType}")
    }

    private suspend fun searchOrders(params: Map<String, String>): ActionResult {
        val query = params["query"]
            ?: return ActionResult.NeedsInput("What do you want to search for?", listOf("query"))
        val orders = orderRepository.orderDao.getOrdersByNumber(query) +
                orderRepository.orderDao.getOrdersByCustomerName(query)
        val unique = orders.distinctBy { it.id }
        return if (unique.isEmpty()) {
            ActionResult.Success("No orders found matching '$query'.")
        } else {
            val summary = unique.take(5).joinToString("\n") { "• #${it.order_number} — ${it.status}" }
            ActionResult.Success("Found ${unique.size} order(s):\n$summary")
        }
    }

    private suspend fun getOrder(params: Map<String, String>): ActionResult {
        val id = params["orderId"] ?: params["searchName"]
            ?: return ActionResult.NeedsInput("Which order? Provide an order number or ID.", listOf("orderId"))
        val order = orderRepository.orderDao.selectById(id)
            ?: orderRepository.orderDao.getOrdersByNumber(id).firstOrNull()
            ?: return ActionResult.Error("Order '$id' not found.")
        return ActionResult.Success(
            summary = "Order #${order.order_number}: status=${order.status}",
            navigationTarget = NavigationTarget(
                routeDescription = "OrderView",
                routeData = mapOf("orderId" to order.id),
            ),
        )
    }

    private suspend fun countOrders(): ActionResult {
        val count = orderRepository.orderDao.countOrders()
        return ActionResult.Success("You have $count order(s).")
    }

    private suspend fun listOrders(params: Map<String, String>): ActionResult {
        val status = params["status"]
        val orders = if (status != null) {
            orderRepository.orderDao.getOrdersByStatus(status)
        } else {
            orderRepository.orderDao.selectAll()
        }
        return if (orders.isEmpty()) {
            ActionResult.Success("No orders found.")
        } else {
            val summary = orders.take(5).joinToString("\n") { "• #${it.order_number} — ${it.status}" }
            val more = if (orders.size > 5) "\n… and ${orders.size - 5} more" else ""
            ActionResult.Success("${orders.size} order(s):\n$summary$more")
        }
    }

    companion object {
        val ACTIONS = listOf(
            ActionDescriptor(
                actionType = ActionType.SEARCH,
                moduleName = "order",
                description = "Search orders by number or customer name",
                parameters = listOf(
                    ActionParameter("query", ParameterType.STRING, required = true, "Search query"),
                ),
            ),
            ActionDescriptor(
                actionType = ActionType.READ,
                moduleName = "order",
                description = "Get order details by ID or order number",
                parameters = listOf(
                    ActionParameter("orderId", ParameterType.STRING, required = false, "Order ID"),
                    ActionParameter("searchName", ParameterType.STRING, required = false, "Order number to search"),
                ),
            ),
            ActionDescriptor(
                actionType = ActionType.COUNT,
                moduleName = "order",
                description = "Get total order count",
                parameters = emptyList(),
            ),
            ActionDescriptor(
                actionType = ActionType.LIST,
                moduleName = "order",
                description = "List orders, optionally filtered by status",
                parameters = listOf(
                    ActionParameter("status", ParameterType.STRING, required = false, "Order status filter"),
                ),
            ),
        )
    }
}
