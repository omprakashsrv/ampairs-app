package com.ampairs.file.data.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.delete
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.model.Response
import com.ampairs.common.postMultiPart
import com.ampairs.common.put
import com.ampairs.common.di.AppScope
import com.ampairs.file.api.FileEntityType
import com.ampairs.file.data.api.model.EntityImageApiModel
import com.ampairs.file.data.api.model.EntityImageListApiModel
import com.ampairs.file.util.FileLogger
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
class FileApiServiceImpl(
    engine: HttpClientEngine,
    tokenRepository: TokenRepository,
) : FileApiService {

    private val client = httpClient(engine, tokenRepository)

    companion object {
        private const val TAG = "FileApiService"
        private const val UPLOAD_TIMEOUT_MS = 120_000L
    }

    override suspend fun upload(
        entityType: FileEntityType,
        entityUid: String,
        localUid: String,
        fileName: String,
        contentType: String,
        imageData: ByteArray,
        isPrimary: Boolean,
    ): Result<EntityImageApiModel> = runCatching {
        FileLogger.d(TAG, "Uploading image: entity=${entityType.backendValue}/$entityUid, file=$fileName, uid=$localUid")
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
                value = localUid,
                dispose = {},
                partHeaders = Headers.build {
                    append(HttpHeaders.ContentDisposition, "form-data; name=\"uid\"")
                }
            ),
            PartData.FormItem(
                value = isPrimary.toString(),
                dispose = {},
                partHeaders = Headers.build {
                    append(HttpHeaders.ContentDisposition, "form-data; name=\"is_primary\"")
                }
            ),
        )
        val response: Response<EntityImageApiModel> = postMultiPart(
            client,
            ApiUrlBuilder.fileUrl("v1/images/${entityType.backendValue}/$entityUid"),
            parts,
            requestTimeoutMillis = UPLOAD_TIMEOUT_MS,
        )
        response.data ?: throw Exception("Upload response is null for $fileName")
    }

    override suspend fun getImages(
        entityType: FileEntityType,
        entityUid: String,
        lastSync: String?,
    ): Result<EntityImageListApiModel> = runCatching {
        val params = mutableMapOf<String, Any>("entity_uid" to entityUid)
        if (!lastSync.isNullOrBlank()) params["last_sync"] = lastSync
        val response: Response<EntityImageListApiModel> = get(
            client,
            ApiUrlBuilder.fileUrl("v1/images/${entityType.backendValue}/$entityUid"),
            params,
        )
        response.data ?: EntityImageListApiModel()
    }

    override suspend fun deleteImage(imageUid: String): Result<Unit> = runCatching {
        delete<Unit>(client, ApiUrlBuilder.fileUrl("v1/images/$imageUid"))
    }

    override suspend fun setPrimaryImage(imageUid: String): Result<EntityImageApiModel> = runCatching {
        val response: Response<EntityImageApiModel> = put(
            client,
            ApiUrlBuilder.fileUrl("v1/images/$imageUid/primary"),
            emptyMap<String, Any>(),
        )
        response.data ?: throw Exception("Set primary response is null for $imageUid")
    }
}
