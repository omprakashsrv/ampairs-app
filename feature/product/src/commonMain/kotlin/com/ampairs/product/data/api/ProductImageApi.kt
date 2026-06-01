package com.ampairs.product.data.api

import com.ampairs.product.domain.ProductUploadImage
import com.ampairs.product.domain.ProductUploadImageUploadResponse

interface ProductImageApi {
    suspend fun uploadProductImageMultipart(
        uid: String,
        productId: String,
        fileName: String,
        contentType: String,
        imageData: ByteArray,
        description: String?,
        isPrimary: Boolean,
        displayOrder: Int?,
    ): ProductUploadImageUploadResponse

    suspend fun getProductImages(productId: String, lastSync: String): List<ProductUploadImage>

    suspend fun deleteProductImage(productId: String, imageId: String)

    suspend fun setPrimaryImage(productId: String, imageId: String): ProductUploadImage
}
