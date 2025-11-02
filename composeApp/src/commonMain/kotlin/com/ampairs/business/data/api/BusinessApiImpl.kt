package com.ampairs.business.data.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.business.domain.*
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.model.Response
import com.ampairs.common.post
import com.ampairs.common.put
import io.ktor.client.engine.HttpClientEngine

/**
 * Implementation of BusinessApi using Ktor HTTP client (simplified).
 * All business management operations use unified endpoints.
 */
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
