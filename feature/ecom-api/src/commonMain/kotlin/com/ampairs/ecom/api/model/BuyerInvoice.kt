package com.ampairs.ecom.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Spec 029 — buyer-facing invoice, statement and money-position DTOs served under
 * `/api/v1/ecom/account/**`. Wire shapes mirror the backend `ecom` controller's buyer-safe DTOs
 * (finalized invoices only; no cost/margin; enums already mapped to strings; `order_ref` is the
 * buyer-facing storefront order ref, or null for a non-ecom invoice).
 */
@Serializable
data class BuyerInvoiceSummary(
    @SerialName("invoice_uid") val invoiceUid: String = "",
    @SerialName("invoice_number") val invoiceNumber: String = "",
    @SerialName("invoice_date") val invoiceDate: String = "",
    @SerialName("status") val status: String = "",
    @SerialName("total") val total: Double = 0.0,
    @SerialName("order_ref") val orderRef: String? = null,
)

@Serializable
data class BuyerInvoiceLine(
    @SerialName("description") val description: String = "",
    @SerialName("quantity") val quantity: Double = 0.0,
    @SerialName("unit_price") val unitPrice: Double = 0.0,
    @SerialName("line_total") val lineTotal: Double = 0.0,
)

@Serializable
data class BuyerInvoiceDetail(
    @SerialName("invoice_uid") val invoiceUid: String = "",
    @SerialName("invoice_number") val invoiceNumber: String = "",
    @SerialName("invoice_date") val invoiceDate: String = "",
    @SerialName("status") val status: String = "",
    @SerialName("order_ref") val orderRef: String? = null,
    @SerialName("lines") val lines: List<BuyerInvoiceLine> = emptyList(),
    @SerialName("subtotal") val subtotal: Double = 0.0,
    @SerialName("tax_total") val taxTotal: Double = 0.0,
    @SerialName("total") val total: Double = 0.0,
)

@Serializable
data class BuyerOpenBill(
    @SerialName("bill_no") val billNo: String? = null,
    @SerialName("bill_date") val billDate: String = "",
    @SerialName("total") val total: Double = 0.0,
    @SerialName("outstanding") val outstanding: Double = 0.0,
    @SerialName("due_date") val dueDate: String? = null,
    @SerialName("days_overdue") val daysOverdue: Long = 0,
    @SerialName("aging_bucket") val agingBucket: String = "",
)

@Serializable
data class BuyerAgingBucket(
    @SerialName("label") val label: String = "",
    @SerialName("amount") val amount: Double = 0.0,
)

@Serializable
data class BuyerOutstanding(
    @SerialName("current_balance") val currentBalance: Double = 0.0,
    @SerialName("balance_direction") val balanceDirection: String = "DR",
    @SerialName("open_bills") val openBills: List<BuyerOpenBill> = emptyList(),
    @SerialName("aging") val aging: List<BuyerAgingBucket> = emptyList(),
)

@Serializable
data class BuyerStatementLine(
    @SerialName("date") val date: String = "",
    @SerialName("kind") val kind: String = "",
    @SerialName("reference") val reference: String? = null,
    @SerialName("narration") val narration: String? = null,
    @SerialName("debit") val debit: Double = 0.0,
    @SerialName("credit") val credit: Double = 0.0,
    @SerialName("running_balance") val runningBalance: Double = 0.0,
)

@Serializable
data class BuyerStatement(
    @SerialName("from") val from: String? = null,
    @SerialName("to") val to: String? = null,
    @SerialName("opening_balance") val openingBalance: Double = 0.0,
    @SerialName("opening_direction") val openingDirection: String = "DR",
    @SerialName("lines") val lines: List<BuyerStatementLine> = emptyList(),
    @SerialName("closing_balance") val closingBalance: Double = 0.0,
    @SerialName("closing_direction") val closingDirection: String = "DR",
)
