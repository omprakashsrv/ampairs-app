package com.ampairs.invoice.print

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.invoice.db.InvoiceRepository
import com.ampairs.printing.core.model.DocumentType
import com.ampairs.printing.core.model.EntityRef
import com.ampairs.printing.core.model.FieldValue
import com.ampairs.printing.core.provider.LineValues
import com.ampairs.printing.core.provider.PrintValueProvider
import com.ampairs.printing.di.DocumentTypeKey
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

/**
 * Maps an [com.ampairs.invoice.domain.Invoice] to printable field values (§7). The only code that
 * knows the invoice shape; the printing engine consumes this generically. Money/date stay typed —
 * the renderer applies the workspace locale (totals are never recomputed). Contributed to the
 * provider registry so [com.ampairs.printing.service.PrintCoordinator] can rebuild it for retries.
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@DocumentTypeKey(DocumentType.INVOICE)
class InvoicePrintValueProvider(
    private val invoiceRepository: InvoiceRepository,
) : PrintValueProvider {

    override val documentType: DocumentType = DocumentType.INVOICE

    override suspend fun standardValues(documentId: String): Map<String, FieldValue> {
        val invoice = invoiceRepository.getInvoice(documentId)
        return buildMap {
            put("invoice_number", FieldValue.Text(invoice.invoiceNumber ?: ""))
            put("invoice_date", FieldValue.DateValue(invoice.invoiceDate.toEpochMilliseconds()))
            put("seller_name", FieldValue.Text(invoice.sellerName ?: ""))
            put("seller_address", FieldValue.Text(invoice.sellerAddress ?: ""))
            put("seller_gst", FieldValue.Text(invoice.sellerGst ?: ""))
            put("base_price", FieldValue.Money(invoice.basePrice))
            put("total_tax", FieldValue.Money(invoice.totalTax))
            put("total_cost", FieldValue.Money(invoice.totalCost))
        }
    }

    override suspend fun customValues(documentId: String): Map<String, String> = emptyMap()

    override suspend fun nestedValues(documentId: String, ref: EntityRef): Map<String, FieldValue> {
        if (ref != EntityRef.CUSTOMER) return emptyMap()
        val invoice = invoiceRepository.getInvoice(documentId)
        val customer = invoice.customer ?: return emptyMap()
        return mapOf(
            "customer_name" to FieldValue.Text(customer.name),
        )
    }

    override suspend fun lines(documentId: String): List<LineValues> {
        val invoice = invoiceRepository.getInvoice(documentId)
        return invoice.items.map { item ->
            LineValues(
                standard = mapOf(
                    "description" to FieldValue.Text(item.description),
                    "quantity" to FieldValue.Number(item.quantity),
                    "price" to FieldValue.Money(item.price),
                    "base_price" to FieldValue.Money(item.basePrice),
                    "total_tax" to FieldValue.Money(item.totalTax),
                    "total_cost" to FieldValue.Money(item.totalCost),
                ),
            )
        }
    }
}
