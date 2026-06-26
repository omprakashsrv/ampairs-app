package com.ampairs.agent.llm

import co.touchlab.kermit.Logger
import com.ampairs.agent.core.AgentStreamEvent
import com.ampairs.common.agent.ActionResult
import com.ampairs.common.agent.ActionType
import com.ampairs.common.agent.AgentAction
import com.google.ai.edge.litertlm.Tool
import com.google.ai.edge.litertlm.ToolParam
import com.google.ai.edge.litertlm.ToolSet
import kotlinx.coroutines.runBlocking

/**
 * The native LiteRT-LM [ToolSet] for the Ampairs assistant (Gallery's `@Tool` pattern). Each tool maps
 * to an [AgentAction] dispatched through the existing registry via [dispatch], so handlers (and their
 * validation) are reused. Propose-vs-execute is decided **generically** by the [ActionResult]:
 * `Confirm` → [AgentStreamEvent.ActionProposed] (money/destructive — the UI shows the confirm bar and
 * nothing persists until the user confirms, FR-006); `Success` → [AgentStreamEvent.ActionExecuted].
 * The string map returned to the model lets it phrase a natural reply.
 *
 * Tools run on the inference thread, so [dispatch] is bridged with [runBlocking]; [emit] forwards the
 * resulting event to the engine's stream channel.
 *
 * Curated to the high-value flows (cart → invoice/order, search, count). Anything not covered still
 * works through the rule-based resolver path.
 */
class AmpairsAgentToolSet(
    private val dispatch: suspend (AgentAction) -> ActionResult,
    private val emit: (AgentStreamEvent) -> Unit,
) : ToolSet {

    // ---- Cart (multi-item draft) ----

    @Tool(description = "Add a product line to the current bill/order draft (cart). Repeat to add multiple items before finalizing.")
    fun addItemToCart(
        @ToolParam(description = "Product name to add") product: String,
        @ToolParam(description = "Quantity (default 1)") quantity: String = "",
        @ToolParam(description = "Optional unit price override; defaults to the product's selling price") price: String = "",
    ): Map<String, String> = run(
        AgentAction(
            ActionType.ADD_ITEM, "cart",
            buildMap {
                put("product", product)
                if (quantity.isNotBlank()) put("quantity", quantity)
                if (price.isNotBlank()) put("price", price)
            },
        ),
    )

    @Tool(description = "Set the customer on the current bill/order draft (cart).")
    fun setCartCustomer(
        @ToolParam(description = "Customer name") customer: String,
    ): Map<String, String> = run(AgentAction(ActionType.SET_CUSTOMER, "cart", mapOf("customer" to customer)))

    @Tool(description = "Show the current draft (cart): items, customer and subtotal.")
    fun showCart(): Map<String, String> = run(AgentAction(ActionType.LIST, "cart", emptyMap()))

    @Tool(description = "Clear the cart, or remove only the last item (scope='last').")
    fun clearCart(
        @ToolParam(description = "Use 'last' to remove the last item; omit to clear all") scope: String = "",
    ): Map<String, String> = run(
        AgentAction(ActionType.DELETE, "cart", if (scope.isBlank()) emptyMap() else mapOf("scope" to scope)),
    )

    // ---- Finalize (money — these come back as ActionProposed, awaiting confirm) ----

    @Tool(description = "Finalize a bill/invoice for the customer, using the cart items when present.")
    fun createInvoice(
        @ToolParam(description = "Customer name to bill (optional if already set on the cart)") customer: String = "",
        @ToolParam(description = "Product for a single line item when not using the cart") product: String = "",
        @ToolParam(description = "Quantity for the single line item") quantity: String = "",
    ): Map<String, String> = run(AgentAction(ActionType.CREATE, "invoice", docParams(customer, product, quantity)))

    @Tool(description = "Finalize an order for the customer, using the cart items when present.")
    fun createOrder(
        @ToolParam(description = "Customer name (optional if already set on the cart)") customer: String = "",
        @ToolParam(description = "Product for a single line item when not using the cart") product: String = "",
        @ToolParam(description = "Quantity for the single line item") quantity: String = "",
    ): Map<String, String> = run(AgentAction(ActionType.CREATE, "order", docParams(customer, product, quantity)))

    // ---- Read-only lookups ----

    @Tool(description = "Search customers by name or phone.")
    fun searchCustomers(
        @ToolParam(description = "Search query") query: String,
    ): Map<String, String> = run(AgentAction(ActionType.SEARCH, "customer", mapOf("query" to query)))

    @Tool(description = "Search products by name.")
    fun searchProducts(
        @ToolParam(description = "Search query") query: String,
    ): Map<String, String> = run(AgentAction(ActionType.SEARCH, "product", mapOf("query" to query)))

    @Tool(description = "Get the total number of invoices.")
    fun countInvoices(): Map<String, String> = run(AgentAction(ActionType.COUNT, "invoice", emptyMap()))

    @Tool(description = "Get the total number of orders.")
    fun countOrders(): Map<String, String> = run(AgentAction(ActionType.COUNT, "order", emptyMap()))

    private fun docParams(customer: String, product: String, quantity: String): Map<String, String> = buildMap {
        if (customer.isNotBlank()) put("customer", customer)
        if (product.isNotBlank()) put("product", product)
        if (quantity.isNotBlank()) put("quantity", quantity)
    }

    /** Dispatch [action], surface the right stream event, and return a result map for the model. */
    private fun run(action: AgentAction): Map<String, String> = runBlocking {
        when (val result = dispatch(action)) {
            is ActionResult.Confirm -> {
                emit(AgentStreamEvent.ActionProposed(result.pendingAction, result.summary, result.amount))
                mapOf("status" to "needs_confirmation", "summary" to result.summary)
            }
            is ActionResult.Success -> {
                emit(AgentStreamEvent.ActionExecuted(result.summary, result, action))
                mapOf("status" to "success", "result" to result.summary)
            }
            is ActionResult.NeedsInput -> mapOf("status" to "needs_input", "question" to result.question)
            is ActionResult.Selection -> mapOf("status" to "needs_selection", "question" to result.question)
            is ActionResult.Error -> {
                Logger.w(tag = "AgentLlm") { "Tool '${action.actionType}/${action.moduleName}' failed: ${result.message}" }
                mapOf("status" to "error", "error" to result.message)
            }
        }
    }
}
