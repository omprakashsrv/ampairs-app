package com.ampairs.business.data.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.business.domain.*
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.model.Response
import com.ampairs.common.post
import com.ampairs.common.postMultiPart
import com.ampairs.common.put
import com.ampairs.common.delete
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.engine.HttpClientEngine
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.content.PartData
import io.ktor.utils.io.ByteReadChannel

/**
 * Implementation of BusinessApi using Ktor HTTP client (simplified).
 * All business management operations use unified endpoints.
 */
@Inject @SingleIn(AppScope::class) @ContributesBinding(AppScope::class)
class BusinessApiImpl(
    engine: HttpClientEngine,
    tokenRepository: TokenRepository
) : BusinessApi {

    private val client = httpClient(engine, tokenRepository)

    // ==================== Main Business Endpoints ====================

    override suspend fun getBusiness(): Result<Business> {
        return try {
            val response: Response<Business> = get(
                client,
                ApiUrlBuilder.businessUrl()
            )
            handleResponse(response, "Failed to fetch business")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateBusiness(request: BusinessPayload): Result<Business> {
        return try {
            val response: Response<Business> = put(
                client,
                ApiUrlBuilder.businessUrl(),
                request
            )
            handleResponse(response, "Failed to update business")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createBusiness(request: BusinessPayload): Result<Business> {
        return try {
            val response: Response<Business> = post(
                client,
                ApiUrlBuilder.businessUrl(),
                request
            )
            handleResponse(response, "Failed to create business profile")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== Dashboard Overview ====================

    override suspend fun getBusinessOverview(): Result<BusinessOverview> {
        return try {
            val response: Response<BusinessOverview> = get(
                client,
                ApiUrlBuilder.businessUrl("overview")
            )
            handleResponse(response, "Failed to fetch business overview")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== Utility ====================

    override suspend fun checkBusinessExists(): Result<Boolean> {
        return try {
            val response: Response<Map<String, Boolean>> = get(
                client,
                ApiUrlBuilder.businessUrl("exists")
            )
            val data = response.data
            val error = response.error

            when {
                data != null && error == null -> Result.success(data["exists"] ?: false)
                error != null -> Result.failure(Exception(error.message))
                else -> Result.failure(IllegalStateException("Failed to check business existence"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== Logo Endpoints ====================

    override suspend fun uploadLogo(
        imageData: ByteArray,
        fileName: String,
        contentType: String
    ): Result<Business> {
        return try {
            val parts = listOf(
                PartData.FileItem(
                    provider = { ByteReadChannel(imageData) },
                    dispose = {},
                    partHeaders = Headers.build {
                        append(HttpHeaders.ContentType, contentType)
                        append(HttpHeaders.ContentDisposition, "form-data; name=\"file\"; filename=\"$fileName\"")
                    }
                )
            )
            val response: Response<Business> = postMultiPart(
                client,
                ApiUrlBuilder.businessUrl("logo"),
                parts
            )
            handleResponse(response, "Failed to upload logo")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteLogo(): Result<Business> {
        return try {
            val response: Response<Business> = delete(
                client,
                ApiUrlBuilder.businessUrl("logo")
            )
            handleResponse(response, "Failed to delete logo")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== Gallery Image Endpoints ====================

    override suspend fun uploadImage(
        imageData: ByteArray,
        fileName: String,
        contentType: String,
        imageType: BusinessImageType,
        title: String?,
        description: String?,
        altText: String?,
        isPrimary: Boolean
    ): Result<BusinessImage> {
        return try {
            val parts = mutableListOf<PartData>(
                PartData.FileItem(
                    provider = { ByteReadChannel(imageData) },
                    dispose = {},
                    partHeaders = Headers.build {
                        append(HttpHeaders.ContentType, contentType)
                        append(HttpHeaders.ContentDisposition, "form-data; name=\"file\"; filename=\"$fileName\"")
                    }
                ),
                PartData.FormItem(
                    value = imageType.name,
                    dispose = {},
                    partHeaders = Headers.build {
                        append(HttpHeaders.ContentDisposition, "form-data; name=\"image_type\"")
                    }
                ),
                PartData.FormItem(
                    value = isPrimary.toString(),
                    dispose = {},
                    partHeaders = Headers.build {
                        append(HttpHeaders.ContentDisposition, "form-data; name=\"is_primary\"")
                    }
                )
            )

            title?.let {
                parts.add(PartData.FormItem(
                    value = it,
                    dispose = {},
                    partHeaders = Headers.build {
                        append(HttpHeaders.ContentDisposition, "form-data; name=\"title\"")
                    }
                ))
            }

            description?.let {
                parts.add(PartData.FormItem(
                    value = it,
                    dispose = {},
                    partHeaders = Headers.build {
                        append(HttpHeaders.ContentDisposition, "form-data; name=\"description\"")
                    }
                ))
            }

            altText?.let {
                parts.add(PartData.FormItem(
                    value = it,
                    dispose = {},
                    partHeaders = Headers.build {
                        append(HttpHeaders.ContentDisposition, "form-data; name=\"alt_text\"")
                    }
                ))
            }

            val response: Response<BusinessImage> = postMultiPart(
                client,
                ApiUrlBuilder.businessUrl("images"),
                parts
            )
            handleResponse(response, "Failed to upload image")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getImages(imageType: BusinessImageType?): Result<List<BusinessImage>> {
        return try {
            val params = imageType?.let { mapOf("image_type" to it.name) } ?: emptyMap()
            val response: Response<List<BusinessImage>> = get(
                client,
                ApiUrlBuilder.businessUrl("images"),
                params
            )
            handleResponse(response, "Failed to fetch images")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getImageDetails(imageUid: String): Result<BusinessImage> {
        return try {
            val response: Response<BusinessImage> = get(
                client,
                ApiUrlBuilder.businessUrl("images/$imageUid")
            )
            handleResponse(response, "Failed to fetch image details")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateImage(imageUid: String, request: UpdateBusinessImageRequest): Result<BusinessImage> {
        return try {
            val response: Response<BusinessImage> = put(
                client,
                ApiUrlBuilder.businessUrl("images/$imageUid"),
                request
            )
            handleResponse(response, "Failed to update image")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setImageAsPrimary(imageUid: String): Result<BusinessImage> {
        return try {
            val response: Response<BusinessImage> = post(
                client,
                ApiUrlBuilder.businessUrl("images/$imageUid/set-primary"),
                null
            )
            handleResponse(response, "Failed to set image as primary")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun reorderImages(imageUids: List<String>): Result<Unit> {
        return try {
            val response: Response<String> = post(
                client,
                ApiUrlBuilder.businessUrl("images/reorder"),
                ReorderImagesRequest(imageUids)
            )
            response.error?.let { error ->
                return Result.failure(Exception(error.message))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteImage(imageUid: String): Result<Unit> {
        return try {
            val response: Response<String> = delete(
                client,
                ApiUrlBuilder.businessUrl("images/$imageUid")
            )
            response.error?.let { error ->
                return Result.failure(Exception(error.message))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== Response Handlers ====================

    private fun <T> handleResponse(response: Response<T>, errorMessage: String): Result<T> {
        val data = response.data
        val error = response.error

        return when {
            data != null && error == null -> Result.success(data)
            error != null -> Result.failure(Exception(error.message))
            else -> Result.failure(IllegalStateException(errorMessage))
        }
    }
}
