package com.ampairs.product.data.api

import com.ampairs.product.api.model.ProductGroupApiModel

interface ProductCatalogApi {

    suspend fun getGroups(): Result<List<ProductGroupApiModel>>
    suspend fun createGroup(model: ProductGroupApiModel): Result<ProductGroupApiModel>
    suspend fun updateGroup(id: String, model: ProductGroupApiModel): Result<ProductGroupApiModel>

    suspend fun getCategories(): Result<List<ProductGroupApiModel>>
    suspend fun createCategory(model: ProductGroupApiModel): Result<ProductGroupApiModel>
    suspend fun updateCategory(id: String, model: ProductGroupApiModel): Result<ProductGroupApiModel>

    suspend fun getBrands(): Result<List<ProductGroupApiModel>>
    suspend fun createBrand(model: ProductGroupApiModel): Result<ProductGroupApiModel>
    suspend fun updateBrand(id: String, model: ProductGroupApiModel): Result<ProductGroupApiModel>

    suspend fun getSubCategories(): Result<List<ProductGroupApiModel>>
    suspend fun createSubCategory(model: ProductGroupApiModel): Result<ProductGroupApiModel>
    suspend fun updateSubCategory(id: String, model: ProductGroupApiModel): Result<ProductGroupApiModel>

    // Bulk upsert — send all unsynced rows of a catalog type in one request.
    suspend fun updateGroups(models: List<ProductGroupApiModel>): Result<List<ProductGroupApiModel>>
    suspend fun updateCategories(models: List<ProductGroupApiModel>): Result<List<ProductGroupApiModel>>
    suspend fun updateBrands(models: List<ProductGroupApiModel>): Result<List<ProductGroupApiModel>>
    suspend fun updateSubCategories(models: List<ProductGroupApiModel>): Result<List<ProductGroupApiModel>>
}
