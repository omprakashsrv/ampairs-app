package com.ampairs.invoice.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.post
import com.ampairs.invoice.api.model.InvoiceApiModel
import com.ampairs.common.model.PageResponse
import com.ampairs.common.model.Response
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import io.ktor.client.engine.HttpClientEngine

@Inject @ContributesBinding(AppScope::class)
class InvoiceApiImpl(engine: HttpClientEngine, tokenRepository: TokenRepository) : InvoiceApi {

    private val client = httpClient(engine, tokenRepository)

    override suspend fun updateInvoice(invoice: InvoiceApiModel): Response<InvoiceApiModel> {
        return post(
            client,
            ApiUrlBuilder.invoiceUrl("v1/invoices"),
            invoice
        )
    }

    override suspend fun getInvoices(lastUpdated: Long): Response<List<InvoiceApiModel>> {
        return get(
            client,
            ApiUrlBuilder.invoiceUrl("v1/invoices"),
            buildMap {
                put("last_updated", lastUpdated)
            }
        )
    }

    override suspend fun bulkUpdateInvoices(invoices: List<InvoiceApiModel>): List<InvoiceApiModel> {
        val response: Response<List<InvoiceApiModel>> = post(
            client,
            ApiUrlBuilder.invoiceUrl("v1/invoices/sync"),
            invoices
        )
        if (response.error != null) throw Exception(response.error?.message ?: "Bulk invoice push failed")
        return response.data ?: emptyList()
    }

    override suspend fun getInvoicesSync(
        lastSync: String,
        page: Int,
        size: Int,
        sortBy: String,
        sortDir: String,
    ): PageResponse<InvoiceApiModel> {
        val params = mutableMapOf<String, Any>(
            "page" to page,
            "size" to size,
            "sort_by" to sortBy,
            "sort_dir" to sortDir,
        )
        if (lastSync.isNotBlank()) params["last_sync"] = lastSync

        val response: Response<PageResponse<InvoiceApiModel>> = get(
            client,
            ApiUrlBuilder.invoiceUrl("v1/invoices/sync"),
            params
        )
        if (response.error != null) throw Exception(response.error?.message ?: "Invoice pull failed")
        return response.data ?: PageResponse(
            content = emptyList(),
            pageNumber = page,
            pageSize = size,
            totalPages = 0,
            totalElements = 0L,
            hasNext = false,
            hasPrevious = false,
            first = true,
            last = true,
        )
    }
}
