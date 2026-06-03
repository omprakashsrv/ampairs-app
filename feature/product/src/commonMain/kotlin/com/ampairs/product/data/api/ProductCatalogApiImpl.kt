package com.ampairs.product.data.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.model.Response
import com.ampairs.common.postList
import com.ampairs.common.di.AppScope
import com.ampairs.product.api.model.AllProductGroupApiModel
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

    override suspend fun getGroups(): Result<List<ProductGroupApiModel>> = runCatching {
        val response: Response<List<ProductGroupApiModel>> = get(client, ApiUrlBuilder.productUrl("v1/products/groups"))
        if (response.error != null) throw Exception(response.error?.message ?: "Network error")
        response.data ?: emptyList()
    }

    override suspend fun createGroup(model: ProductGroupApiModel): Result<ProductGroupApiModel> = runCatching {
        val response: Response<List<ProductGroupApiModel>> = postList(client, ApiUrlBuilder.productUrl("v1/products/groups"), listOf(model))
        response.data?.firstOrNull() ?: model
    }

    override suspend fun updateGroup(id: String, model: ProductGroupApiModel): Result<ProductGroupApiModel> = runCatching {
        val response: Response<List<ProductGroupApiModel>> = postList(client, ApiUrlBuilder.productUrl("v1/products/groups"), listOf(model))
        response.data?.firstOrNull() ?: model
    }

    override suspend fun getCategories(): Result<List<ProductGroupApiModel>> = runCatching {
        val response: Response<AllProductGroupApiModel> = get(client, ApiUrlBuilder.productUrl("v1/products/all-groups-category"))
        if (response.error != null) throw Exception(response.error?.message ?: "Network error")
        response.data?.categories ?: emptyList()
    }

    override suspend fun createCategory(model: ProductGroupApiModel): Result<ProductGroupApiModel> = runCatching {
        val response: Response<List<ProductGroupApiModel>> = postList(client, ApiUrlBuilder.productUrl("v1/products/categories"), listOf(model))
        response.data?.firstOrNull() ?: model
    }

    override suspend fun updateCategory(id: String, model: ProductGroupApiModel): Result<ProductGroupApiModel> = runCatching {
        val response: Response<List<ProductGroupApiModel>> = postList(client, ApiUrlBuilder.productUrl("v1/products/categories"), listOf(model))
        response.data?.firstOrNull() ?: model
    }

    override suspend fun getBrands(): Result<List<ProductGroupApiModel>> = runCatching {
        val response: Response<List<ProductGroupApiModel>> = get(client, ApiUrlBuilder.productUrl("v1/products/brands"))
        if (response.error != null) throw Exception(response.error?.message ?: "Network error")
        response.data ?: emptyList()
    }

    override suspend fun createBrand(model: ProductGroupApiModel): Result<ProductGroupApiModel> = runCatching {
        val response: Response<List<ProductGroupApiModel>> = postList(client, ApiUrlBuilder.productUrl("v1/products/brands"), listOf(model))
        response.data?.firstOrNull() ?: model
    }

    override suspend fun updateBrand(id: String, model: ProductGroupApiModel): Result<ProductGroupApiModel> = runCatching {
        val response: Response<List<ProductGroupApiModel>> = postList(client, ApiUrlBuilder.productUrl("v1/products/brands"), listOf(model))
        response.data?.firstOrNull() ?: model
    }

    override suspend fun getSubCategories(): Result<List<ProductGroupApiModel>> = runCatching {
        val response: Response<List<ProductGroupApiModel>> = get(client, ApiUrlBuilder.productUrl("v1/products/sub-categories"))
        if (response.error != null) throw Exception(response.error?.message ?: "Network error")
        response.data ?: emptyList()
    }

    override suspend fun createSubCategory(model: ProductGroupApiModel): Result<ProductGroupApiModel> = runCatching {
        val response: Response<List<ProductGroupApiModel>> = postList(client, ApiUrlBuilder.productUrl("v1/products/sub-categories"), listOf(model))
        response.data?.firstOrNull() ?: model
    }

    override suspend fun updateSubCategory(id: String, model: ProductGroupApiModel): Result<ProductGroupApiModel> = runCatching {
        val response: Response<List<ProductGroupApiModel>> = postList(client, ApiUrlBuilder.productUrl("v1/products/sub-categories"), listOf(model))
        response.data?.firstOrNull() ?: model
    }
}
