package com.ampairs.inventory.agent

import com.ampairs.agent.core.ActionDescriptor
import com.ampairs.agent.core.ActionHandler
import com.ampairs.agent.core.ActionParameter
import com.ampairs.agent.core.ActionResult
import com.ampairs.agent.core.ActionType
import com.ampairs.agent.core.AgentAction
import com.ampairs.agent.core.ParameterType
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.Inject
import com.ampairs.inventory.db.InventoryRepository

@Inject
class InventoryActionHandler(
    private val inventoryRepository: InventoryRepository,
) : ActionHandler {

    override val moduleName = "inventory"

    override val supportedActions: List<ActionDescriptor> get() = ACTIONS

    override suspend fun execute(action: AgentAction): ActionResult = when (action.actionType) {
        ActionType.SEARCH -> searchInventory(action.params)
        ActionType.COUNT -> countInventory(action.params)
        ActionType.GET_INVENTORY -> getProductInventory(action.params)
        ActionType.LIST -> listLowStock(action.params)
        else -> ActionResult.Error("Unsupported action: ${action.actionType}")
    }

    private suspend fun searchInventory(params: Map<String, String>): ActionResult {
        val query = params["query"]
            ?: return ActionResult.NeedsInput("What product are you looking for?", listOf("query"))
        val items = inventoryRepository.inventoryDao.getInventoryByDescription(query, 10, 0)
        return if (items.isEmpty()) {
            ActionResult.Success("No inventory found matching '$query'.")
        } else {
            val summary = items.take(5).joinToString("\n") { "• ${it.description}: stock=${it.stock}" }
            ActionResult.Success("Found ${items.size} item(s):\n$summary")
        }
    }

    private suspend fun countInventory(params: Map<String, String>): ActionResult {
        val query = params["query"]
        val count = if (query != null) {
            inventoryRepository.inventoryDao.countInventoryByDescription(query)
        } else {
            inventoryRepository.inventoryDao.countInventory()
        }
        return ActionResult.Success("Total inventory items: $count")
    }

    private suspend fun getProductInventory(params: Map<String, String>): ActionResult {
        val productId = params["productId"]
            ?: return ActionResult.NeedsInput("Which product's inventory?", listOf("productId"))
        val inventory = inventoryRepository.getProductInventory(productId)
            ?: return ActionResult.Error("No inventory found for product '$productId'.")
        return ActionResult.Success("Stock for product: ${inventory.stock} ${inventory.unit?.name ?: inventory.unitId ?: "units"}")
    }

    private suspend fun listLowStock(params: Map<String, String>): ActionResult {
        val threshold = params["threshold"]?.toDoubleOrNull() ?: 10.0
        val items = inventoryRepository.inventoryDao.getLowStockItems(threshold)
        return if (items.isEmpty()) {
            ActionResult.Success("No low-stock items (threshold: $threshold).")
        } else {
            val summary = items.take(5).joinToString("\n") { "• ${it.description}: stock=${it.stock}" }
            val more = if (items.size > 5) "\n… and ${items.size - 5} more" else ""
            ActionResult.Success("${items.size} low-stock item(s):\n$summary$more")
        }
    }

    companion object {
        val ACTIONS = listOf(
            ActionDescriptor(
                actionType = ActionType.SEARCH,
                moduleName = "inventory",
                description = "Search inventory by product description",
                parameters = listOf(
                    ActionParameter("query", ParameterType.STRING, required = true, "Search query"),
                ),
            ),
            ActionDescriptor(
                actionType = ActionType.COUNT,
                moduleName = "inventory",
                description = "Count total inventory items",
                parameters = listOf(
                    ActionParameter("query", ParameterType.STRING, required = false, "Optional filter"),
                ),
            ),
            ActionDescriptor(
                actionType = ActionType.GET_INVENTORY,
                moduleName = "inventory",
                description = "Get stock level for a specific product",
                parameters = listOf(
                    ActionParameter("productId", ParameterType.STRING, required = true, "Product ID"),
                ),
            ),
            ActionDescriptor(
                actionType = ActionType.LIST,
                moduleName = "inventory",
                description = "List low-stock items below a threshold",
                parameters = listOf(
                    ActionParameter("threshold", ParameterType.NUMBER, required = false, "Stock threshold (default 10)"),
                ),
            ),
        )
    }
}
