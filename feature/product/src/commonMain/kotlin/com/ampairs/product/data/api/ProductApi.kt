package com.ampairs.product.data.api

import com.ampairs.common.model.PageResponse
import com.ampairs.product.api.model.ProductApiModel

interface ProductApi {
    suspend fun getProducts(): Result<List<ProductApiModel>>

    /**
     * Incremental sync feed (paginated): products updated since [lastSync], INCLUDING soft-deleted
     * (status=DELETED) so the client can permanently delete removed rows.
     */
    suspend fun getProductsSync(lastSync: String?, page: Int, size: Int): Result<PageResponse<ProductApiModel>>
    suspend fun getProduct(productId: String): Result<ProductApiModel>
    suspend fun createProduct(product: ProductApiModel): Result<ProductApiModel>
    suspend fun updateProduct(productId: String, product: ProductApiModel): Result<ProductApiModel>
    suspend fun bulkUpdateProducts(products: List<ProductApiModel>): Result<List<ProductApiModel>>
    suspend fun deleteProduct(productId: String): Result<Unit>
    suspend fun searchProducts(query: String): Result<List<ProductApiModel>>
}
