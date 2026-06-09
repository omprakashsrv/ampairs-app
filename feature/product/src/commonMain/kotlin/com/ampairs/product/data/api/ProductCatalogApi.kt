package com.ampairs.product.data.api

import com.ampairs.common.model.PageResponse
import com.ampairs.product.api.model.ProductGroupApiModel

interface ProductCatalogApi {

    /**
     * Paginated incremental sync feed for each catalog type. Backend has no soft-delete on catalog
     * entities, so these are upsert-only. [lastSync] is sent only when non-blank.
     */
    suspend fun getGroupsSync(
        lastSync: String?,
        page: Int,
        size: Int,
    ): Result<PageResponse<ProductGroupApiModel>>

    suspend fun getCategoriesSync(
        lastSync: String?,
        page: Int,
        size: Int,
    ): Result<PageResponse<ProductGroupApiModel>>

    suspend fun getBrandsSync(
        lastSync: String?,
        page: Int,
        size: Int,
    ): Result<PageResponse<ProductGroupApiModel>>

    suspend fun getSubCategoriesSync(
        lastSync: String?,
        page: Int,
        size: Int,
    ): Result<PageResponse<ProductGroupApiModel>>

    // Bulk upsert — send all unsynced rows of a catalog type in one request.
    suspend fun updateGroups(models: List<ProductGroupApiModel>): Result<List<ProductGroupApiModel>>
    suspend fun updateCategories(models: List<ProductGroupApiModel>): Result<List<ProductGroupApiModel>>
    suspend fun updateBrands(models: List<ProductGroupApiModel>): Result<List<ProductGroupApiModel>>
    suspend fun updateSubCategories(models: List<ProductGroupApiModel>): Result<List<ProductGroupApiModel>>
}
