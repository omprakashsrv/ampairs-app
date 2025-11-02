package com.ampairs.product.data.api

import com.ampairs.product.api.model.ProductApiModel

/**
 * Product API interface following the customer module pattern
 */
interface ProductApi {
    suspend fun getProducts(workspaceId: String): Result<List<ProductApiModel>>
    suspend fun getProduct(workspaceId: String, productId: String): Result<ProductApiModel>
    suspend fun createProduct(workspaceId: String, product: ProductApiModel): Result<ProductApiModel>
    suspend fun updateProduct(workspaceId: String, productId: String, product: ProductApiModel): Result<ProductApiModel>
    suspend fun deleteProduct(workspaceId: String, productId: String): Result<Unit>
    suspend fun searchProducts(workspaceId: String, query: String): Result<List<ProductApiModel>>
}