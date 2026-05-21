package com.ampairs.subscription.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.model.Response
import com.ampairs.common.post
import com.ampairs.subscription.domain.model.*
import io.ktor.client.engine.HttpClientEngine

/**
 * Implementation of InvoiceApi using Ktor HTTP client
 */
@Inject @SingleIn(AppScope::class) @ContributesBinding(AppScope::class)
class InvoiceApiImpl(
    engine: HttpClientEngine,
    tokenRepository: TokenRepository
) : InvoiceApi {

    private val client = httpClient(engine, tokenRepository)

    override suspend fun getInvoices(
        workspaceId: String,
        status: InvoiceStatus?,
        page: Int,
        size: Int
    ): Response<PagedResponse<Invoice>> {
        val url = ApiUrlBuilder.subscriptionUrl("v1/subscription/invoices") +
                "?workspaceId=$workspaceId" +
                (status?.let { "&status=${it.name}" } ?: "") +
                "&page=$page&size=$size&sort=createdAt,DESC"

        return get(client, url)
    }

    override suspend fun getInvoice(invoiceUid: String): Response<Invoice> {
        return get(client, ApiUrlBuilder.subscriptionUrl("v1/subscription/invoices/$invoiceUid"))
    }

    override suspend fun getInvoiceSummary(workspaceId: String): Response<InvoiceSummary> {
        val url = ApiUrlBuilder.subscriptionUrl("v1/subscription/invoices/summary") +
                "?workspaceId=$workspaceId"
        return get(client, url)
    }

    override suspend fun payInvoice(
        invoiceUid: String,
        request: PayInvoiceRequest
    ): Response<PaymentLinkResponse> {
        return post(
            client,
            ApiUrlBuilder.subscriptionUrl("v1/subscription/invoices/$invoiceUid/pay"),
            request
        )
    }

    override suspend fun retryPayment(invoiceUid: String): Response<PaymentLinkResponse> {
        return post(
            client,
            ApiUrlBuilder.subscriptionUrl("v1/subscription/invoices/$invoiceUid/retry-payment"),
            Unit  // Empty body
        )
    }
}
