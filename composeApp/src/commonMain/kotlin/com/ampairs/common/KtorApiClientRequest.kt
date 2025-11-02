package com.ampairs.common

import com.ampairs.common.ApiUrlBuilder
import com.ampairs.auth.api.model.RefreshToken
import com.ampairs.auth.api.model.Token
import com.ampairs.auth.domain.asRefreshTokens
import com.ampairs.network.model.ErrorResponse
import com.ampairs.common.model.Response
import com.ampairs.common.model.Error
import com.ampairs.network.model.toResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * POST request with automatic token refresh handling
 */
suspend inline fun <reified T> post(client: HttpClient, url: String, body: Any?): T {
    return try {
        val tokenRepository = getTokenRepository(client)
        var response = client.post(url) {
            if (body != null) {
                setBody(body)
            }
        }
    
    // Handle 401 and attempt token refresh
    if (response.status == HttpStatusCode.Unauthorized) {
        println("🔄 401 Unauthorized - attempting token refresh")
        
        val refreshed = refreshTokens(tokenRepository)
        if (refreshed) {
            println("✅ Token refreshed - retrying original request")
            // Retry with refreshed token
            response = client.post(url) {
                if (body != null) {
                    setBody(body)
                }
                // Add the new token manually
                val newAccessToken = tokenRepository.getAccessToken()
                if (!newAccessToken.isNullOrEmpty()) {
                    header("Authorization", "Bearer $newAccessToken")
                }
            }
        } else {
            println("❌ Token refresh failed")
        }
        }

        handleResponse<T>(response)
    } catch (e: NetworkException) {
        println("❌ Network request failed: ${e.message}")
        createNetworkErrorResponse<T>(e)
    } catch (e: Exception) {
        println("❌ Unexpected error: ${e.message}")
        createNetworkErrorResponse<T>(e)
    }
}

/**
 * PUT request with automatic token refresh handling
 */
suspend inline fun <reified T> put(client: HttpClient, url: String, body: Any?): T {
    val tokenRepository = getTokenRepository(client)
    var response = client.put(url) {
        if (body != null) {
            setBody(body)
        }
    }
    
    // Handle 401 and attempt token refresh
    if (response.status == HttpStatusCode.Unauthorized) {
        println("🔄 401 Unauthorized - attempting token refresh")
        
        val refreshed = refreshTokens(tokenRepository)
        if (refreshed) {
            println("✅ Token refreshed - retrying original request")
            // Retry with refreshed token
            response = client.put(url) {
                if (body != null) {
                    setBody(body)
                }
                // Add the new token manually
                val newAccessToken = tokenRepository.getAccessToken()
                if (!newAccessToken.isNullOrEmpty()) {
                    header("Authorization", "Bearer $newAccessToken")
                }
            }
        } else {
            println("❌ Token refresh failed")
        }
    }
    
    return handleResponse<T>(response)
}

/**
 * DELETE request with automatic token refresh handling
 */
suspend inline fun <reified T> delete(client: HttpClient, url: String): T {
    return delete(client, url, null)
}

/**
 * DELETE request with parameters and automatic token refresh handling
 */
suspend inline fun <reified T> delete(
    client: HttpClient,
    url: String,
    parameters: Map<String, Any>?
): T {
    val tokenRepository = getTokenRepository(client)
    var response = client.delete(url) {
        parameters?.forEach { (key, value) ->
            parameter(key, value)
        }
    }
    
    // Handle 401 and attempt token refresh
    if (response.status == HttpStatusCode.Unauthorized) {
        println("🔄 401 Unauthorized - attempting token refresh")
        
        val refreshed = refreshTokens(tokenRepository)
        if (refreshed) {
            println("✅ Token refreshed - retrying original request")
            // Retry with refreshed token
            response = client.delete(url) {
                parameters?.forEach { (key, value) ->
                    parameter(key, value)
                }
                // Add the new token manually
                val newAccessToken = tokenRepository.getAccessToken()
                if (!newAccessToken.isNullOrEmpty()) {
                    header("Authorization", "Bearer $newAccessToken")
                }
            }
        } else {
            println("❌ Token refresh failed")
        }
    }
    
    return handleResponse<T>(response)
}

/**
 * GET request with automatic token refresh handling
 */
suspend inline fun <reified T> get(client: HttpClient, url: String): T {
    return get(client, url, null)
}

/**
 * GET request with parameters and automatic token refresh handling
 */
suspend inline fun <reified T> get(
    client: HttpClient,
    url: String,
    parameters: Map<String, Any>?
): T {
    val tokenRepository = getTokenRepository(client)
    var response = client.get(url) {
        parameters?.forEach { (key, value) ->
            parameter(key, value)
        }
    }
    
    // Handle 401 and attempt token refresh
    if (response.status == HttpStatusCode.Unauthorized) {
        println("🔄 401 Unauthorized - attempting token refresh")
        
        val refreshed = refreshTokens(tokenRepository)
        if (refreshed) {
            println("✅ Token refreshed - retrying original request")
            // Retry with refreshed token
            response = client.get(url) {
                parameters?.forEach { (key, value) ->
                    parameter(key, value)
                }
                // Add the new token manually
                val newAccessToken = tokenRepository.getAccessToken()
                if (!newAccessToken.isNullOrEmpty()) {
                    header("Authorization", "Bearer $newAccessToken")
                }
            }
        } else {
            println("❌ Token refresh failed")
        }
    }
    
    return handleResponse<T>(response)
}

