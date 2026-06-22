package com.ampairs.printing.core.model

import kotlinx.serialization.Serializable

/** The business document a template renders. New types are added here + a feature [PrintValueProvider]. */
@Serializable
enum class DocumentType(val key: String) {
    INVOICE("invoice"),
    ORDER("order"),
    RECEIPT("receipt"),
    CREDIT_NOTE("credit_note"),
    DEBIT_NOTE("debit_note"),
    QUOTATION("quotation"),
    DELIVERY_CHALLAN("delivery_challan"),
    PAYMENT_RECEIPT("payment_receipt"),
    ACCOUNT_STATEMENT("account_statement"),
    DAY_END_REPORT("day_end_report"),
    GIFT_RECEIPT("gift_receipt"),
    LABEL("label"),
}

/** The physical printer family — selects the renderer and the layout dialect. */
@Serializable
enum class PrinterClass { THERMAL, PAGE, LABEL }

/** How bytes/markup reach the printer. NETWORK/BLUETOOTH/USB carry raw bytes; OS_PRINT/SHARE carry markup/PDF. */
@Serializable
enum class ConnectionType { NETWORK, BLUETOOTH, USB, OS_PRINT, SHARE }

@Serializable
enum class Align { LEFT, CENTER, RIGHT }

/** Whether a binding resolves once per document (header) or once per line (table rows). */
@Serializable
enum class BindingScope { DOCUMENT, LINE }

/** Which entity a binding resolves against — the document itself or a nested entity. */
@Serializable
enum class EntityRef { SELF, CUSTOMER, PRODUCT }

/** Where the field's definition/value comes from. Mirrors feature/form's FormField.source plus COMPUTED. */
@Serializable
enum class FieldSource { STANDARD, CUSTOM, COMPUTED }

/** Page-mode band a block belongs to (thermal/label ignore this and flow top-to-bottom). */
@Serializable
enum class BandRegion { HEADER, COLUMN_HEADER, BODY, FOOTER }

@Serializable
enum class Symbology { CODE128, EAN13, UPCA, QR }

@Serializable
enum class PageSize { A4, A5, A6, A7, LETTER, LEGAL }

@Serializable
enum class Orientation { PORTRAIT, LANDSCAPE }

/** Spool job lifecycle. UNCONFIRMED = sent but not acknowledged — never auto-retried (FR-016). */
enum class PrintJobState { QUEUED, SENDING, SENT, CONFIRMED, FAILED, UNCONFIRMED, DEAD }
