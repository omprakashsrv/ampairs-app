package com.ampairs.update.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.model.Response
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.get
import io.ktor.client.statement.readBytes

/**
 * Implementation of UpdateApi using Ktor HTTP client
 */
class UpdateApiImpl(
    engine: HttpClientEngine,
    private val tokenRepository: TokenRepository
) : UpdateApi {

    private val client = httpClient(engine, tokenRepository)

    override suspend fun checkForUpdates(
        platform: String,
        currentVersion: String,
        versionCode: Int,
    ): Response<UpdateCheckResponse> {
        val params = mapOf(
            "platform" to platform,
            "currentVersion" to currentVersion,
            "versionCode" to versionCode
        )
        return get(client, ApiUrlBuilder.appUpdateUrl("check"), params)
    }

    override suspend fun downloadUpdate(downloadUrl: String): ByteArray {
        // Direct download - will be wrapped by UpdateDownloader for progress tracking
        return client.get(downloadUrl).readBytes()
    }
}
