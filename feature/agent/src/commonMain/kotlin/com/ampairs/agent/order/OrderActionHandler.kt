package com.ampairs.agent.order

import com.ampairs.business.data.BusinessDataService
import com.ampairs.common.agent.ActionDescriptor
import com.ampairs.common.agent.ActionHandler
import com.ampairs.common.agent.ActionHandlerKey
import com.ampairs.common.agent.ActionParameter
import com.ampairs.common.agent.ActionResult
import com.ampairs.common.agent.ActionType
import com.ampairs.common.agent.AgentAction
import com.ampairs.common.agent.DraftDocumentStore
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.locale.LocalAppLocale
import com.ampairs.order.db.OrderRepository
import com.ampairs.order.db.entity.OrderEntity
import com.ampairs.order.db.entity.OrderItemEntity
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import kotlin.time.Clock

/**
 * Create an order from the conversational draft (cart). Requires customer to be set and items present.
 * Emits a confirmation action before finalizing; on confirmation, creates the order, clears the draft,
 * and queues sync automatically.
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@ActionHandlerKey("order")
class OrderActionHandler(
    private val draft: DraftDocumentStore,
    private val orderRepository: OrderRepository,
    private val businessDataService: BusinessDataService,
) : ActionHandler {

    override val moduleName = "order"

    override val supportedActions: List<ActionDescriptor> get() = ACTIONS

    override suspend fun execute(action: AgentAction): ActionResult {
        if (action.actionType != ActionType.CREATE_ORDER) {
            return ActionResult.Error("Unsupported order action: ${action.actionType}")
        }

        val validated = validate()
        if (validated != null) return validated

        return try {
            createOrder()
        } catch (e: Exception) {
            ActionResult.Error("Failed to create order: ${e.message}")
        }
    }

    private fun validate(): ActionResult? {
        if (draft.isEmpty) {
            return ActionResult.Error("Cart is empty. Add items before creating an order.")
        }
        if (draft.customerId == null || draft.customerName == null) {
            return ActionResult.Error("No customer selected. Please set a customer before creating an order.")
        }
        return null
    }

    private suspend fun createOrder(): ActionResult {
        val business = businessDataService.getBusinessProfile()
            ?: return ActionResult.Error("Unable to load business profile. Try again.")

        val now = Clock.System.now().toEpochMilliseconds()
        val orderUid = com.ampairs.common.id_generator.UidGenerator.generateUid("ORD")

        val order = OrderEntity(
            id = orderUid,
            order_number = "", // Will be auto-assigned by repository
            order_date = com.ampairs.common.locale.formatDate(
                com.ampairs.common.locale.now(),
                LocalAppLocale.Default,
            ),
            status = "PENDING",
            customer_id = draft.customerId ?: "",
            customer_name = draft.customerName ?: "",
            customer_gst = "",
            total_cost = draft.subtotal,
            total_tax = 0.0,
            total_items = draft.itemCount.toLong(),
            total_quantity = draft.totalQuantity,
            base_price = draft.subtotal,
            last_updated = now,
        )

        val items = draft.lines.map { line ->
            OrderItemEntity(
                id = com.ampairs.common.id_generator.UidGenerator.generateUid("ORI"),
                order_id = orderUid,
                product_id = line.productId,
                product_name = line.productName,
                quantity = line.quantity,
                unit_price = line.unitPrice,
                line_total = line.lineSubtotal,
                line_tax = 0.0,
                last_updated = now,
            )
        }

        orderRepository.saveOrder(order, items)

        val preview = orderRepository.nextNumberPreview()
        draft.clear()

        return ActionResult.Success(
            summary = "Order $preview created for ${draft.customerName} with ${draft.itemCount} item(s). " +
                "Total: ₹${draft.subtotal.toLong()}. Order will sync when you're online.",
            amount = draft.subtotal,
        )
    }

    companion object {
        val ACTIONS = listOf(
            ActionDescriptor(
                actionType = ActionType.CREATE_ORDER,
                moduleName = "order",
                description = "Create an order from the current draft (cart). Customer and items must be set.",
                parameters = emptyList(),
            ),
        )
    }
}
