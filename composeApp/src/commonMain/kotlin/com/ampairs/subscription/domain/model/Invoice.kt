package com.ampairs.subscription.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Subscription invoice model
 */
@Serializable
data class Invoice(
    @SerialName("uid")
    val uid: String,

    @SerialName("invoice_number")
    val invoiceNumber: String,

    @SerialName("subscription_id")
    val subscriptionId: String,

    @SerialName("workspace_id")
    val workspaceId: String,

    @SerialName("status")
    val status: InvoiceStatus,

    @SerialName("billing_period_start")
    val billingPeriodStart: String,

    @SerialName("billing_period_end")
    val billingPeriodEnd: String,

    @SerialName("due_date")
    val dueDate: String,

    @SerialName("total_amount")
    val totalAmount: Double,

    @SerialName("paid_amount")
    val paidAmount: Double,

    @SerialName("remaining_balance")
    val remainingBalance: Double,

    @SerialName("currency")
    val currency: String,

    @SerialName("auto_payment_enabled")
    val autoPaymentEnabled: Boolean = false,

    @SerialName("payment_method_id")
    val paymentMethodId: Long? = null,

    @SerialName("payment_link_url")
    val paymentLinkUrl: String? = null,

    @SerialName("paid_at")
    val paidAt: String? = null,

    @SerialName("created_at")
    val createdAt: String,

    @SerialName("updated_at")
    val updatedAt: String
) {
    /**
     * Check if invoice is overdue
     */
    fun isOverdue(): Boolean {
        // TODO: Implement date comparison with current time
        return status == InvoiceStatus.OVERDUE
    }

    /**
     * Check if invoice is fully paid
     */
    fun isFullyPaid(): Boolean {
        return status == InvoiceStatus.PAID || remainingBalance <= 0.0
    }

    /**
     * Check if invoice can be paid
     */
    fun canBePaid(): Boolean {
        return status in listOf(
            InvoiceStatus.PENDING,
            InvoiceStatus.OVERDUE,
            InvoiceStatus.PARTIALLY_PAID
        ) && remainingBalance > 0.0
    }
}

/**
 * Invoice status enum
 */
@Serializable
enum class InvoiceStatus {
    @SerialName("PENDING")
    PENDING,

    @SerialName("PAID")
    PAID,

    @SerialName("OVERDUE")
    OVERDUE,

    @SerialName("CANCELLED")
    CANCELLED,

    @SerialName("PARTIALLY_PAID")
    PARTIALLY_PAID
}

/**
 * Invoice summary for dashboard
 */
@Serializable
data class InvoiceSummary(
    @SerialName("total_invoices")
    val totalInvoices: Int,

    @SerialName("pending_invoices")
    val pendingInvoices: Int,

    @SerialName("overdue_invoices")
    val overdueInvoices: Int,

    @SerialName("total_outstanding")
    val totalOutstanding: Double,

    @SerialName("next_due_date")
    val nextDueDate: String? = null,

    @SerialName("next_invoice_amount")
    val nextInvoiceAmount: Double? = null
)

/**
 * Payment link response
 */
@Serializable
data class PaymentLinkResponse(
    @SerialName("invoice_uid")
    val invoiceUid: String,

    @SerialName("payment_link_url")
    val paymentLinkUrl: String,

    @SerialName("expires_at")
    val expiresAt: String? = null
)

/**
 * Pay invoice request
 */
@Serializable
data class PayInvoiceRequest(
    @SerialName("use_auto_charge")
    val useAutoCharge: Boolean = false
)
