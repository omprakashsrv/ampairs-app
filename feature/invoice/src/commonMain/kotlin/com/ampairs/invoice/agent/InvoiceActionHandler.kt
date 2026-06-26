package com.ampairs.invoice.agent

import com.ampairs.common.agent.ActionDescriptor
import com.ampairs.common.agent.ActionHandler
import com.ampairs.common.agent.ActionParameter
import com.ampairs.common.agent.ActionResult
import com.ampairs.common.agent.ActionType
import com.ampairs.common.agent.AgentAction
import com.ampairs.common.agent.NavigationTarget
import com.ampairs.common.agent.ParameterType
import com.ampairs.common.agent.ActionHandlerKey
import com.ampairs.common.agent.CONFIRMED_PARAM
import com.ampairs.common.agent.DateRange
import com.ampairs.common.agent.DraftDocumentStore
import com.ampairs.common.agent.ReportPeriod
import com.ampairs.common.agent.ReportRow
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.id_generator.UidGenerator
import com.ampairs.common.model.DateTimeAdapter
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import com.ampairs.customer.domain.Customer
import com.ampairs.invoice.db.InvoiceRepository
import com.ampairs.invoice.domain.INVOICE_PREFIX
import com.ampairs.invoice.domain.Invoice
import com.ampairs.invoice.domain.InvoiceItem
import com.ampairs.invoice.domain.InvoiceStatus
import com.ampairs.invoice.domain.TaxInfo
import com.ampairs.invoice.domain.TaxSpec
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

