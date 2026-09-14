package com.ampairs.ecom.data.repository

import com.ampairs.ecom.api.model.BuyerInvoiceDetail
import com.ampairs.ecom.api.model.BuyerInvoiceSummary
import com.ampairs.ecom.api.model.PageResponse
import com.ampairs.ecom.data.api.EcomApi
import com.ampairs.ecom.domain.EcomLogger
import com.ampairs.ecom.domain.EcomSession
import dev.zacsweers.metro.Inject

/**
 * Spec 029 — buyer invoices (list + detail) and the order↔invoice link. Live, UI-invoked reads
 * (no local cache) scoped to the active storefront; the backend gates access to the linked CRM
 * customer, so an unlinked buyer gets a 403 surfaced here as a failed [Result].
 */
@Inject
class BuyerInvoiceRepository(
    private val api: EcomApi,
    private val session: EcomSession,
) {
    suspend fun getInvoices(page: Int = 0, size: Int = 20): Result<PageResponse<BuyerInvoiceSummary>> {
        val slug = session.activeSlug ?: return Result.failure(IllegalStateException("No active storefront"))
        return api.getInvoices(slug, page = page, size = size)
            .onFailure { EcomLogger.w("Invoice", "list failed", it) }
    }

    suspend fun getInvoice(invoiceUid: String): Result<BuyerInvoiceDetail> {
        val slug = session.activeSlug ?: return Result.failure(IllegalStateException("No active storefront"))
        return api.getInvoice(slug, invoiceUid)
            .onFailure { EcomLogger.w("Invoice", "detail failed", it) }
    }

    suspend fun getOrderInvoices(ecomOrderRef: String): Result<List<BuyerInvoiceSummary>> {
        val slug = session.activeSlug ?: return Result.failure(IllegalStateException("No active storefront"))
        return api.getOrderInvoices(slug, ecomOrderRef)
            .onFailure { EcomLogger.w("Invoice", "order invoices failed", it) }
    }
}
