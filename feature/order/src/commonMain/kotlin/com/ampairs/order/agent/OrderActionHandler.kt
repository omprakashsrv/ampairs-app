package com.ampairs.order.agent

import com.ampairs.common.agent.ActionDescriptor
import com.ampairs.common.agent.ActionHandler
import com.ampairs.common.agent.ActionHandlerKey
import com.ampairs.common.agent.ActionParameter
import com.ampairs.common.agent.ActionResult
import com.ampairs.common.agent.ActionType
import com.ampairs.common.agent.AgentAction
import com.ampairs.common.agent.CONFIRMED_PARAM
import com.ampairs.common.agent.DraftDocumentStore
import com.ampairs.common.agent.NavigationTarget
import com.ampairs.common.agent.ParameterType
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.customer.domain.Customer
import com.ampairs.order.db.OrderRepository
import com.ampairs.order.domain.Order
import com.ampairs.order.domain.OrderItem
import com.ampairs.order.domain.OrderStatus
import com.ampairs.order.domain.TaxInfo
import com.ampairs.order.domain.TaxSpec
import com.ampairs.product.domain.ProductSummary
import com.ampairs.store.domain.StoreSettingsProvider
import com.ampairs.tax.calculation.document.DocumentCalcInput
import com.ampairs.tax.calculation.document.DocumentTotalsCalculator
import com.ampairs.tax.calculation.document.LineCalcInput
import com.ampairs.tax.calculation.document.OverallDiscountMode
import com.ampairs.tax.calculation.document.PriceMode
import com.ampairs.tax.calculation.document.ScenarioResolver
import com.ampairs.tax.calculation.document.TaxRateProvider
import com.ampairs.tax.calculation.document.TaxScenario
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

