package com.ampairs.product.data.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.model.PageResponse
import com.ampairs.common.model.Response
import com.ampairs.common.postList
import com.ampairs.common.di.AppScope
import com.ampairs.product.api.model.ProductStandardCostApiModel
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class ProductStandardCostApiImpl(
    engine: io.ktor.client.engine.HttpClientEngine,
    tokenRepository: TokenRepository,
) : ProductStandardCostApi {

    private val client = httpClient(engine, tokenRepository)

    private val syncPath = "v1/standard-costs/sync"

    override suspend fun pull(
        lastSync: String?,
        page: Int,
        size: Int,
    ): Result<PageResponse<ProductStandardCostApiModel>> = runCatching {
        val params = mutableMapOf(
            "page" to page.toString(),
            "size" to size.toString(),
            "sort_by" to "updatedAt",
            "sort_dir" to "ASC",
        )
        if (!lastSync.isNullOrBlank()) params["last_sync"] = lastSync
        val response: Response<PageResponse<ProductStandardCostApiModel>> = get(
            client,
            ApiUrlBuilder.productUrl(syncPath),
            params,
        )
        if (response.data != null && response.error == null) {
            response.data!!
        } else {
            throw Exception(response.error?.message ?: "Server returned no data")
        }
    }

    override suspend fun push(
        models: List<ProductStandardCostApiModel>,
    ): Result<List<ProductStandardCostApiModel>> = runCatching {
        if (models.isEmpty()) return@runCatching emptyList()
        val response: Response<List<ProductStandardCostApiModel>> =
            postList(client, ApiUrlBuilder.productUrl(syncPath), models)
        response.data ?: models
    }
}
