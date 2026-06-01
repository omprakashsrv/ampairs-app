package com.ampairs.product.data.api

import com.ampairs.product.api.model.ImageApiModel
import com.ampairs.product.api.model.ProductGroupApiModel

interface ProductCatalogApi {

    // type = ProductClassificationType enum name: BRAND | GROUP | CATEGORY | SUB_CATEGORY
    suspend fun uploadCatalogItemImage(
        type: String,
        fileName: String,
        contentType: String,
        imageData: ByteArray,
    ): Result<ImageApiModel>

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
}
