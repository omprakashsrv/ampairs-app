package com.ampairs.product.data.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.delete
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.model.Response
import com.ampairs.common.postMultiPart
import com.ampairs.common.put
import com.ampairs.common.di.AppScope
import com.ampairs.product.api.model.ImageApiModel
import com.ampairs.product.domain.ProductUploadImage
import com.ampairs.product.domain.ProductUploadImageUploadResponse
import com.ampairs.product.util.ProductLogger
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.engine.HttpClientEngine
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.content.PartData
import io.ktor.utils.io.ByteReadChannel

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class ProductImageApiImpl(
    engine: HttpClientEngine,
    tokenRepository: TokenRepository,
) : ProductImageApi {

    private val client = httpClient(engine, tokenRepository)

    companion object {
        private const val TAG = "ProductImageApi"
    }

    override suspend fun uploadProductImageMultipart(
        uid: String,
        productId: String,
        fileName: String,
        contentType: String,
        imageData: ByteArray,
        description: String?,
        isPrimary: Boolean,
        displayOrder: Int?,
    ): ProductUploadImageUploadResponse {
        return try {
            ProductLogger.d(TAG, "Starting multipart upload for product: $productId, file: $fileName, uid: $uid")

            val parts = listOf(
                PartData.FileItem(
                    provider = { ByteReadChannel(imageData) },
                    dispose = {},
                    partHeaders = Headers.build {
                        append(HttpHeaders.ContentType, contentType)
                        append(HttpHeaders.ContentDisposition, "form-data; name=\"file\"; filename=\"$fileName\"")
                    }
                ),
                PartData.FormItem(
                    value = "/$productId",
                    dispose = {},
                    partHeaders = Headers.build {
                        append(HttpHeaders.ContentDisposition, "form-data; name=\"path\"")
                    }
                ),
            )

            val response: Response<ImageApiModel> = postMultiPart(
                client,
                ApiUrlBuilder.productUrl("v1/products/upload-image"),
                parts,
                requestTimeoutMillis = 120_000L,
            )

            ProductLogger.d(TAG, "Multipart upload completed for: $fileName")
            val apiImage = response.data ?: throw Exception("Upload response is null")

            ProductUploadImageUploadResponse(
                uid = apiImage.id.ifBlank { uid },
                imageUrl = apiImage.url.ifBlank { apiImage.objectKey },
                thumbnailUrl = "",
            )
        } catch (e: Exception) {
            ProductLogger.e(TAG, "Error uploading product image via multipart", e)
            throw e
        }
    }

    override suspend fun getProductImages(productId: String, lastSync: String): List<ProductUploadImage> {
        return try {
            val params = mutableMapOf<String, Any>()
            if (lastSync.isNotBlank()) {
                params["last_sync"] = lastSync
            }
            val response: Response<List<ProductUploadImage>> = get(
                client,
                ApiUrlBuilder.productUrl("v1/$productId/images"),
                params
            )
            response.data ?: emptyList()
        } catch (e: Exception) {
            ProductLogger.e(TAG, "Error getting product images for productId: $productId", e)
            emptyList()
        }
    }

    override suspend fun deleteProductImage(productId: String, imageId: String) {
        try {
            delete<Unit>(
                client,
                ApiUrlBuilder.productUrl("v1/images/$productId/$imageId")
            )
        } catch (e: Exception) {
            ProductLogger.e(TAG, "Error deleting product image: $imageId", e)
            throw e
        }
    }

    override suspend fun setPrimaryImage(productId: String, imageId: String): ProductUploadImage {
        return try {
            val response: Response<ProductUploadImage> = put(
                client,
                ApiUrlBuilder.productUrl("v1/images/$productId/$imageId/primary"),
                emptyMap<String, Any>()
            )
            response.data ?: throw Exception("Set primary response is null")
        } catch (e: Exception) {
            ProductLogger.e(TAG, "Error setting primary image: $imageId", e)
            throw e
        }
    }
}
