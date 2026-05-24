package com.ampairs.invoice.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.post
import com.ampairs.invoice.api.model.InvoiceApiModel
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
}
