package com.ampairs.printing.preview

import com.ampairs.printing.core.model.DocumentType
import com.ampairs.printing.core.model.EntityRef
import com.ampairs.printing.core.model.FieldValue
import com.ampairs.printing.core.provider.LineValues
import com.ampairs.printing.core.provider.PrintValueProvider

/**
 * Canned, domain-free sample data so the template editor can render a *live preview through the real
 * [com.ampairs.printing.core.engine.PrintEngine] + renderer* — the only way the preview is guaranteed
 * to match the actual printout. Returns a superset of the field keys the built-in templates bind
 * (invoice + order + receipt), so any default or edited template resolves to something visible.
 */
class SampleValueProvider(
    override val documentType: DocumentType = DocumentType.INVOICE,
) : PrintValueProvider {

    override suspend fun standardValues(documentId: String): Map<String, FieldValue> = mapOf(
        "seller_name" to FieldValue.Text("Acme Traders"),
        "invoice_number" to FieldValue.Text("INV-1024"),
        "order_number" to FieldValue.Text("ORD-512"),
        "invoice_date" to FieldValue.Text("20 Jun 2026"),
        "order_date" to FieldValue.Text("20 Jun 2026"),
        "base_price" to FieldValue.Money(1000.0),
        "total_tax" to FieldValue.Money(180.0),
        "total_cost" to FieldValue.Money(1180.0),
    )

    override suspend fun customValues(documentId: String): Map<String, String> = emptyMap()

    override suspend fun nestedValues(documentId: String, ref: EntityRef): Map<String, FieldValue> =
        if (ref == EntityRef.CUSTOMER) mapOf("customer_name" to FieldValue.Text("Rajesh Kumar")) else emptyMap()

    override suspend fun lines(documentId: String): List<LineValues> = listOf(
        sampleLine("Widget A", 2.0, 250.0, 500.0),
        sampleLine("Widget B", 1.0, 400.0, 400.0),
        sampleLine("Service fee", 1.0, 280.0, 280.0),
    )

    private fun sampleLine(desc: String, qty: Double, price: Double, total: Double) = LineValues(
        standard = mapOf(
            "description" to FieldValue.Text(desc),
            "quantity" to FieldValue.Number(qty, 0),
            "price" to FieldValue.Money(price),
            "total_cost" to FieldValue.Money(total),
        ),
    )
}