@Inject
@ContributesIntoMap(WorkspaceScope::class)
@ActionHandlerKey("order")
class OrderActionHandler(
    private val orderRepository: OrderRepository,
    private val taxRateProvider: TaxRateProvider,
    private val storeSettings: StoreSettingsProvider,
    private val draft: DraftDocumentStore,
) : ActionHandler {

    override val moduleName = "order"

    override val supportedActions: List<ActionDescriptor> get() = ACTIONS

    override suspend fun execute(action: AgentAction): ActionResult = when (action.actionType) {
        ActionType.CREATE -> createOrder(action.params)
        ActionType.SEARCH -> searchOrders(action.params)
        ActionType.READ -> getOrder(action.params)
        ActionType.COUNT -> countOrders()
        ActionType.LIST -> listOrders(action.params)
        else -> ActionResult.Error("Unsupported action: ${action.actionType}")
    }

    /**
     * Create a DRAFT order in two phases (confirm-before-write, FR-006). Uses the conversational cart
     * ([DraftDocumentStore]) when it has items; otherwise falls back to an optional single line item.
     * Totals/taxes are computed with the same canonical [DocumentTotalsCalculator] the order editor
     * uses, so the assistant's total matches the editor on open.
     */
    private suspend fun createOrder(params: Map<String, String>): ActionResult {
        val confirmed = params[CONFIRMED_PARAM] == "true"
        val cartMode = params["fromCart"] == "true" || (!confirmed && !draft.isEmpty)
        return when {
            cartMode && confirmed -> persistFromCart(params)
            cartMode -> proposeFromCart(params)
            confirmed -> persistOrder(params)
            else -> proposeOrder(params)
        }
    }

    private suspend fun proposeFromCart(params: Map<String, String>): ActionResult {
        if (draft.isEmpty) {
            return ActionResult.NeedsInput("Your cart is empty. Add items like \"add 2 widgets\" first.", listOf("product"))
        }
        val customerId = draft.customerId ?: resolveCustomerId(params)
            ?: return ActionResult.NeedsInput("Who is the order for? Say \"customer NAME\".", listOf("customer"))
        val customer = orderRepository.customerDataService.getById(customerId)
            ?: return ActionResult.Error("Couldn't load the customer — please try again.")
        val items = buildItemsFromCart()
        if (items.isEmpty()) return ActionResult.Error("Couldn't resolve the cart items — please try again.")

        val preview = buildDraft(customer, items)
        applyTaxes(preview)
        return ActionResult.Confirm(
            summary = "Create a draft order for ${customer.name} with ${items.size} item(s) (${formatQty(draft.totalQuantity)} unit(s))?",
            pendingAction = AgentAction(
                ActionType.CREATE,
                moduleName,
                mapOf(CONFIRMED_PARAM to "true", "fromCart" to "true", "customerId" to customerId),
            ),
            amount = preview.totalCost,
        )
    }

    private suspend fun persistFromCart(params: Map<String, String>): ActionResult {
        if (draft.isEmpty) return ActionResult.Error("The cart is empty — nothing to create.")
        val customerId = params["customerId"] ?: draft.customerId
            ?: return ActionResult.Error("Lost track of the customer — please try again.")
        val customer = orderRepository.customerDataService.getById(customerId)
            ?: return ActionResult.Error("Couldn't load the customer — please try again.")
        val items = buildItemsFromCart()
        if (items.isEmpty()) return ActionResult.Error("Couldn't resolve the cart items — please try again.")

        val order = buildDraft(customer, items)
        applyTaxes(order)
        return try {
            orderRepository.saveOrder(order)
            draft.clear()
            ActionResult.Success(
                summary = "Draft order created for ${customer.name} with ${items.size} item(s). Open it to review and finalize.",
                navigationTarget = NavigationTarget("OrderView", mapOf("orderId" to order.id)),
                amount = order.totalCost,
            )
        } catch (e: Exception) {
            ActionResult.Error("Couldn't create the order: ${e.message}")
        }
    }

    private suspend fun proposeOrder(params: Map<String, String>): ActionResult {
        val customerName = (params["customer"] ?: params["customerName"] ?: params["name"])?.trim()
        if (customerName.isNullOrBlank()) {
            return ActionResult.NeedsInput("Who is the order for?", listOf("customer"))
        }
        val candidates = orderRepository.customerDataService.listCustomers(customerName)
        if (candidates.isEmpty()) {
            return ActionResult.Error("No customer matching \"$customerName\". Add the customer first, then try again.")
        }
        val picked = candidates.firstOrNull { it.name.equals(customerName, ignoreCase = true) }
            ?: candidates.singleOrNull()
            ?: return ActionResult.NeedsInput(
                "Which customer did you mean? " + candidates.take(5).joinToString(", ") { it.name },
                listOf("customer"),
            )
        val customer = orderRepository.customerDataService.getById(picked.id)
            ?: return ActionResult.Error("Couldn't load customer \"${picked.name}\".")

        val productName = (params["product"] ?: params["item"])?.trim()
        var resolvedProduct: ProductSummary? = null
        var resolvedQty = 1.0
        val lineItem: OrderItem? = if (productName.isNullOrBlank()) {
            null
        } else {
            resolvedQty = params["quantity"]?.trim()?.toDoubleOrNull()?.takeIf { it > 0.0 } ?: 1.0
            val products = orderRepository.productDataService.searchSummaries(productName, limit = 10)
            if (products.isEmpty()) return ActionResult.Error("No product matching \"$productName\".")
            val product = products.firstOrNull { it.name.equals(productName, ignoreCase = true) }
                ?: products.singleOrNull()
                ?: return ActionResult.NeedsInput(
                    "Which product did you mean? " + products.take(5).joinToString(", ") { it.name },
                    listOf("product"),
                )
            resolvedProduct = product
            OrderItem(product).apply { quantity = resolvedQty }
        }

        val preview = buildDraft(customer, listOfNotNull(lineItem))
        applyTaxes(preview)
        val linePart = lineItem?.let { " with ${formatQty(it.quantity)} × ${it.product?.name ?: "item"}" } ?: ""
        val pendingParams = buildMap {
            put(CONFIRMED_PARAM, "true")
            put("customerId", picked.id)
            resolvedProduct?.let {
                put("productId", it.id)
                put("quantity", formatQty(resolvedQty))
            }
        }
        return ActionResult.Confirm(
            summary = "Create a draft order for ${customer.name}$linePart?",
            pendingAction = AgentAction(ActionType.CREATE, moduleName, pendingParams),
            amount = preview.totalCost,
        )
    }

    private suspend fun persistOrder(params: Map<String, String>): ActionResult {
        val customerId = params["customerId"]
            ?: return ActionResult.Error("Lost track of the customer — please try again.")
        val customer = orderRepository.customerDataService.getById(customerId)
            ?: return ActionResult.Error("Couldn't load the customer — please try again.")
        val lineItem: OrderItem? = params["productId"]?.let { productId ->
            val product = orderRepository.productDataService.getById(productId)
                ?: return ActionResult.Error("Couldn't load the product — please try again.")
            val qty = params["quantity"]?.trim()?.toDoubleOrNull()?.takeIf { it > 0.0 } ?: 1.0
            OrderItem(product).apply { quantity = qty }
        }
        val order = buildDraft(customer, listOfNotNull(lineItem))
        applyTaxes(order)
        return try {
            orderRepository.saveOrder(order)
            val linePart = lineItem?.let { " with ${formatQty(it.quantity)} × ${it.product?.name ?: "item"}" } ?: ""
            ActionResult.Success(
                summary = "Draft order created for ${customer.name}$linePart. Open it to review and finalize.",
                navigationTarget = NavigationTarget("OrderView", mapOf("orderId" to order.id)),
                amount = order.totalCost,
            )
        } catch (e: Exception) {
            ActionResult.Error("Couldn't create the order: ${e.message}")
        }
    }

    private suspend fun buildItemsFromCart(): List<OrderItem> = draft.lines.mapNotNull { line ->
        val product = orderRepository.productDataService.getById(line.productId) ?: return@mapNotNull null
        OrderItem(product).apply {
            quantity = line.quantity
            price = line.unitPrice
            priceOverridden = line.unitPrice != product.sellingPrice
        }
    }

    private suspend fun resolveCustomerId(params: Map<String, String>): String? {
        val name = (params["customer"] ?: params["customerName"] ?: params["name"])?.trim()
        if (name.isNullOrBlank()) return null
        val candidates = orderRepository.customerDataService.listCustomers(name)
        return candidates.firstOrNull { it.name.equals(name, ignoreCase = true) }?.id
            ?: candidates.singleOrNull()?.id
    }

    private fun buildDraft(customer: Customer, lineItems: List<OrderItem>): Order = Order().apply {
        this.customer = customer
        status = OrderStatus.DRAFT
        items = lineItems.toMutableList()
    }

    /** Mirror of the order editor's `computeTotals()` (no agent-side discounts). */
    private suspend fun applyTaxes(order: Order) {
        if (order.items.isEmpty()) return
        val scenario = ScenarioResolver.resolve(
            sellerStateCode = null,
            buyerStateCode = ScenarioResolver.stateCodeFromGstin(order.customer?.gstNumber),
        )
        val priceMode =
            if (storeSettings.getBoolean("common", "prices_include_tax", default = false)) {
                PriceMode.TAX_INCLUSIVE
            } else {
                PriceMode.TAX_EXCLUSIVE
            }
        val rates = taxRateProvider.resolveAll(order.items.mapNotNull { it.product?.taxCode }, scenario)
        val result = DocumentTotalsCalculator.calculate(
            DocumentCalcInput(
                lines = order.items.map { item ->
                    LineCalcInput(
                        id = item.id,
                        taxCode = item.product?.taxCode ?: "",
                        unitPrice = item.price,
                        quantity = item.quantity,
                    )
                },
                priceMode = priceMode,
                overallDiscount = null,
                overallDiscountMode = OverallDiscountMode.POST_TAX_REDUCTION,
                scenario = scenario,
                rates = rates,
            ),
        )
        val spec = if (scenario == TaxScenario.INTRA) TaxSpec.INTRA else TaxSpec.INTER
        val byId = result.lines.associateBy { it.id }
        order.items.forEach { item ->
            val line = byId[item.id] ?: return@forEach
            item.basePrice = line.taxable
            item.totalTax = line.totalTax
            item.totalCost = line.lineTotal
            item.taxInfos = line.components.map { c ->
                TaxInfo(name = c.name, percentage = c.percentage, taxSpec = spec, value = c.amount)
            }
        }
        order.taxSpec = spec
        order.basePrice = result.taxableSubtotal
        order.totalTax = result.totalTax
        order.taxInfos = result.taxComponents.map { c ->
            TaxInfo(name = c.name, percentage = 0.0, taxSpec = spec, value = c.amount)
        }.toMutableList()
        order.totalItems = order.items.size
        order.totalQuantity = order.items.sumOf { it.quantity }
        order.totalCost = result.grandTotal
    }

    private fun formatQty(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()

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
            navigationTarget = NavigationTarget("OrderView", mapOf("orderId" to order.id)),
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
                actionType = ActionType.CREATE,
                moduleName = "order",
                description = "Finalize a draft order. Uses the conversational cart (items added via cart.ADD_ITEM) when present; otherwise creates one with an optional single line item. Customer comes from the cart or the 'customer' param.",
                parameters = listOf(
                    ActionParameter("customer", ParameterType.STRING, required = false, "Customer name (optional if already set on the cart)"),
                    ActionParameter("product", ParameterType.STRING, required = false, "Product name for a single line item (when not using the cart)"),
                    ActionParameter("quantity", ParameterType.NUMBER, required = false, "Quantity for the single line item"),
                ),
            ),
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
