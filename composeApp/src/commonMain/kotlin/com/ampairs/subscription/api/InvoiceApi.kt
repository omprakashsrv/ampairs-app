package com.ampairs.subscription.api

import com.ampairs.common.model.Response
import com.ampairs.subscription.domain.model.*

/**
 * API interface for subscription invoice operations
 */
interface InvoiceApi {

    /**
     * Get all invoices for current workspace
     */
    suspend fun getInvoices(
        workspaceId: String,
        status: InvoiceStatus? = null,
        page: Int = 0,
        size: Int = 20
    ): Response<PagedResponse<Invoice>>

    /**
     * Get invoice by UID
     */
    suspend fun getInvoice(invoiceUid: String): Response<Invoice>

    /**
     * Get invoice summary for dashboard
     */
    suspend fun getInvoiceSummary(workspaceId: String): Response<InvoiceSummary>

    /**
     * Pay an invoice (generate payment link or auto-charge)
     */
    suspend fun payInvoice(
        invoiceUid: String,
        request: PayInvoiceRequest
    ): Response<PaymentLinkResponse>

    /**
     * Retry failed payment
     */
    suspend fun retryPayment(invoiceUid: String): Response<PaymentLinkResponse>
}

/**
 * Paged response wrapper
 */
@kotlinx.serialization.Serializable
data class PagedResponse<T>(
    @kotlinx.serialization.SerialName("content")
    val content: List<T>,

    @kotlinx.serialization.SerialName("total_pages")
    val totalPages: Int,

    @kotlinx.serialization.SerialName("total_elements")
    val totalElements: Long,

    @kotlinx.serialization.SerialName("size")
    val size: Int,

    @kotlinx.serialization.SerialName("number")
    val number: Int,

    @kotlinx.serialization.SerialName("first")
    val first: Boolean = false,

    @kotlinx.serialization.SerialName("last")
    val last: Boolean = false,

    @kotlinx.serialization.SerialName("empty")
    val empty: Boolean = false
)
