package com.ampairs.purchase.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.di.AppScope
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.model.PageResponse
import com.ampairs.common.model.Response
import com.ampairs.common.post
import com.ampairs.purchase.api.model.PurchaseApiModel
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import io.ktor.client.engine.HttpClientEngine

@Inject @ContributesBinding(AppScope::class)
class PurchaseApiImpl(engine: HttpClientEngine, tokenRepository: TokenRepository) : PurchaseApi {

    // sendDefaults: the /sync push is a full-row upsert; the backend rejects bodies with missing
    // primitive fields, so defaulted values (0.0 totals, item_no 0, ...) must be sent.
    private val client = httpClient(engine, tokenRepository, sendDefaults = true)

    override suspend fun bulkUpdatePurchases(purchases: List<PurchaseApiModel>): List<PurchaseApiModel> {
        val response: Response<List<PurchaseApiModel>> = post(
            client,
            ApiUrlBuilder.purchaseUrl("v1/purchases/sync"),
            purchases
        )
        if (response.error != null) throw Exception(response.error?.message ?: "Bulk purchase push failed")
        return response.data ?: emptyList()
    }

    override suspend fun getPurchases(
        lastSync: String,
        page: Int,
        size: Int,
        sortBy: String,
        sortDir: String,
    ): PageResponse<PurchaseApiModel> {
        val params = mutableMapOf<String, Any>(
            "page" to page,
            "size" to size,
            "sort_by" to sortBy,
            "sort_dir" to sortDir,
        )
        if (lastSync.isNotBlank()) params["last_sync"] = lastSync

        val response: Response<PageResponse<PurchaseApiModel>> = get(
            client,
            ApiUrlBuilder.purchaseUrl("v1/purchases/sync"),
            params
        )
        if (response.error != null) throw Exception(response.error?.message ?: "Purchase pull failed")
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
