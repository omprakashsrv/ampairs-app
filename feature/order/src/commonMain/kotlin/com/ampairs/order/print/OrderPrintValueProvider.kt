package com.ampairs.order.print

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.order.db.OrderRepository
import com.ampairs.printing.core.model.DocumentType
import com.ampairs.printing.core.model.EntityRef
import com.ampairs.printing.core.model.FieldValue
import com.ampairs.printing.core.provider.LineValues
import com.ampairs.printing.core.provider.PrintValueProvider
import com.ampairs.printing.di.DocumentTypeKey
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import kotlin.time.ExperimentalTime

/**
 * Maps an [com.ampairs.order.domain.Order] to printable field values (§7) — the only code that knows
 * the order shape; the print engine consumes it generically. Money/date stay typed so the renderer
 * applies the workspace locale (totals are never recomputed). Contributed to the provider registry so
 * [com.ampairs.printing.service.PrintCoordinator] can rebuild it for retries.
 */
@OptIn(ExperimentalTime::class)
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@DocumentTypeKey(DocumentType.ORDER)
class OrderPrintValueProvider(
    private val orderRepository: OrderRepository,
) : PrintValueProvider {

    override val documentType: DocumentType = DocumentType.ORDER

    override suspend fun standardValues(documentId: String): Map<String, FieldValue> {
        val order = orderRepository.getOrder(documentId)
        return buildMap {
            put("order_number", FieldValue.Text(order.orderNumber ?: ""))
            put("order_date", FieldValue.DateValue(order.orderDate.toEpochMilliseconds()))
            put("seller_name", FieldValue.Text(order.sellerName ?: ""))
            put("seller_address", FieldValue.Text(order.sellerAddress ?: ""))
            put("seller_gst", FieldValue.Text(order.sellerGst ?: ""))
            put("base_price", FieldValue.Money(order.basePrice))
            put("total_tax", FieldValue.Money(order.totalTax))
            put("total_cost", FieldValue.Money(order.totalCost))
        }
    }

    override suspend fun customValues(documentId: String): Map<String, String> = emptyMap()

    override suspend fun nestedValues(documentId: String, ref: EntityRef): Map<String, FieldValue> {
        if (ref != EntityRef.CUSTOMER) return emptyMap()
        val customer = orderRepository.getOrder(documentId).customer ?: return emptyMap()
        return mapOf("customer_name" to FieldValue.Text(customer.name))
    }

    override suspend fun lines(documentId: String): List<LineValues> {
        val order = orderRepository.getOrder(documentId)
        return order.items.map { item ->
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