@Inject
@ContributesIntoMap(WorkspaceScope::class)
@ActionHandlerKey("invoice")
class InvoiceActionHandler(
    private val invoiceRepository: InvoiceRepository,
    private val agentDao: InvoiceAgentDao,
    private val taxRateProvider: TaxRateProvider,
    private val storeSettings: StoreSettingsProvider,
    private val draft: DraftDocumentStore,
) : ActionHandler {

    override val moduleName = "invoice"

    override val supportedActions: List<ActionDescriptor> get() = ACTIONS

    override suspend fun execute(action: AgentAction): ActionResult = when (action.actionType) {
        ActionType.CREATE -> createInvoice(action.params)
        ActionType.SEARCH -> searchInvoices(action.params)
        ActionType.READ -> getInvoice(action.params)
        ActionType.COUNT -> countInvoices()
        ActionType.LIST -> listInvoices(action.params)
        ActionType.TOTAL_SALES -> reportTotalSales(action.params)
        ActionType.AVERAGE_INVOICE -> reportAverageInvoice(action.params)
        ActionType.TOP_CUSTOMERS -> reportTopCustomers(action.params)
        ActionType.TOP_PRODUCTS -> reportTopProducts(action.params)
        else -> ActionResult.Error("Unsupported action: ${action.actionType}")
    }

    // ── Curated reports (deterministic; preferred over free SQL) ─────────────────────────────────

    private suspend fun reportTotalSales(params: Map<String, String>): ActionResult {
        val period = resolvePeriod(params)
        val total = if (period.range == null) {
            agentDao.sumSales()
        } else {
            val (start, end) = bounds(period.range)
            agentDao.sumSalesBetween(start, end)
        }
        return ActionResult.Success(summary = "Total sales${period.label}.", amount = total ?: 0.0)
    }

    private suspend fun reportAverageInvoice(params: Map<String, String>): ActionResult {
        val period = resolvePeriod(params)
        val avg = if (period.range == null) {
            agentDao.averageInvoiceValue()
        } else {
            val (start, end) = bounds(period.range)
            agentDao.averageInvoiceValueBetween(start, end)
        }
        return if (avg == null) {
            ActionResult.Success("No invoices yet${period.label}, so there's no average to show.")
        } else {
            ActionResult.Success(summary = "Average invoice value${period.label}.", amount = avg)
        }
    }

    private suspend fun reportTopCustomers(params: Map<String, String>): ActionResult {
        val period = resolvePeriod(params)
        val limit = resolveLimit(params)
        val rows = if (period.range == null) {
            agentDao.topCustomers(limit)
        } else {
            val (start, end) = bounds(period.range)
            agentDao.topCustomersBetween(start, end, limit)
        }.map { ReportRow(label = it.label?.ifBlank { "Unnamed customer" } ?: "Unnamed customer", amount = it.total) }
        return if (rows.isEmpty()) {
            ActionResult.Success("No sales recorded${period.label}.")
        } else {
            ActionResult.Success(summary = "Top ${rows.size} customers by sales${period.label}:", rows = rows)
        }
    }

    private suspend fun reportTopProducts(params: Map<String, String>): ActionResult {
        val period = resolvePeriod(params)
        val limit = resolveLimit(params)
        val rows = if (period.range == null) {
            agentDao.topProducts(limit)
        } else {
            val (start, end) = bounds(period.range)
            agentDao.topProductsBetween(start, end, limit)
        }.map { ReportRow(label = it.label?.ifBlank { "Unnamed item" } ?: "Unnamed item", amount = it.total) }
        return if (rows.isEmpty()) {
            ActionResult.Success("No product sales recorded${period.label}.")
        } else {
            ActionResult.Success(summary = "Top ${rows.size} products by sales${period.label}:", rows = rows)
        }
    }

    /** Resolved period: a half-open range (null = all time) plus a human label like " (this month)". */
    private data class ResolvedPeriod(val range: DateRange?, val label: String)

    private fun resolvePeriod(params: Map<String, String>): ResolvedPeriod {
        val raw = (params["period"] ?: params["range"] ?: params["duration"])?.trim()
        val range = ReportPeriod.current(raw)
        val label = if (range != null && !raw.isNullOrBlank()) {
            " (" + raw.lowercase().replace('_', ' ').replace('-', ' ') + ")"
        } else {
            ""
        }
        return ResolvedPeriod(range, label)
    }

    private fun resolveLimit(params: Map<String, String>): Int =
        params["limit"]?.trim()?.toIntOrNull()?.coerceIn(1, 20) ?: 5

    /**
     * Half-open period bounds as device-local "yyyy-MM-dd HH:mm:ss" strings — matches the stored
     * `invoice_date` format so lexical comparison (`>= start AND < end`) is correct.
     */
    private fun bounds(range: DateRange): Pair<String, String> =
        DateTimeAdapter.toDateTimeString(range.startInclusive) to DateTimeAdapter.toDateTimeString(range.endExclusive)

    /**
     * Create a DRAFT invoice for a named customer, optionally with a single line item — in two phases
     * so a money action never persists from a single utterance (FR-006):
     *  - **propose** (no `CONFIRMED_PARAM`): resolve customer/product against local data, compute the
     *    total, and return [ActionResult.Confirm] with the resolved ids in `pendingAction`. Nothing
     *    is written.
     *  - **persist** (`CONFIRMED_PARAM == "true"`): rebuild from those ids and save via
     *    [InvoiceRepository] (offline-first, flagged PENDING_PUSH), returning a nav target.
     * DRAFT status means no receivable is posted until the user finalizes. Multi-item commands arrive
     * with the on-device LLM resolver (Phase 2).
     */
    private suspend fun createInvoice(params: Map<String, String>): ActionResult {
        val confirmed = params[CONFIRMED_PARAM] == "true"
        // Cart mode: the conversational draft (DraftDocumentStore) holds the items — finalize from it.
        val cartMode = params["fromCart"] == "true" || (!confirmed && !draft.isEmpty)
        return when {
            cartMode && confirmed -> persistFromCart(params)
            cartMode -> proposeFromCart(params)
            confirmed -> persistInvoice(params)
            else -> proposeInvoice(params)
        }
    }

    /** Propose an invoice built from the multi-item conversational cart (one confirmation, full total). */
    private suspend fun proposeFromCart(params: Map<String, String>): ActionResult {
        if (draft.isEmpty) {
            return ActionResult.NeedsInput("Your cart is empty. Add items like \"add 2 widgets\" first.", listOf("product"))
        }
        val customerId = draft.customerId ?: resolveCustomerId(params)
            ?: return ActionResult.NeedsInput("Who is the bill for? Say \"customer NAME\".", listOf("customer"))
        val customer = invoiceRepository.customerDataService.getById(customerId)
            ?: return ActionResult.Error("Couldn't load the customer — please try again.")
        val items = buildItemsFromCart()
        if (items.isEmpty()) return ActionResult.Error("Couldn't resolve the cart items — please try again.")

        val previewDraft = buildDraft(customer, items)
        applyTaxes(previewDraft)
        return ActionResult.Confirm(
            summary = "Create a draft invoice for ${customer.name} with ${items.size} item(s) (${formatQty(draft.totalQuantity)} unit(s))?",
            pendingAction = AgentAction(
                ActionType.CREATE,
                moduleName,
                mapOf(CONFIRMED_PARAM to "true", "fromCart" to "true", "customerId" to customerId),
            ),
            amount = previewDraft.totalCost,
        )
    }

    private suspend fun persistFromCart(params: Map<String, String>): ActionResult {
        if (draft.isEmpty) return ActionResult.Error("The cart is empty — nothing to create.")
        val customerId = params["customerId"] ?: draft.customerId
            ?: return ActionResult.Error("Lost track of the customer — please try again.")
        val customer = invoiceRepository.customerDataService.getById(customerId)
            ?: return ActionResult.Error("Couldn't load the customer — please try again.")
        val items = buildItemsFromCart()
        if (items.isEmpty()) return ActionResult.Error("Couldn't resolve the cart items — please try again.")

        val invoice = buildDraft(customer, items)
        applyTaxes(invoice)
        return try {
            invoiceRepository.saveInvoice(invoice)
            draft.clear()
            ActionResult.Success(
                summary = "Draft invoice created for ${customer.name} with ${items.size} item(s). Open it to review and finalize.",
                navigationTarget = NavigationTarget(
                    routeDescription = "InvoiceView",
                    routeData = mapOf("invoiceId" to invoice.id),
                ),
                amount = invoice.totalCost,
            )
        } catch (e: Exception) {
            ActionResult.Error("Couldn't create the invoice: ${e.message}")
        }
    }

    /** Resolve each cart line to a current product and build invoice items (honoring price overrides). */
    private suspend fun buildItemsFromCart(): List<InvoiceItem> = draft.lines.mapNotNull { line ->
        val product = invoiceRepository.productDataService.getById(line.productId) ?: return@mapNotNull null
        InvoiceItem(product).apply {
            quantity = line.quantity
            price = line.unitPrice
            priceOverridden = line.unitPrice != product.sellingPrice
        }
    }

    /** Resolve a customer id from a free-text name param (single/exact match), or null when ambiguous. */
    private suspend fun resolveCustomerId(params: Map<String, String>): String? {
        val name = (params["customer"] ?: params["customerName"] ?: params["name"])?.trim()
        if (name.isNullOrBlank()) return null
        val candidates = invoiceRepository.customerDataService.listCustomers(name)
        return candidates.firstOrNull { it.name.equals(name, ignoreCase = true) }?.id
            ?: candidates.singleOrNull()?.id
    }

    private suspend fun proposeInvoice(params: Map<String, String>): ActionResult {
        val customerName = (params["customer"] ?: params["customerName"] ?: params["name"])?.trim()
        if (customerName.isNullOrBlank()) {
            return ActionResult.NeedsInput("Who is the bill for?", listOf("customer"))
        }

        val candidates = invoiceRepository.customerDataService.listCustomers(customerName)
        if (candidates.isEmpty()) {
            return ActionResult.Error("No customer matching \"$customerName\". Add the customer first, then try again.")
        }
        val pickedCustomer = candidates.firstOrNull { it.name.equals(customerName, ignoreCase = true) }
            ?: candidates.singleOrNull()
            ?: return ActionResult.NeedsInput(
                "Which customer did you mean? " + candidates.take(5).joinToString(", ") { it.name },
                listOf("customer"),
            )
        val customer = invoiceRepository.customerDataService.getById(pickedCustomer.id)
            ?: return ActionResult.Error("Couldn't load customer \"${pickedCustomer.name}\".")

        // Optional single line item: "... 2 widget". Resolve the product locally if one was given.
        val productName = (params["product"] ?: params["item"])?.trim()
        var resolvedProduct: ProductSummary? = null
        var resolvedQty = 1.0
        val lineItem: InvoiceItem? = if (productName.isNullOrBlank()) {
            null
        } else {
            resolvedQty = params["quantity"]?.trim()?.toDoubleOrNull()?.takeIf { it > 0.0 } ?: 1.0
            val products = invoiceRepository.productDataService.searchSummaries(productName, limit = 10)
            if (products.isEmpty()) return ActionResult.Error("No product matching \"$productName\".")
            val product = products.firstOrNull { it.name.equals(productName, ignoreCase = true) }
                ?: products.singleOrNull()
                ?: return ActionResult.NeedsInput(
                    "Which product did you mean? " + products.take(5).joinToString(", ") { it.name },
                    listOf("product"),
                )
            resolvedProduct = product
            InvoiceItem(product).apply { quantity = resolvedQty }
        }

        // Build the draft in memory only (to compute the total) — do NOT persist before confirmation.
        val previewDraft = buildDraft(customer, listOfNotNull(lineItem))
        applyTaxes(previewDraft)
        val linePart = lineItem?.let { " with ${formatQty(it.quantity)} × ${it.product?.name ?: "item"}" } ?: ""
        val pendingParams = buildMap {
            put(CONFIRMED_PARAM, "true")
            put("customerId", pickedCustomer.id)
            resolvedProduct?.let {
                put("productId", it.id)
                put("quantity", formatQty(resolvedQty))
            }
        }
        return ActionResult.Confirm(
            summary = "Create a draft invoice for ${customer.name}$linePart?",
            pendingAction = AgentAction(ActionType.CREATE, moduleName, pendingParams),
            amount = previewDraft.totalCost, // UI renders the total via formatMoney(LocalAppLocale.current)
        )
    }

    private suspend fun persistInvoice(params: Map<String, String>): ActionResult {
        val customerId = params["customerId"]
            ?: return ActionResult.Error("Lost track of the customer — please try again.")
        val customer = invoiceRepository.customerDataService.getById(customerId)
            ?: return ActionResult.Error("Couldn't load the customer — please try again.")

        val lineItem: InvoiceItem? = params["productId"]?.let { productId ->
            val product = invoiceRepository.productDataService.getById(productId)
                ?: return ActionResult.Error("Couldn't load the product — please try again.")
            val qty = params["quantity"]?.trim()?.toDoubleOrNull()?.takeIf { it > 0.0 } ?: 1.0
            InvoiceItem(product).apply { quantity = qty }
        }

        val invoice = buildDraft(customer, listOfNotNull(lineItem))
        applyTaxes(invoice)
        return try {
            invoiceRepository.saveInvoice(invoice)
            val linePart = lineItem?.let { " with ${formatQty(it.quantity)} × ${it.product?.name ?: "item"}" } ?: ""
            ActionResult.Success(
                summary = "Draft invoice created for ${customer.name}$linePart. Open it to review and finalize.",
                navigationTarget = NavigationTarget(
                    routeDescription = "InvoiceView",
                    routeData = mapOf("invoiceId" to invoice.id),
                ),
                amount = invoice.totalCost, // UI renders the total via formatMoney(LocalAppLocale.current)
            )
        } catch (e: Exception) {
            ActionResult.Error("Couldn't create the invoice: ${e.message}")
        }
    }

    private fun buildDraft(customer: Customer, lineItems: List<InvoiceItem>): Invoice = Invoice().apply {
        id = UidGenerator.generateUid(INVOICE_PREFIX)
        this.customer = customer
        status = InvoiceStatus.DRAFT
        items = lineItems.toMutableList()
    }

    /**
     * Apply GST to the draft via the tax module's pure calculator — mirrors
     * `InvoiceViewModel.computeTotals()` so the assistant's total matches what the editor shows on
     * open (T043, FR-005). Scenario is driven by the buyer's GSTIN (seller-origin state is owned by
     * the tax module and still unset, exactly like the editor's `sellerStateCode = null`); price mode
     * comes from the workspace `prices_include_tax` setting. No discounts in the assistant flow yet.
     */
    private suspend fun applyTaxes(invoice: Invoice) {
        if (invoice.items.isEmpty()) return
        val scenario = ScenarioResolver.resolve(
            sellerStateCode = null,
            buyerStateCode = ScenarioResolver.stateCodeFromGstin(invoice.customer?.gstNumber),
        )
        val priceMode =
            if (storeSettings.getBoolean("common", "prices_include_tax", default = false)) {
                PriceMode.TAX_INCLUSIVE
            } else {
                PriceMode.TAX_EXCLUSIVE
            }
        val rates = taxRateProvider.resolveAll(invoice.items.mapNotNull { it.product?.taxCode }, scenario)
        val result = DocumentTotalsCalculator.calculate(
            DocumentCalcInput(
                lines = invoice.items.map { item ->
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
        invoice.items.forEach { item ->
            val line = byId[item.id] ?: return@forEach
            item.basePrice = line.taxable
            item.totalTax = line.totalTax
            item.totalCost = line.lineTotal
            item.taxInfos = line.components.map { c ->
                TaxInfo(name = c.name, percentage = c.percentage, taxSpec = spec, value = c.amount)
            }
        }
        invoice.taxSpec = spec
        invoice.basePrice = result.taxableSubtotal
        invoice.totalTax = result.totalTax
        invoice.taxInfos = result.taxComponents.map { c ->
            TaxInfo(name = c.name, percentage = 0.0, taxSpec = spec, value = c.amount)
        }.toMutableList()
        invoice.totalItems = invoice.items.size
        invoice.totalQuantity = invoice.items.sumOf { it.quantity }
        invoice.totalCost = result.grandTotal
    }

    private fun formatQty(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()

    private suspend fun searchInvoices(params: Map<String, String>): ActionResult {
        val query = params["query"]
            ?: return ActionResult.NeedsInput("What do you want to search for?", listOf("query"))
        val invoices = invoiceRepository.invoiceDao.getInvoicesByNumber(query) +
                invoiceRepository.invoiceDao.getInvoicesByCustomerName(query)
        val unique = invoices.distinctBy { it.id }
        return if (unique.isEmpty()) {
            ActionResult.Success("No invoices found matching '$query'.")
        } else {
            val summary = unique.take(5).joinToString("\n") { "• #${it.invoice_number} — ${it.status}" }
            ActionResult.Success("Found ${unique.size} invoice(s):\n$summary")
        }
    }

    private suspend fun getInvoice(params: Map<String, String>): ActionResult {
        val id = params["invoiceId"] ?: params["searchName"]
            ?: return ActionResult.NeedsInput("Which invoice? Provide an invoice number or ID.", listOf("invoiceId"))
        val invoice = invoiceRepository.invoiceDao.selectById(id)
            ?: invoiceRepository.invoiceDao.getInvoicesByNumber(id).firstOrNull()
            ?: return ActionResult.Error("Invoice '$id' not found.")
        return ActionResult.Success(
            summary = "Invoice #${invoice.invoice_number}: status=${invoice.status}",
            navigationTarget = NavigationTarget(
                routeDescription = "InvoiceView",
                routeData = mapOf("invoiceId" to invoice.id),
            ),
        )
    }

    private suspend fun countInvoices(): ActionResult {
        val count = invoiceRepository.invoiceDao.countInvoices()
        return ActionResult.Success("You have $count invoice(s).")
    }

    private suspend fun listInvoices(params: Map<String, String>): ActionResult {
        val status = params["status"]
        val invoices = if (status != null) {
            invoiceRepository.invoiceDao.getInvoicesByStatus(status)
        } else {
            invoiceRepository.invoiceDao.selectAll()
        }
        return if (invoices.isEmpty()) {
            ActionResult.Success("No invoices found.")
        } else {
            val summary = invoices.take(5).joinToString("\n") { "• #${it.invoice_number} — ${it.status}" }
            val more = if (invoices.size > 5) "\n… and ${invoices.size - 5} more" else ""
            ActionResult.Success("${invoices.size} invoice(s):\n$summary$more")
        }
    }

    companion object {
        /** Shared report params: an optional time window and an optional result cap. */
        private val PERIOD_PARAM = ActionParameter(
            "period",
            ParameterType.STRING,
            required = false,
            "Time window: today, yesterday, this_week, last_week, this_month, last_month, this_year, last_year. Omit for all time.",
        )
        private val LIMIT_PARAM = ActionParameter(
            "limit",
            ParameterType.NUMBER,
            required = false,
            "How many to return (default 5, max 20).",
        )

        val ACTIONS = listOf(
            ActionDescriptor(
                actionType = ActionType.CREATE,
                moduleName = "invoice",
                description = "Finalize a draft invoice/bill. Uses the conversational cart (items added via cart.ADD_ITEM) when present; otherwise creates one with an optional single line item. Customer comes from the cart or the 'customer' param.",
                parameters = listOf(
                    ActionParameter("customer", ParameterType.STRING, required = false, "Customer name to bill (optional if already set on the cart)"),
                    ActionParameter("product", ParameterType.STRING, required = false, "Product name for a single line item (when not using the cart)"),
                    ActionParameter("quantity", ParameterType.NUMBER, required = false, "Quantity for the single line item"),
                ),
            ),
            ActionDescriptor(
                actionType = ActionType.SEARCH,
                moduleName = "invoice",
                description = "Search invoices by number or customer name",
                parameters = listOf(
                    ActionParameter("query", ParameterType.STRING, required = true, "Search query"),
                ),
            ),
            ActionDescriptor(
                actionType = ActionType.READ,
                moduleName = "invoice",
                description = "Get invoice details by ID or invoice number",
                parameters = listOf(
                    ActionParameter("invoiceId", ParameterType.STRING, required = false, "Invoice ID"),
                    ActionParameter("searchName", ParameterType.STRING, required = false, "Invoice number to search"),
                ),
            ),
            ActionDescriptor(
                actionType = ActionType.COUNT,
                moduleName = "invoice",
                description = "Get total invoice count",
                parameters = emptyList(),
            ),
            ActionDescriptor(
                actionType = ActionType.LIST,
                moduleName = "invoice",
                description = "List invoices, optionally filtered by status",
                parameters = listOf(
                    ActionParameter("status", ParameterType.STRING, required = false, "Invoice status filter"),
                ),
            ),
            ActionDescriptor(
                actionType = ActionType.TOTAL_SALES,
                moduleName = "invoice",
                description = "Total sales / revenue (sum of invoice totals). Use for \"how much did we sell\", \"total sales\", \"revenue this month\".",
                parameters = listOf(PERIOD_PARAM),
            ),
            ActionDescriptor(
                actionType = ActionType.AVERAGE_INVOICE,
                moduleName = "invoice",
                description = "Average invoice/bill value. Use for \"average invoice\", \"average sale value\".",
                parameters = listOf(PERIOD_PARAM),
            ),
            ActionDescriptor(
                actionType = ActionType.TOP_CUSTOMERS,
                moduleName = "invoice",
                description = "Highest-revenue customers ranked by total sales. Use for \"top customers\", \"best customers\", \"who buys the most\".",
                parameters = listOf(PERIOD_PARAM, LIMIT_PARAM),
            ),
            ActionDescriptor(
                actionType = ActionType.TOP_PRODUCTS,
                moduleName = "invoice",
                description = "Best-selling products ranked by sales value. Use for \"top products\", \"best sellers\", \"what sells most\".",
                parameters = listOf(PERIOD_PARAM, LIMIT_PARAM),
            ),
        )
    }
}