/**
 * Multipart POST request with automatic token refresh handling
 */
suspend inline fun <reified T> postMultiPart(
    client: HttpClient,
    url: String,
    parts: List<PartData>
): T {
    val tokenRepository = getTokenRepository(client)
    var response = client.submitFormWithBinaryData(
        url = url,
        formData = parts
    )
    
    // Handle 401 and attempt token refresh
    if (response.status == HttpStatusCode.Unauthorized) {
        println("🔄 401 Unauthorized - attempting token refresh")
        
        val refreshed = refreshTokens(tokenRepository)
        if (refreshed) {
            println("✅ Token refreshed - retrying original request")
            // Retry with refreshed token
            response = client.submitFormWithBinaryData(
                url = url,
                formData = parts
            ) {
                // Add the new token manually
                val newAccessToken = tokenRepository.getAccessToken()
                if (!newAccessToken.isNullOrEmpty()) {
                    header("Authorization", "Bearer $newAccessToken")
                }
            }
        } else {
            println("❌ Token refresh failed")
        }
    }
    
    return handleResponse<T>(response)
}

/**
 * Handle HTTP responses - convert to API Response format
 */
suspend inline fun <reified T> handleResponse(response: HttpResponse): T {
    return if (response.status.isSuccess()) {
        response.body<T>()
    } else {
        // Convert error response to API Response format
        val errorResponse = try {
            val errorBody = response.body<ErrorResponse>()
            errorBody.toResponse()
        } catch (_: Exception) {
            Response(
                error = Error(
                    code = response.status.value.toString(),
                    message = "HTTP ${response.status.value}: ${response.status.description}"
                ),
                data = null
            )
        }
        errorResponse as T
    }
}

/**
 * Get TokenRepository from HttpClient attributes - placeholder for now
 * In a real implementation, this would extract the token repository from client configuration
 */
fun getTokenRepository(client: HttpClient): com.ampairs.auth.api.TokenRepository {
    // For now, we'll get the token repository from Koin DI
    return org.koin.mp.KoinPlatform.getKoin().get()
}

/**
 * Get DeviceService from Koin DI to retrieve device ID
 */
fun getDeviceService(): com.ampairs.common.DeviceService {
    return org.koin.mp.KoinPlatform.getKoin().get()
}

/**
 * Attempt to refresh tokens using the refresh token
 */
suspend fun refreshTokens(tokenRepository: com.ampairs.auth.api.TokenRepository): Boolean {
    return try {
        val refreshToken = tokenRepository.getRefreshToken()
        if (refreshToken.isNullOrEmpty()) {
            println("❌ No refresh token available")
            return false
        }
        
        println("🔑 Attempting to refresh tokens with refresh token: ${refreshToken.take(10)}...")
        
        // Create a simple HTTP client for the refresh request (without auth interceptors)
        val refreshClient = HttpClient {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    encodeDefaults = false
                })
            }
            expectSuccess = false // Don't throw exceptions for this client
        }
        
        // Get device ID for the refresh request
        val deviceService = getDeviceService()
        val deviceId = deviceService.getDeviceId()
        
        println("🔑 Using device ID for refresh: ${deviceId.take(10)}...")
        
        val response = refreshClient.post(ApiUrlBuilder.authUrl("auth/v1/refresh_token")) {
            contentType(ContentType.Application.Json)
            setBody(RefreshToken(refreshToken, deviceId))
        }
        
        if (response.status == HttpStatusCode.OK) {
            val tokenResponse = response.body<Response<Token>>()
            val newTokens = tokenResponse.data?.asRefreshTokens()
            
            if (newTokens != null) {
                println("✅ Token refresh successful - New Access: ${newTokens.accessToken.take(10)}..., New Refresh: ${newTokens.refreshToken?.take(10)}...")
                tokenRepository.updateToken(newTokens.accessToken, newTokens.refreshToken)
                refreshClient.close()
                return true
            }
        }

        // Log more details about the failure
        println("❌ Token refresh failed - Status: ${response.status}")
        try {
            val errorBody = response.body<String>()
            println("❌ Error response body: $errorBody")
        } catch (e: Exception) {
            println("❌ Could not read error response body: ${e.message}")
        }
        
        refreshClient.close()
        false

    } catch (e: Exception) {
        println("💥 Token refresh failed with exception: ${e.message}")
        e.printStackTrace()
        // Clear tokens on refresh failure
        tokenRepository.clearTokens()
        false
    }
}

/**
 * Create a standardized error response for network failures
 */
inline fun <reified T> createNetworkErrorResponse(exception: Exception): T {
    val errorResponse = Response(
        error = Error(
            code = "NETWORK_ERROR",
            message = when {
                exception is NetworkException -> exception.message ?: "Network connection failed"
                exception::class.simpleName == "ConnectException" -> "Unable to connect to server. Please check your network connection."
                exception::class.simpleName == "UnknownHostException" -> "Server not found. Please check your network connection."
                exception::class.simpleName == "SocketTimeoutException" -> "Request timed out. Please try again."
                else -> "Network error: ${exception.message ?: "Unknown error"}"
            }
        ),
        data = null
    )
    return errorResponse as T
}