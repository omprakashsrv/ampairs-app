package com.ampairs.unit.data.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.model.PageResponse
import com.ampairs.common.model.Response
import com.ampairs.common.post
import com.ampairs.unit.domain.model.Unit
import io.ktor.client.engine.HttpClientEngine

/**
 * Ktor-based implementation of [UnitApi].
 *
 * Uses ApiUrlBuilder for URL construction and httpClient helper for authenticated requests.
 * Mirrors the canonical CustomerApiImpl bulk `/sync` style.
 */
@Inject @SingleIn(AppScope::class) @ContributesBinding(AppScope::class)
class UnitApiImpl(
    engine: HttpClientEngine,
    tokenRepository: TokenRepository
) : UnitApi {

    private val client = httpClient(engine, tokenRepository)

    override suspend fun getUnitsSync(
        lastSync: String,
        page: Int,
        size: Int,
        sortBy: String,
        sortDir: String,
    ): PageResponse<Unit> {
        val params = mutableMapOf(
            "page" to page,
            "size" to size,
            "sort_by" to sortBy,
            "sort_dir" to sortDir,
        )
        if (lastSync.isNotBlank()) {
            params["last_sync"] = lastSync
        }

        val response: Response<PageResponse<Unit>> = get(
            client,
            ApiUrlBuilder.unitUrl("v1/units/sync"),
            params
        )
        if (response.error != null) throw Exception(response.error?.message ?: "Network error")
        return response.data ?: PageResponse(
            content = emptyList(),
            pageNumber = page,
            pageSize = size,
            totalPages = 0,
            totalElements = 0L,
            hasNext = false,
            hasPrevious = false,
            first = true,
            last = true
        )
    }

    override suspend fun bulkUpdateUnits(units: List<Unit>): List<Unit> {
        val response: Response<List<Unit>> = post(
            client,
            ApiUrlBuilder.unitUrl("v1/units/sync"),
            units
        )
        return response.data ?: throw Exception("Failed to bulk update units")
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
            ApiUrlBuilder.unitUrl("v1/units/search"),
            params
        )
    }

    override suspend fun getUnitById(id: String): Response<Unit> {
        return get(
            client,
            ApiUrlBuilder.unitUrl("v1/units/$id")
        )
    }
}
