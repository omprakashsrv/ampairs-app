package com.ampairs.product.data.api

import com.ampairs.product.api.model.ProductApiModel

interface ProductApi {
    suspend fun getProducts(): Result<List<ProductApiModel>>
    suspend fun getProduct(productId: String): Result<ProductApiModel>
    suspend fun createProduct(product: ProductApiModel): Result<ProductApiModel>
    suspend fun updateProduct(productId: String, product: ProductApiModel): Result<ProductApiModel>
    suspend fun deleteProduct(productId: String): Result<Unit>
    suspend fun searchProducts(query: String): Result<List<ProductApiModel>>
}
