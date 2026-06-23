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
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.id_generator.UidGenerator
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import com.ampairs.invoice.db.InvoiceRepository
import com.ampairs.invoice.domain.INVOICE_PREFIX
import com.ampairs.invoice.domain.Invoice
import com.ampairs.invoice.domain.InvoiceItem
import com.ampairs.invoice.domain.InvoiceStatus

@Inject
@ContributesIntoMap(WorkspaceScope::class)
@ActionHandlerKey("invoice")
class InvoiceActionHandler(
    private val invoiceRepository: InvoiceRepository,
) : ActionHandler {

    override val moduleName = "invoice"

    override val supportedActions: List<ActionDescriptor> get() = ACTIONS

    override suspend fun execute(action: AgentAction): ActionResult = when (action.actionType) {
        ActionType.CREATE -> createInvoice(action.params)
        ActionType.SEARCH -> searchInvoices(action.params)
        ActionType.READ -> getInvoice(action.params)
        ActionType.COUNT -> countInvoices()
        ActionType.LIST -> listInvoices(action.params)
        else -> ActionResult.Error("Unsupported action: ${action.actionType}")
    }

    /**
     * Create a DRAFT invoice for a named customer, optionally with a single line item. Resolves the
     * customer and product against local data (offline-first), saves via [InvoiceRepository] (which
     * flags it PENDING_PUSH for sync), and returns a navigation target to open it. DRAFT status means
     * no receivable is posted until the user reviews and finalizes — the safe default for an
     * assistant-drafted bill. Multi-item commands arrive with the on-device LLM resolver (Phase 2).
     */
    private suspend fun createInvoice(params: Map<String, String>): ActionResult {
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
        val lineItem: InvoiceItem? = if (productName.isNullOrBlank()) {
            null
        } else {
            val qty = params["quantity"]?.trim()?.toDoubleOrNull()?.takeIf { it > 0.0 } ?: 1.0
            val products = invoiceRepository.productDataService.searchSummaries(productName, limit = 10)
            if (products.isEmpty()) return ActionResult.Error("No product matching \"$productName\".")
            val product = products.firstOrNull { it.name.equals(productName, ignoreCase = true) }
                ?: products.singleOrNull()
                ?: return ActionResult.NeedsInput(
                    "Which product did you mean? " + products.take(5).joinToString(", ") { it.name },
                    listOf("product"),
                )
            InvoiceItem(product).apply { quantity = qty }
        }

        val invoice = Invoice().apply {
            id = UidGenerator.generateUid(INVOICE_PREFIX)
            this.customer = customer
            status = InvoiceStatus.DRAFT
            items = mutableListOf<InvoiceItem>().apply { lineItem?.let { add(it) } }
        }

        return try {
            invoiceRepository.saveInvoice(invoice)
            val linePart = lineItem?.let { " with ${formatQty(it.quantity)} × ${it.product?.name ?: "item"}" } ?: ""
            ActionResult.Success(
                summary = "Draft invoice created for ${customer.name}$linePart (total ${formatAmount(invoice.totalCost)}). Open it to review and finalize.",
                navigationTarget = NavigationTarget(
                    routeDescription = "InvoiceView",
                    routeData = mapOf("invoiceId" to invoice.id),
                ),
            )
        } catch (e: Exception) {
            ActionResult.Error("Couldn't create the invoice: ${e.message}")
        }
    }

    // Plain (currency-symbol-free) formatting for the chat summary — the symbol/grouping belongs to
    // the UI layer (LocalAppLocale), which a non-composable handler can't read.
    private fun formatAmount(value: Double): String {
        val rounded = kotlin.math.round(value * 100.0) / 100.0
        return if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString()
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
        val ACTIONS = listOf(
            ActionDescriptor(
                actionType = ActionType.CREATE,
                moduleName = "invoice",
                description = "Create a draft invoice/bill for a customer, optionally with one line item",
                parameters = listOf(
                    ActionParameter("customer", ParameterType.STRING, required = true, "Customer name to bill"),
                    ActionParameter("product", ParameterType.STRING, required = false, "Product name for a line item"),
                    ActionParameter("quantity", ParameterType.NUMBER, required = false, "Quantity for the line item"),
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
        )
    }
}
