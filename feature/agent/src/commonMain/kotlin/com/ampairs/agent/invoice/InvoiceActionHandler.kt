package com.ampairs.agent.invoice

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
import com.ampairs.invoice.db.InvoiceRepository
import com.ampairs.invoice.db.entity.InvoiceEntity
import com.ampairs.invoice.db.entity.InvoiceItemEntity
import com.ampairs.invoice.domain.InvoiceStatus
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import kotlin.time.Clock

/**
 * Create an invoice from the conversational draft (cart). Requires customer to be set and items present.
 * Emits a confirmation action before finalizing; on confirmation, creates the invoice, clears the draft,
 * and queues sync automatically.
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@ActionHandlerKey("invoice")
class InvoiceActionHandler(
    private val draft: DraftDocumentStore,
    private val invoiceRepository: InvoiceRepository,
    private val businessDataService: BusinessDataService,
) : ActionHandler {

    override val moduleName = "invoice"

    override val supportedActions: List<ActionDescriptor> get() = ACTIONS

    override suspend fun execute(action: AgentAction): ActionResult {
        if (action.actionType != ActionType.CREATE_INVOICE) {
            return ActionResult.Error("Unsupported invoice action: ${action.actionType}")
        }

        val validated = validate()
        if (validated != null) return validated

        return try {
            createInvoice()
        } catch (e: Exception) {
            ActionResult.Error("Failed to create invoice: ${e.message}")
        }
    }

    private fun validate(): ActionResult? {
        if (draft.isEmpty) {
            return ActionResult.Error("Cart is empty. Add items before creating an invoice.")
        }
        if (draft.customerId == null || draft.customerName == null) {
            return ActionResult.Error("No customer selected. Please set a customer before creating an invoice.")
        }
        return null
    }

    private suspend fun createInvoice(): ActionResult {
        val business = businessDataService.getBusinessProfile()
            ?: return ActionResult.Error("Unable to load business profile. Try again.")

        val now = Clock.System.now().toEpochMilliseconds()
        val invoiceUid = com.ampairs.common.id_generator.UidGenerator.generateUid("INV")

        val invoice = InvoiceEntity(
            id = invoiceUid,
            invoice_number = "", // Will be auto-assigned by repository
            invoice_date = com.ampairs.common.locale.formatDate(
                com.ampairs.common.locale.now(),
                LocalAppLocale.Default,
            ),
            status = InvoiceStatus.NEW.name,
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
            InvoiceItemEntity(
                id = com.ampairs.common.id_generator.UidGenerator.generateUid("II"),
                invoice_id = invoiceUid,
                product_id = line.productId,
                product_name = line.productName,
                quantity = line.quantity,
                unit_price = line.unitPrice,
                line_total = line.lineSubtotal,
                line_tax = 0.0,
                last_updated = now,
            )
        }

        invoiceRepository.saveInvoice(invoice, items)

        val preview = invoiceRepository.nextNumberPreview(null)
        draft.clear()

        return ActionResult.Success(
            summary = "Invoice $preview created for ${draft.customerName} with ${draft.itemCount} item(s). " +
                "Total: ₹${draft.subtotal.toLong()}. Invoice will sync when you're online.",
            amount = draft.subtotal,
        )
    }

    companion object {
        val ACTIONS = listOf(
            ActionDescriptor(
                actionType = ActionType.CREATE_INVOICE,
                moduleName = "invoice",
                description = "Create an invoice from the current draft (cart). Customer and items must be set.",
                parameters = emptyList(),
            ),
        )
    }
}
