package com.ampairs.product.data.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.delete
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.model.Response
import com.ampairs.common.post
import com.ampairs.common.postList
import com.ampairs.common.put
import com.ampairs.product.api.model.ProductApiModel
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.engine.HttpClientEngine

@Inject @SingleIn(AppScope::class) @ContributesBinding(AppScope::class)
class ProductApiImpl(
    engine: HttpClientEngine,
    tokenRepository: TokenRepository
) : ProductApi {

    private val client = httpClient(engine, tokenRepository)

    override suspend fun getProducts(): Result<List<ProductApiModel>> {
        return try {
            val response: Response<List<ProductApiModel>> = get(
                client,
                ApiUrlBuilder.productUrl("v1/products")
            )
            if (response.data != null && response.error == null) {
                Result.success(response.data!!)
            } else {
                Result.failure(Exception(response.error?.message ?: "Server returned no data"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getProduct(productId: String): Result<ProductApiModel> {
        return try {
            val response: Response<ProductApiModel> = get(
                client,
                ApiUrlBuilder.productUrl("v1/products/$productId")
            )
            response.data?.let { Result.success(it) }
                ?: Result.failure(Exception("Product not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createProduct(product: ProductApiModel): Result<ProductApiModel> {
        return try {
            val response: Response<ProductApiModel> = post(
                client,
                ApiUrlBuilder.productUrl("v1/products"),
                product
            )
            response.data?.let { Result.success(it) }
                ?: Result.failure(Exception("Failed to create product"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProduct(productId: String, product: ProductApiModel): Result<ProductApiModel> = runCatching {
        val response: Response<ProductApiModel> = put(client, ApiUrlBuilder.productUrl("v1/products/$productId"), product)
        response.data ?: error("Failed to update product $productId")
    }

    override suspend fun bulkUpdateProducts(products: List<ProductApiModel>): Result<List<ProductApiModel>> = runCatching {
        val response: Response<List<ProductApiModel>> = postList(client, ApiUrlBuilder.productUrl("v1/products"), products)
        response.data ?: emptyList()
    }

    override suspend fun deleteProduct(productId: String): Result<Unit> {
        return try {
            delete<Response<Unit>>(
                client,
                ApiUrlBuilder.productUrl("v1/products/$productId")
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchProducts(query: String): Result<List<ProductApiModel>> {
        return try {
            val response: Response<List<ProductApiModel>> = get(
                client,
                ApiUrlBuilder.productUrl("v1/products/search"),
                mapOf("q" to query)
            )
            Result.success(response.data ?: emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
