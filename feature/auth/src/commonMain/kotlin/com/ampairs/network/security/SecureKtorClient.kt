
package com.ampairs.network.security

import com.ampairs.common.ApiUrlBuilder
import com.ampairs.auth.api.TokenRepository
import com.ampairs.auth.api.model.RefreshToken
import com.ampairs.auth.api.model.Token
import com.ampairs.auth.domain.asRefreshTokens
import com.ampairs.common.UnauthenticatedHandler
import com.ampairs.common.model.Response
import com.ampairs.common.security.SecurityException
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

/**
 * Secure HTTP client factory that integrates certificate pinning
 */
class SecureKtorClientFactory(
    private val certificatePinningService: CertificatePinningService,
    private val appUpdateEnforcer: AppUpdateEnforcer
) {

    /**
     * Create an HttpClient with certificate pinning and security checks
     */
    fun createSecureHttpClient(
        engine: HttpClientEngine,
        tokenRepository: TokenRepository
    ): HttpClient {
        // Check certificate status before creating client
        runBlocking {
            val updateStatus = certificatePinningService.checkAppUpdateRequired()

            when (updateStatus) {
                is AppUpdateStatus.Required -> {
                    appUpdateEnforcer.showUpdateDialog(updateStatus)
                    throw SecurityException("App update required for security reasons") // TODO: Use string resource
                }
                is AppUpdateStatus.Recommended -> {
                    appUpdateEnforcer.showUpdateDialog(updateStatus)
                }
                AppUpdateStatus.NotRequired -> {
                    // Continue with client creation
                }
            }
        }

        return HttpClient(engine) {
            expectSuccess = true

            defaultRequest {
//                if (tokenRepository.getWorkspaceId().isNotEmpty()) {
//                    header("X-Company", tokenRepository.getWorkspaceId())
//                }
                contentType(ContentType.Application.Json)
            }

            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                    }
                )
            }

            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        println("SecureHttpClient: $message")
                    }
                }
                level = LogLevel.INFO
            }

            install(HttpTimeout) {
                val timeout = 30000L
                connectTimeoutMillis = timeout
                requestTimeoutMillis = timeout
                socketTimeoutMillis = timeout
            }

            install(Auth) {
                bearer {
                    loadTokens {
                        BearerTokens(
                            tokenRepository.getAccessToken() ?: "",
                            tokenRepository.getRefreshToken() ?: ""
                        )
                    }
                    refreshTokens {
                        runBlocking {
                            if (!appUpdateEnforcer.shouldAllowNetworkRequests()) {
                                throw SecurityException("Network requests blocked due to security policy") // TODO: Use string resource
                            }
                        }

                        try {
                            val tokenResponse: Response<Token> = client.post {
                                url(ApiUrlBuilder.authUrl("auth/v1/refresh_token"))
                                contentType(ContentType.Application.Json)
                                setBody(
                                    RefreshToken(
                                        oldTokens?.refreshToken ?: tokenRepository.getRefreshToken()
                                    )
                                )
                                markAsRefreshTokenRequest()
                            }.body()

                            val refreshTokens = tokenResponse.data?.asRefreshTokens()
                            refreshTokens?.let {
                                tokenRepository.updateToken(it.accessToken, it.refreshToken)
                            }
                            refreshTokens
                        } catch (e: Exception) {
                            UnauthenticatedHandler.onUnauthenticated()
                            null
                        }
                    }
                }
            }
        }
    }
}

/**
 * Extension function to check security before making requests
 */
suspend fun HttpClient.secureRequest(
    appUpdateEnforcer: AppUpdateEnforcer,
    block: suspend HttpClient.() -> Unit
) {
    if (!appUpdateEnforcer.shouldAllowNetworkRequests()) {
        throw SecurityException("Network requests blocked due to security policy") // TODO: Use string resource
    }

    block()
}
