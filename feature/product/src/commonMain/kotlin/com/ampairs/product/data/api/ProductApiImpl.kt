package com.ampairs.product.data.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.post
import com.ampairs.common.put
import com.ampairs.common.delete
import com.ampairs.common.model.Response
import com.ampairs.product.api.model.ProductApiModel
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode

const val PRODUCT_ENDPOINT = "http://localhost:8080"

class ProductApiImpl(
    engine: HttpClientEngine,
    tokenRepository: TokenRepository
) : ProductApi {

    private val client = httpClient(engine, tokenRepository)

    override suspend fun getProducts(workspaceId: String): Result<List<ProductApiModel>> {
        return try {
            val response: Response<List<ProductApiModel>> = get(
                client,
                "$PRODUCT_ENDPOINT/api/v1/products"
            )
            Result.success(response.data ?: emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getProduct(workspaceId: String, productId: String): Result<ProductApiModel> {
        return try {
            val response: Response<ProductApiModel> = get(
                client,
                "$PRODUCT_ENDPOINT/api/v1/products/$productId"
            )
            response.data?.let { Result.success(it) }
                ?: Result.failure(Exception("Product not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createProduct(workspaceId: String, product: ProductApiModel): Result<ProductApiModel> {
        return try {
            val response: Response<ProductApiModel> = post(
                client,
                "$PRODUCT_ENDPOINT/api/v1/products",
                product
            )
            response.data?.let { Result.success(it) }
                ?: Result.failure(Exception("Failed to create product"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProduct(workspaceId: String, productId: String, product: ProductApiModel): Result<ProductApiModel> {
        return try {
            // First try PUT (update existing)
            try {
                val response: Response<ProductApiModel> = put(
                    client,
                    "$PRODUCT_ENDPOINT/api/v1/products/$productId",
                    product
                )
                response.data?.let { Result.success(it) }
                    ?: Result.failure(Exception("Failed to update product"))
            } catch (e: ClientRequestException) {
                if (e.response.status == HttpStatusCode.NotFound) {
                    // Entity doesn't exist on backend (created offline), fallback to POST
                    try {
                        val createResponse: Response<ProductApiModel> = post(
                            client,
                            "$PRODUCT_ENDPOINT/api/v1/products",
                            product
                        )
                        createResponse.data?.let { Result.success(it) }
                            ?: Result.failure(Exception("Failed to create product"))
                    } catch (createException: Exception) {
                        Result.failure(Exception("Update failed (404), then create failed: ${createException.message}"))
                    }
                } else {
                    Result.failure(e)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteProduct(workspaceId: String, productId: String): Result<Unit> {
        return try {
            delete<Response<Unit>>(
                client,
                "$PRODUCT_ENDPOINT/api/v1/products/$productId"
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchProducts(workspaceId: String, query: String): Result<List<ProductApiModel>> {
        return try {
            val params = mapOf("q" to query)
            val response: Response<List<ProductApiModel>> = get(
                client,
                "$PRODUCT_ENDPOINT/api/v1/products/search",
                params
            )
            Result.success(response.data ?: emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}