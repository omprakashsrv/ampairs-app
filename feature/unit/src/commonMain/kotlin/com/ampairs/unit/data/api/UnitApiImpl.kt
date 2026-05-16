package com.ampairs.unit.data.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.delete
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.model.PageResponse
import com.ampairs.common.model.Response
import com.ampairs.common.post
import com.ampairs.unit.domain.model.Unit
import io.ktor.client.engine.HttpClientEngine

/**
 * Ktor-based implementation of UnitApi
 *
 * Uses ApiUrlBuilder for URL construction and httpClient helper for authenticated requests
 */
class UnitApiImpl(
    engine: HttpClientEngine,
    tokenRepository: TokenRepository
) : UnitApi {

    private val client = httpClient(engine, tokenRepository)

    override suspend fun getUnits(
        page: Int,
        size: Int,
        lastSyncTime: String?,
        sortBy: String,
        sortDirection: String
    ): Response<PageResponse<Unit>> {
        val params = mutableMapOf(
            "page" to page.toString(),
            "size" to size.toString(),
            "sortBy" to sortBy,
            "sortDirection" to sortDirection
        )

        // Add lastSyncTime for incremental sync if provided
        lastSyncTime?.let { params["lastSyncTime"] = it }

        return get(
            client,
            ApiUrlBuilder.productUrl("v1/units"),
            params
        )
    }

    override suspend fun searchUnits(
        query: String,
        page: Int,
        size: Int
    ): Response<PageResponse<Unit>> {
        val params = mapOf(
            "query" to query,
            "page" to page.toString(),
            "size" to size.toString()
        )

        return get(
            client,
            ApiUrlBuilder.productUrl("v1/units/search"),
            params
        )
    }

    override suspend fun getUnitById(id: String): Response<Unit> {
        return get(
            client,
            ApiUrlBuilder.productUrl("v1/units/$id")
        )
    }

    override suspend fun createUnit(unit: Unit): Response<Unit> {
        return post(
            client,
            ApiUrlBuilder.productUrl("v1/units"),
            unit
        )
    }

    override suspend fun updateUnit(id: String, unit: Unit): Response<Unit> {
        return post(
            client,
            ApiUrlBuilder.productUrl("v1/units/$id"),
            unit
        )
    }

    override suspend fun deleteUnit(id: String): Response<kotlin.Unit> {
        return delete(
            client,
            ApiUrlBuilder.productUrl("v1/units/$id")
        )
    }
}
