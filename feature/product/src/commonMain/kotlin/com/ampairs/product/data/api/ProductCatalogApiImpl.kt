package com.ampairs.product.data.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.model.PageResponse
import com.ampairs.common.model.Response
import com.ampairs.common.postList
import com.ampairs.common.di.AppScope
import com.ampairs.product.api.model.ProductGroupApiModel
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class ProductCatalogApiImpl(
    engine: io.ktor.client.engine.HttpClientEngine,
    tokenRepository: TokenRepository,
) : ProductCatalogApi {

    private val client = httpClient(engine, tokenRepository)

    private suspend fun pullPage(
        path: String,
        lastSync: String?,
        page: Int,
        size: Int,
    ): Result<PageResponse<ProductGroupApiModel>> = runCatching {
        val params = mutableMapOf(
            "page" to page.toString(),
            "size" to size.toString(),
        )
        if (!lastSync.isNullOrBlank()) params["last_sync"] = lastSync
        val response: Response<PageResponse<ProductGroupApiModel>> = get(
            client,
            ApiUrlBuilder.productUrl(path),
            params,
        )
        if (response.data != null && response.error == null) {
            response.data!!
        } else {
            throw Exception(response.error?.message ?: "Server returned no data")
        }
    }

    override suspend fun getGroupsSync(
        lastSync: String?,
        page: Int,
        size: Int,
    ): Result<PageResponse<ProductGroupApiModel>> =
        pullPage("v1/products/groups/sync", lastSync, page, size)

    override suspend fun getCategoriesSync(
        lastSync: String?,
        page: Int,
        size: Int,
    ): Result<PageResponse<ProductGroupApiModel>> =
        pullPage("v1/products/categories/sync", lastSync, page, size)

    override suspend fun getBrandsSync(
        lastSync: String?,
        page: Int,
        size: Int,
    ): Result<PageResponse<ProductGroupApiModel>> =
        pullPage("v1/products/brands/sync", lastSync, page, size)

    override suspend fun getSubCategoriesSync(
        lastSync: String?,
        page: Int,
        size: Int,
    ): Result<PageResponse<ProductGroupApiModel>> =
        pullPage("v1/products/sub-categories/sync", lastSync, page, size)

    override suspend fun updateGroups(models: List<ProductGroupApiModel>): Result<List<ProductGroupApiModel>> = runCatching {
        if (models.isEmpty()) return@runCatching emptyList()
        val response: Response<List<ProductGroupApiModel>> = postList(client, ApiUrlBuilder.productUrl("v1/products/groups/sync"), models)
        response.data ?: models
    }

    override suspend fun updateCategories(models: List<ProductGroupApiModel>): Result<List<ProductGroupApiModel>> = runCatching {
        if (models.isEmpty()) return@runCatching emptyList()
        val response: Response<List<ProductGroupApiModel>> = postList(client, ApiUrlBuilder.productUrl("v1/products/categories/sync"), models)
        response.data ?: models
    }

    override suspend fun updateBrands(models: List<ProductGroupApiModel>): Result<List<ProductGroupApiModel>> = runCatching {
        if (models.isEmpty()) return@runCatching emptyList()
        val response: Response<List<ProductGroupApiModel>> = postList(client, ApiUrlBuilder.productUrl("v1/products/brands/sync"), models)
        response.data ?: models
    }

    override suspend fun updateSubCategories(models: List<ProductGroupApiModel>): Result<List<ProductGroupApiModel>> = runCatching {
        if (models.isEmpty()) return@runCatching emptyList()
        val response: Response<List<ProductGroupApiModel>> = postList(client, ApiUrlBuilder.productUrl("v1/products/sub-categories/sync"), models)
        response.data ?: models
    }
}
