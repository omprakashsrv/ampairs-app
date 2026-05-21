package com.ampairs.subscription.repository

import com.ampairs.subscription.api.InvoiceApi
import com.ampairs.subscription.domain.model.*
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Repository for invoice operations
 * Handles data fetching from API (no local caching for now)
 */
@Inject
class InvoiceRepository(
    private val api: InvoiceApi
) {

    /**
     * Get all invoices for workspace
     */
    fun getInvoices(
        workspaceId: String,
        status: InvoiceStatus? = null,
        page: Int = 0,
        size: Int = 20
    ): Flow<Result<List<Invoice>>> = flow {
        try {
            val response = api.getInvoices(workspaceId, status, page, size)
            val data = response.data
            if (data != null && response.error == null) {
                emit(Result.success(data.content))
            } else {
                emit(Result.failure(Exception(response.error?.message ?: "Failed to fetch invoices")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    /**
     * Get single invoice by UID
     */
    fun getInvoice(invoiceUid: String): Flow<Result<Invoice>> = flow {
        try {
            val response = api.getInvoice(invoiceUid)
            val data = response.data
            if (data != null && response.error == null) {
                emit(Result.success(data))
            } else {
                emit(Result.failure(Exception(response.error?.message ?: "Failed to fetch invoice")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    /**
     * Get invoice summary for dashboard
     */
    fun getInvoiceSummary(workspaceId: String): Flow<Result<InvoiceSummary>> = flow {
        try {
            val response = api.getInvoiceSummary(workspaceId)
            val data = response.data
            if (data != null && response.error == null) {
                emit(Result.success(data))
            } else {
                emit(Result.failure(Exception(response.error?.message ?: "Failed to fetch summary")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    /**
     * Pay an invoice
     */
    suspend fun payInvoice(
        invoiceUid: String,
        useAutoCharge: Boolean = false
    ): Result<PaymentLinkResponse> {
        return try {
            val request = PayInvoiceRequest(useAutoCharge = useAutoCharge)
            val response = api.payInvoice(invoiceUid, request)
            val data = response.data

            if (data != null && response.error == null) {
                Result.success(data)
            } else {
                // Check if it's the "success" error for auto-charge
                if (response.error?.message?.contains("Payment processed successfully") == true) {
                    // Auto-charge was successful, return empty payment link
                    Result.success(
                        PaymentLinkResponse(
                            invoiceUid = invoiceUid,
                            paymentLinkUrl = ""  // Empty for auto-charge success
                        )
                    )
                } else {
                    Result.failure(Exception(response.error?.message ?: "Failed to process payment"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Retry failed payment
     */
    suspend fun retryPayment(invoiceUid: String): Result<PaymentLinkResponse> {
        return try {
            val response = api.retryPayment(invoiceUid)
            val data = response.data

            if (data != null && response.error == null) {
                Result.success(data)
            } else {
                // Check if it's the "success" error for auto-charge
                if (response.error?.message?.contains("Payment processed successfully") == true) {
                    Result.success(
                        PaymentLinkResponse(
                            invoiceUid = invoiceUid,
                            paymentLinkUrl = ""
                        )
                    )
                } else {
                    Result.failure(Exception(response.error?.message ?: "Failed to retry payment"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
