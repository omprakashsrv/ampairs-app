package com.ampairs.invoice.api

import com.ampairs.invoice.api.model.InvoiceApiModel
import com.ampairs.common.model.PageResponse
import com.ampairs.common.model.Response

interface InvoiceApi {

    suspend fun getInvoices(lastUpdated: Long): Response<List<InvoiceApiModel>>

    /** Bulk upsert for offline sync (spec 010). Server stores taxInfos/totals as supplied. */
    suspend fun bulkUpdateInvoices(invoices: List<InvoiceApiModel>): List<InvoiceApiModel>

    /** Paginated incremental pull for offline sync (spec 010). */
    suspend fun getInvoicesSync(
        lastSync: String = "",
        page: Int = 0,
        size: Int = 100,
        sortBy: String = "updatedAt",
        sortDir: String = "ASC",
    ): PageResponse<InvoiceApiModel>
}