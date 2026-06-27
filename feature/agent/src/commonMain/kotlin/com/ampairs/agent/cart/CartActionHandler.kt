package com.ampairs.agent.cart

import com.ampairs.common.agent.ActionDescriptor
import com.ampairs.common.agent.ActionHandler
import com.ampairs.common.agent.ActionHandlerKey
import com.ampairs.common.agent.ActionParameter
import com.ampairs.common.agent.ActionResult
import com.ampairs.common.agent.ActionType
import com.ampairs.common.agent.AgentAction
import com.ampairs.common.agent.DraftDocumentStore
import com.ampairs.common.agent.DraftLine
import com.ampairs.common.agent.ParameterType
import com.ampairs.common.agent.SelectionOption
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.customer.data.CustomerDataService
import com.ampairs.product.data.ProductDataService
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

/**
 * Conversational document builder ("cart"). Lets the user keep adding line items and set a customer
 * across multiple turns; the invoice/order CREATE handlers then finalize the accumulated draft from
 * [DraftDocumentStore]. Product/customer resolution is a deterministic DB search (not the model) —
 * the LLM only extracts the action + slot values; this handler matches them against local data.
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@ActionHandlerKey("cart")
class CartActionHandler(
    private val draft: DraftDocumentStore,
    private val customerDataService: CustomerDataService,
    private val productDataService: ProductDataService,
) : ActionHandler {

    override val moduleName = "cart"

    override val supportedActions: List<ActionDescriptor> get() = ACTIONS

    override suspend fun execute(action: AgentAction): ActionResult = when (action.actionType) {
        ActionType.ADD_ITEM -> addItem(action.params)
        ActionType.SET_CUSTOMER -> setCustomer(action.params)
        ActionType.LIST -> showCart()
        ActionType.DELETE -> clearOrRemove(action.params)
        else -> ActionResult.Error("Unsupported cart action: ${action.actionType}")
    }

    private suspend fun addItem(params: Map<String, String>): ActionResult {
        val name = (params["product"] ?: params["item"] ?: params["name"])?.trim()
        if (name.isNullOrBlank()) {
            return ActionResult.NeedsInput("Which product do you want to add?", listOf("product"))
        }
        val products = productDataService.searchSummaries(name, limit = 10)
        if (products.isEmpty()) {
            return ActionResult.Error("No product matching \"$name\". Add the product first, then try again.")
        }
        val product = products.firstOrNull { it.name.equals(name, ignoreCase = true) }
            ?: products.singleOrNull()
            ?: run {
                // Multiple candidates: return selection dialog instead of text list
                val options = products.take(10).map { p ->
                    SelectionOption(
                        id = p.id,
                        label = p.name,
                        secondaryLabel = "${formatNum(p.sellingPrice)} each",
                    )
                }
                val pendingAction = AgentAction(
                    actionType = ActionType.ADD_ITEM,
                    moduleName = "cart",
                    params = params,
                )
                return ActionResult.Selection(
                    question = "Which product did you mean?",
                    paramName = "product",
                    options = options,
                    pendingAction = pendingAction,
                )
            }
        val qty = params["quantity"]?.trim()?.toDoubleOrNull()?.takeIf { it > 0.0 } ?: 1.0
        val unitPrice = params["price"]?.trim()?.toDoubleOrNull()?.takeIf { it >= 0.0 } ?: product.sellingPrice
        draft.addLine(DraftLine(product.id, product.name, qty, unitPrice))
        return ActionResult.Success(
            summary = "Added ${formatQty(qty)} × ${product.name}. Cart now has ${draft.itemCount} item(s). " +
                "Add more, set a customer, or say \"create invoice\" / \"create order\".",
            amount = draft.subtotal,
        )
    }

    private suspend fun setCustomer(params: Map<String, String>): ActionResult {
        val name = (params["customer"] ?: params["name"])?.trim()
        if (name.isNullOrBlank()) {
            return ActionResult.NeedsInput("Who is this for? Say the customer name.", listOf("customer"))
        }
        val candidates = customerDataService.listCustomers(name)
        if (candidates.isEmpty()) {
            return ActionResult.Error("No customer matching \"$name\". Add the customer first, then try again.")
        }
        val picked = candidates.firstOrNull { it.name.equals(name, ignoreCase = true) }
            ?: candidates.singleOrNull()
            ?: run {
                // Multiple candidates: return selection dialog instead of text list
                val options = candidates.take(10).map { c ->
                    SelectionOption(
                        id = c.id,
                        label = c.name,
                        secondaryLabel = c.phone,
                    )
                }
                val pendingAction = AgentAction(
                    actionType = ActionType.SET_CUSTOMER,
                    moduleName = "cart",
                    params = params,
                )
                return ActionResult.Selection(
                    question = "Which customer did you mean?",
                    paramName = "customer",
                    options = options,
                    pendingAction = pendingAction,
                )
            }
        draft.setCustomer(picked.id, picked.name)
        val cartPart = if (draft.isEmpty) {
            "Cart is empty — add items like \"add 2 widgets\"."
        } else {
            "Cart has ${draft.itemCount} item(s). Say \"create invoice\" or \"create order\" when ready."
        }
        return ActionResult.Success("Billing to ${picked.name}. $cartPart")
    }

    private fun showCart(): ActionResult {
        if (draft.isEmpty && draft.customerName == null) {
            return ActionResult.Success(
                "Your cart is empty. Add items like \"add 2 widgets\", set a customer, then say \"create invoice\".",
            )
        }
        val lines = draft.lines.joinToString("\n") {
            "• ${formatQty(it.quantity)} × ${it.productName} (${formatNum(it.unitPrice)} each)"
        }
        val who = draft.customerName?.let { "Customer: $it" } ?: "No customer set yet (say \"customer NAME\")."
        val tail = if (draft.isEmpty) "" else "\nSay \"create invoice\" or \"create order\" to finalize."
        return ActionResult.Success(
            summary = (lines.ifBlank { "(no items)" } + "\n" + who + tail),
            amount = if (draft.isEmpty) null else draft.subtotal,
        )
    }

    private fun clearOrRemove(params: Map<String, String>): ActionResult {
        return if (params["scope"]?.lowercase() == "last") {
            val removed = draft.removeLast()
            if (removed == null) {
                ActionResult.Success("The cart is already empty.")
            } else {
                ActionResult.Success(
                    summary = "Removed ${formatQty(removed.quantity)} × ${removed.productName}. " +
                        "Cart now has ${draft.itemCount} item(s).",
                    amount = if (draft.isEmpty) null else draft.subtotal,
                )
            }
        } else {
            draft.clear()
            ActionResult.Success("Cart cleared.")
        }
    }

    private fun formatQty(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()

    private fun formatNum(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()

    companion object {
        val ACTIONS = listOf(
            ActionDescriptor(
                actionType = ActionType.ADD_ITEM,
                moduleName = "cart",
                description = "Add a product line to the current bill/order draft (cart). Repeat to add multiple items before finalizing.",
                parameters = listOf(
                    ActionParameter("product", ParameterType.STRING, required = true, "Product name to add"),
                    ActionParameter("quantity", ParameterType.NUMBER, required = false, "Quantity (default 1)"),
                    ActionParameter("price", ParameterType.NUMBER, required = false, "Optional unit price override; defaults to the product's selling price"),
                ),
            ),
            ActionDescriptor(
                actionType = ActionType.SET_CUSTOMER,
                moduleName = "cart",
                description = "Set the customer on the current draft (cart)",
                parameters = listOf(
                    ActionParameter("customer", ParameterType.STRING, required = true, "Customer name"),
                ),
            ),
            ActionDescriptor(
                actionType = ActionType.LIST,
                moduleName = "cart",
                description = "Show the current draft (cart): items, customer, and subtotal",
                parameters = emptyList(),
            ),
            ActionDescriptor(
                actionType = ActionType.DELETE,
                moduleName = "cart",
                description = "Clear the cart, or remove only the last item (scope=last)",
                parameters = listOf(
                    ActionParameter("scope", ParameterType.STRING, required = false, "Use 'last' to remove the last item; omit to clear all"),
                ),
            ),
        )
    }
}
