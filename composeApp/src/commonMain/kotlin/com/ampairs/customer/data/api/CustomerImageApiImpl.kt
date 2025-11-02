package com.ampairs.customer.data.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.post
import com.ampairs.common.postMultiPart
import com.ampairs.common.put
import com.ampairs.common.delete
import com.ampairs.common.model.Response
import com.ampairs.customer.domain.CustomerImage
import com.ampairs.customer.domain.CustomerImageListResponse
import com.ampairs.customer.domain.CustomerImageUploadRequest
import com.ampairs.customer.domain.CustomerImageUploadResponse
import com.ampairs.customer.domain.CustomerImageUpdateRequest
import com.ampairs.customer.domain.CustomerImageBulkRequest
import com.ampairs.customer.domain.ThumbnailResponse
import com.ampairs.customer.util.CustomerLogger
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.content.PartData
import io.ktor.utils.io.ByteReadChannel

/**
 * Implementation of CustomerImageApi using Ktor HTTP client.
 * Provides REST API integration for customer image operations.
 */
class CustomerImageApiImpl(
    engine: HttpClientEngine,
    tokenRepository: TokenRepository
) : CustomerImageApi {

    private val client = httpClient(engine, tokenRepository)

    override suspend fun getCustomerImages(
        customerId: String,
        lastSync: String
    ): List<CustomerImage> {
        val params = mutableMapOf<String, Any>()
        if (lastSync.isNotBlank()) {
            params["last_sync"] = lastSync
        }

        return try {
            val response: Response<CustomerImageListResponse> = get(
                client,
                ApiUrlBuilder.customerUrl("v1/$customerId/images"),
                params
            )
            response.data?.images ?: emptyList()
        } catch (e: Exception) {
            CustomerLogger.e("CustomerImageApi", "Error getting customer images", e)
            throw e // Propagate exception to repository for proper error handling
        }
    }

    override suspend fun getAllCustomerImages(
        customerId: String,
        lastSync: String
    ): List<CustomerImage> {
        val params = mutableMapOf<String, Any>()
        if (lastSync.isNotBlank()) {
            params["last_sync"] = lastSync
        }

        return try {
            val response: Response<CustomerImageListResponse> = get(
                client,
                ApiUrlBuilder.customerUrl("v1/$customerId/images"),
                params
            )
            response.data?.images ?: emptyList()
        } catch (e: Exception) {
            CustomerLogger.e("CustomerImageApi", "Error getting all customer images", e)
            emptyList()
        }
    }

    override suspend fun getCustomerImage(customerId: String, imageId: String): CustomerImage? {
        return try {
            val response: Response<CustomerImage> = get(
                client,
                ApiUrlBuilder.customerUrl("v1/images/$customerId/$imageId"),
                emptyMap()
            )
            response.data
        } catch (e: Exception) {
            CustomerLogger.e("CustomerImageApi", "Error getting customer image", e)
            null
        }
    }

    override suspend fun uploadCustomerImage(request: CustomerImageUploadRequest): CustomerImageUploadResponse {
        return try {
            // For backward compatibility, delegate to multipart upload
            // Note: This requires the imageData to be available in the request
            throw Exception("Use uploadCustomerImageMultipart instead - this method is deprecated")
        } catch (e: Exception) {
            CustomerLogger.e("CustomerImageApi", "Error uploading customer image", e)
            throw e
        }
    }

    override suspend fun uploadCustomerImageMultipart(
        uid: String,
        customerId: String,
        fileName: String,
        contentType: String,
        imageData: ByteArray,
        description: String?,
        isPrimary: Boolean,
        displayOrder: Int?
    ): CustomerImageUploadResponse {
        return try {
            CustomerLogger.d("CustomerImageApi", "Starting multipart upload for customer: $customerId, file: $fileName, uid: $uid")

            val parts = listOf(
                // Add the file data using FileItem for ByteArray
                PartData.FileItem(
                    provider = { ByteReadChannel(imageData) },
                    dispose = { },
                    partHeaders = Headers.build {
                        append(HttpHeaders.ContentType, contentType)
                        append(HttpHeaders.ContentDisposition, "form-data; name=\"file\"; filename=\"$fileName\"")
                    }
                ),
                // Add form parameters matching backend expectations
                PartData.FormItem(
                    value = uid,
                    dispose = { },
                    partHeaders = Headers.build {
                        append(HttpHeaders.ContentDisposition, "form-data; name=\"uid\"")
                    }
                ),
                PartData.FormItem(
                    value = customerId,
                    dispose = { },
                    partHeaders = Headers.build {
                        append(HttpHeaders.ContentDisposition, "form-data; name=\"customerUid\"")
                    }
                ),
                PartData.FormItem(
                    value = isPrimary.toString(),
                    dispose = { },
                    partHeaders = Headers.build {
                        append(HttpHeaders.ContentDisposition, "form-data; name=\"isPrimary\"")
                    }
                )
            ).let { baseParts ->
                // Add optional parts
                val allParts = baseParts.toMutableList()
                description?.let {
                    allParts.add(
                        PartData.FormItem(
                            value = it,
                            dispose = { },
                            partHeaders = Headers.build {
                                append(HttpHeaders.ContentDisposition, "form-data; name=\"description\"")
                            }
                        )
                    )
                }
                displayOrder?.let {
                    allParts.add(
                        PartData.FormItem(
                            value = it.toString(),
                            dispose = { },
                            partHeaders = Headers.build {
                                append(HttpHeaders.ContentDisposition, "form-data; name=\"displayOrder\"")
                            }
                        )
                    )
                }
                allParts
            }

            val response: Response<CustomerImage> = postMultiPart(
                client,
                ApiUrlBuilder.customerUrl("v1/images/upload"),
                parts
            )

            CustomerLogger.d("CustomerImageApi", "Multipart upload completed for: $fileName")
            val customerImage = response.data ?: throw Exception("Upload response is null")

            // Convert CustomerImage to CustomerImageUploadResponse format
            CustomerImageUploadResponse(
                uid = customerImage.uid,
                uploadUrl = "", // Not needed for direct upload
                imageUrl = customerImage.imageUrl ?: "",
                thumbnailUrl = customerImage.thumbnailUrl ?: "",
                expiresAt = null
            )
        } catch (e: Exception) {
            CustomerLogger.e("CustomerImageApi", "Error uploading customer image via multipart", e)
            throw e
        }
    }

    override suspend fun uploadImageFile(uploadUrl: String, imageData: ByteArray, contentType: String): Boolean {
        return try {
            val response: HttpResponse = client.put(uploadUrl) {
                contentType(ContentType.parse(contentType))
                setBody(imageData)
            }
            response.status.value in 200..299
        } catch (e: Exception) {
            CustomerLogger.e("CustomerImageApi", "Error uploading image file to presigned URL", e)
            false
        }
    }

    override suspend fun updateCustomerImage(customerId: String, imageId: String, request: CustomerImageUpdateRequest): CustomerImage {
        return try {
            val response: Response<CustomerImage> = put(
                client,
                ApiUrlBuilder.customerUrl("v1/images/$customerId/$imageId"),
                request
            )
            response.data ?: throw Exception("Update response is null")
        } catch (e: Exception) {
            CustomerLogger.e("CustomerImageApi", "Error updating customer image", e)
            throw e
        }
    }

    override suspend fun deleteCustomerImage(customerId: String, imageId: String) {
        try {
            delete<Unit>(
                client,
                ApiUrlBuilder.customerUrl("v1/images/$customerId/$imageId")
            )
        } catch (e: Exception) {
            CustomerLogger.e("CustomerImageApi", "Error deleting customer image", e)
            throw e
        }
    }

    override suspend fun setPrimaryImage(customerId: String, imageId: String): CustomerImage {
        return try {
            val response: Response<CustomerImage> = put(
                client,
                ApiUrlBuilder.customerUrl("v1/images/$customerId/$imageId/primary"),
                emptyMap<String, Any>()
            )
            response.data ?: throw Exception("Set primary response is null")
        } catch (e: Exception) {
            CustomerLogger.e("CustomerImageApi", "Error setting primary image", e)
            throw e
        }
    }

    override suspend fun getThumbnail(customerId: String, imageId: String, size: String): ThumbnailResponse {
        return try {
            val response: Response<ThumbnailResponse> = get(
                client,
                ApiUrlBuilder.customerUrl("v1/images/$customerId/$imageId/thumbnail"),
                mapOf("size" to size)
            )
            response.data ?: throw Exception("Thumbnail response is null")
        } catch (e: Exception) {
            CustomerLogger.e("CustomerImageApi", "Error getting thumbnail", e)
            throw e
        }
    }

    override suspend fun getThumbnails(customerId: String, imageId: String, sizes: List<String>): List<ThumbnailResponse> {
        return try {
            val response: Response<List<ThumbnailResponse>> = get(
                client,
                ApiUrlBuilder.customerUrl("v1/images/$customerId/$imageId/thumbnails"),
                mapOf("sizes" to sizes.joinToString(","))
            )
            response.data ?: emptyList()
        } catch (e: Exception) {
            CustomerLogger.e("CustomerImageApi", "Error getting thumbnails", e)
            emptyList()
        }
    }

    override suspend fun bulkOperation(request: CustomerImageBulkRequest): List<CustomerImage> {
        return try {
            val response: Response<List<CustomerImage>> = post(
                client,
                ApiUrlBuilder.customerUrl("v1/images/bulk"),
                request
            )
            response.data ?: emptyList()
        } catch (e: Exception) {
            CustomerLogger.e("CustomerImageApi", "Error performing bulk operation", e)
            emptyList()
        }
    }

    override suspend fun deleteMultipleImages(imageIds: List<String>) {
        try {
            val request = CustomerImageBulkRequest(
                imageIds = imageIds,
                action = "delete"
            )
            bulkOperation(request)
        } catch (e: Exception) {
            CustomerLogger.e("CustomerImageApi", "Error deleting multiple images", e)
            throw e
        }
    }

    override suspend fun reorderImages(imageIds: List<String>, sortOrders: List<Int>): List<CustomerImage> {
        return try {
            val request = CustomerImageBulkRequest(
                imageIds = imageIds,
                action = "reorder",
                data = imageIds.zip(sortOrders).associate { it.first to it.second.toString() }
            )
            bulkOperation(request)
        } catch (e: Exception) {
            CustomerLogger.e("CustomerImageApi", "Error reordering images", e)
            emptyList()
        }
    }

    override suspend fun getPrimaryImage(customerId: String): CustomerImage? {
        return try {
            val response: Response<CustomerImage> = get(
                client,
                ApiUrlBuilder.customerUrl("v1/$customerId/images/primary"),
                emptyMap()
            )
            response.data
        } catch (e: Exception) {
            CustomerLogger.e("CustomerImageApi", "Error getting primary image", e)
            null
        }
    }

    override suspend fun getCustomerImagesCount(customerId: String): Int {
        return try {
            val response: Response<Int> = get(
                client,
                ApiUrlBuilder.customerUrl("v1/$customerId/images/count"),
                emptyMap()
            )
            response.data ?: 0
        } catch (e: Exception) {
            CustomerLogger.e("CustomerImageApi", "Error getting customer images count", e)
            0
        }
    }

    override suspend fun deleteAllCustomerImages(customerId: String) {
        try {
            delete<Unit>(
                client,
                ApiUrlBuilder.customerUrl("v1/$customerId/images")
            )
        } catch (e: Exception) {
            CustomerLogger.e("CustomerImageApi", "Error deleting all customer images", e)
            throw e
        }
    }

    override suspend fun getDownloadUrl(customerId: String, imageId: String): String {
        return try {
            val response: Response<Map<String, String>> = get(
                client,
                ApiUrlBuilder.customerUrl("v1/images/$customerId/$imageId/download"),
                emptyMap()
            )
            response.data?.get("download_url") ?: throw Exception("Download URL not found")
        } catch (e: Exception) {
            CustomerLogger.e("CustomerImageApi", "Error getting download URL", e)
            throw e
        }
    }

    override suspend fun downloadImage(imageUrl: String): ByteArray {
        return try {
            // Build complete URL if relative path provided
            val fullUrl = ApiUrlBuilder.buildCompleteUrl(imageUrl)

            CustomerLogger.d("CustomerImageApi", "Downloading image from URL: $fullUrl")
            val response = client.get(fullUrl)
            if (response.status.value in 200..299) {
                val imageData = response.body<ByteArray>()
                CustomerLogger.d("CustomerImageApi", "Successfully downloaded image, size: ${imageData.size} bytes")
                imageData
            } else {
                CustomerLogger.w("CustomerImageApi", "Failed to download image, status: ${response.status}")
                throw Exception("Failed to download image: HTTP ${response.status.value}")
            }
        } catch (e: Exception) {
            CustomerLogger.e("CustomerImageApi", "Error downloading image from $imageUrl", e)
            throw e
        }
    }

    override suspend fun downloadThumbnail(thumbnailUrl: String): ByteArray {
        return try {
            // Build complete URL if relative path provided
            val fullUrl = ApiUrlBuilder.buildCompleteUrl(thumbnailUrl)

            CustomerLogger.d("CustomerImageApi", "Downloading thumbnail from URL: $fullUrl")
            val response = client.get(fullUrl)
            if (response.status.value in 200..299) {
                val thumbnailData = response.body<ByteArray>()
                CustomerLogger.d("CustomerImageApi", "Successfully downloaded thumbnail, size: ${thumbnailData.size} bytes")
                thumbnailData
            } else {
                CustomerLogger.w("CustomerImageApi", "Failed to download thumbnail, status: ${response.status}")
                throw Exception("Failed to download thumbnail: HTTP ${response.status.value}")
            }
        } catch (e: Exception) {
            CustomerLogger.e("CustomerImageApi", "Error downloading thumbnail from $thumbnailUrl", e)
            throw e
        }
    }
}