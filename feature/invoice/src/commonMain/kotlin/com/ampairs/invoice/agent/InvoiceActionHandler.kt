package com.ampairs.invoice.agent

import com.ampairs.agent.core.ActionDescriptor
import com.ampairs.agent.core.ActionHandler
import com.ampairs.agent.core.ActionParameter
import com.ampairs.agent.core.ActionResult
import com.ampairs.agent.core.ActionType
import com.ampairs.agent.core.AgentAction
import com.ampairs.agent.core.NavigationTarget
import com.ampairs.agent.core.ParameterType
import com.ampairs.invoice.db.InvoiceRepository

class InvoiceActionHandler(
    private val invoiceRepository: InvoiceRepository,
) : ActionHandler {

    override val moduleName = "invoice"

    override val supportedActions: List<ActionDescriptor> get() = ACTIONS

    override suspend fun execute(action: AgentAction): ActionResult = when (action.actionType) {
        ActionType.SEARCH -> searchInvoices(action.params)
        ActionType.READ -> getInvoice(action.params)
        ActionType.COUNT -> countInvoices()
        ActionType.LIST -> listInvoices(action.params)
        else -> ActionResult.Error("Unsupported action: ${action.actionType}")
    }

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
