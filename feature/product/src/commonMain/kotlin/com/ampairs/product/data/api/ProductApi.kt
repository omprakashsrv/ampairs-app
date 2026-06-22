package com.ampairs.product.data.api

import com.ampairs.common.model.PageResponse
import com.ampairs.product.api.model.ProductApiModel

interface ProductApi {

    /**
     * Incremental sync feed (paginated): products updated since [lastSync], INCLUDING soft-deleted
     * (status=DELETED) so the client can permanently delete removed rows.
     */
    suspend fun getProductsSync(
        lastSync: String?,
        page: Int,
        size: Int,
        sortBy: String = "updatedAt",
        sortDir: String = "ASC",
    ): Result<PageResponse<ProductApiModel>>

    /** Bulk push of unsynced products (creates/updates + in-band soft-deletes via status=DELETED). */
    suspend fun bulkUpdateProducts(products: List<ProductApiModel>): Result<List<ProductApiModel>>

    // Non-sync UI endpoints.
    suspend fun getProduct(productId: String): Result<ProductApiModel>
    suspend fun searchProducts(query: String): Result<List<ProductApiModel>>

    /**
     * Toggle whether a product is listed on the workspace's ecom storefront. Online, UI-invoked
     * action; the backend updates the storefront catalog asynchronously.
     */
    suspend fun setEcomListing(productId: String, listed: Boolean): Result<ProductApiModel>
}
