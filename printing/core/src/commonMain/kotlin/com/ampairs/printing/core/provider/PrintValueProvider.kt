package com.ampairs.printing.core.provider

import com.ampairs.printing.core.model.DocumentType
import com.ampairs.printing.core.model.EntityRef
import com.ampairs.printing.core.model.FieldValue

/**
 * Supplies the data for one [DocumentType], resolving a document id into field values. Implemented
 * inside each owning feature (invoice/order/...) and contributed via Metro, so `printing/core` never
 * imports document features. Mirrors feature/form's standard-value-map + attributes-map bridge (§7).
 */
interface PrintValueProvider {
    val documentType: DocumentType

    /** Header-level standard fields, keyed by FormField.fieldKey (e.g. "grand_total" → Money). */
    suspend fun standardValues(documentId: String): Map<String, FieldValue>

    /** Header-level custom fields = entity.attributes, keyed by attribute key. */
    suspend fun customValues(documentId: String): Map<String, String>

    /** Nested header entities (the document's customer), resolved against their own schema. */
    suspend fun nestedValues(documentId: String, ref: EntityRef): Map<String, FieldValue> = emptyMap()

    /** Repeating line items for LINE-scope bindings (table rows). */
    suspend fun lines(documentId: String): List<LineValues> = emptyList()
}

/** One line item's resolvable values. */
data class LineValues(
    val standard: Map<String, FieldValue> = emptyMap(),
    val custom: Map<String, String> = emptyMap(),
    /** Nested per-line entities (the line's product), keyed by ref. */
    val nested: Map<EntityRef, Map<String, FieldValue>> = emptyMap(),
)

/**
 * Print-only fields not present in the form schema (amount-in-words, tax summary, page numbers, and
 * the dynamic `upi_qr` scan-to-pay payload). Implemented in the feature/render layer.
 */
interface ComputedFieldCatalog {
    fun keys(documentType: DocumentType): List<String>
    suspend fun compute(documentType: DocumentType, documentId: String, fieldKey: String): FieldValue
}
