package com.ampairs.inventory.agent

import com.ampairs.common.agent.ActionDescriptor
import com.ampairs.common.agent.ActionHandler
import com.ampairs.common.agent.ActionParameter
import com.ampairs.common.agent.ActionResult
import com.ampairs.common.agent.ActionType
import com.ampairs.common.agent.AgentAction
import com.ampairs.common.agent.ParameterType
import com.ampairs.common.agent.ActionHandlerKey
import com.ampairs.common.di.WorkspaceScope
import dev.zacsweers.metro.ContributesIntoMap
import com.ampairs.inventory.data.InventoryDataService
import com.ampairs.inventory.data.repository.InventoryItemRepository
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.first

@Inject
@ContributesIntoMap(WorkspaceScope::class)
@ActionHandlerKey("inventory")
class InventoryActionHandler(
    private val inventoryItemRepository: InventoryItemRepository,
    private val inventoryDataService: InventoryDataService,
    private val agentDao: InventoryAgentDao,
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
        val items = inventoryItemRepository.searchItems(query).first()
        return if (items.isEmpty()) {
            ActionResult.Success("No inventory found matching '$query'.")
        } else {
            val summary = items.take(5).joinToString("\n") { "• ${it.name}: stock=${it.currentStock}" }
            ActionResult.Success("Found ${items.size} item(s):\n$summary")
        }
    }

    private suspend fun countInventory(params: Map<String, String>): ActionResult {
        val query = params["query"]
        val count = if (query != null) {
            inventoryItemRepository.searchItems(query).first().size
        } else {
            agentDao.countActive()
        }
        return ActionResult.Success("Total inventory items: $count")
    }

    private suspend fun getProductInventory(params: Map<String, String>): ActionResult {
        val productId = params["productId"]
            ?: return ActionResult.NeedsInput("Which product's inventory?", listOf("productId"))
        val stock = inventoryDataService.getStock(productId)
            ?: return ActionResult.Error("No inventory found for product '$productId'.")
        return ActionResult.Success("Stock for product: ${stock.onHand} (available ${stock.available})")
    }

    private suspend fun listLowStock(params: Map<String, String>): ActionResult {
        val items = inventoryItemRepository.observeLowStock().first()
        return if (items.isEmpty()) {
            ActionResult.Success("No low-stock items.")
        } else {
            val summary = items.take(5).joinToString("\n") { "• ${it.name}: stock=${it.currentStock} (reorder ${it.reorderLevel})" }
            val more = if (items.size > 5) "\n… and ${items.size - 5} more" else ""
            ActionResult.Success("${items.size} low-stock item(s):\n$summary$more")
        }
    }

    companion object {
        val ACTIONS = listOf(
            ActionDescriptor(
                actionType = ActionType.SEARCH,
                moduleName = "inventory",
                description = "Search inventory by product name or SKU",
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
                description = "List low-stock items",
                parameters = listOf(
                    ActionParameter("threshold", ParameterType.NUMBER, required = false, "Unused; low stock uses each item's reorder level"),
                ),
            ),
        )
    }
}
