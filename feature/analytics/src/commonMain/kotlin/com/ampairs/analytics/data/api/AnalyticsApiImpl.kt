package com.ampairs.analytics.data.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.di.AppScope
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.model.PageResponse
import com.ampairs.common.model.Response
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.engine.HttpClientEngine

/** Ktor implementation of [AnalyticsApi] against `/api/analytics/v1/forecasts/sync`. */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class AnalyticsApiImpl(
    engine: HttpClientEngine,
    tokenRepository: TokenRepository,
) : AnalyticsApi {

    private val client = httpClient(engine, tokenRepository)

    override suspend fun getForecastsSync(
        lastSync: String,
        page: Int,
        size: Int,
        sortBy: String,
        sortDir: String,
    ): PageResponse<DemandForecastResponse> {
        val response: Response<PageResponse<DemandForecastResponse>> = get(
            client,
            ApiUrlBuilder.analyticsUrl("v1/forecasts/sync"),
            syncParams(lastSync, page, size, sortBy, sortDir),
        )
        if (response.error != null) throw Exception(response.error?.message ?: "Network error")
        return response.data ?: emptyPage(page, size)
    }

    override suspend fun getKpis(
        fromDate: String,
        toDate: String,
        period: String,
        metricGroup: String,
    ): KpiResponseDto {
        val response: Response<KpiResponseDto> = get(
            client,
            ApiUrlBuilder.analyticsUrl("v1/dashboard/kpis"),
            mapOf(
                "from_date" to fromDate,
                "to_date" to toDate,
                "period" to period,
                "metric_group" to metricGroup,
            ),
        )
        if (response.error != null) throw Exception(response.error?.message ?: "Network error")
        return response.data ?: KpiResponseDto()
    }

    override suspend fun getTrend(
        fromDate: String,
        toDate: String,
        period: String,
        metricId: String,
    ): List<TrendPointDto> {
        val response: Response<List<TrendPointDto>> = get(
            client,
            ApiUrlBuilder.analyticsUrl("v1/dashboard/trend"),
            mapOf(
                "from_date" to fromDate,
                "to_date" to toDate,
                "period" to period,
                "metric_id" to metricId,
            ),
        )
        if (response.error != null) throw Exception(response.error?.message ?: "Network error")
        return response.data ?: emptyList()
    }

    private fun syncParams(lastSync: String, page: Int, size: Int, sortBy: String, sortDir: String): Map<String, Any> {
        val params = mutableMapOf<String, Any>(
            "page" to page,
            "size" to size,
            "sort_by" to sortBy,
            "sort_dir" to sortDir,
        )
        if (lastSync.isNotBlank()) params["last_sync"] = lastSync
        return params
    }

    private fun <T> emptyPage(page: Int, size: Int) = PageResponse<T>(
        content = emptyList(),
        pageNumber = page,
        pageSize = size,
        totalPages = 0,
        totalElements = 0L,
        hasNext = false,
        hasPrevious = false,
        first = true,
        last = true,
    )
}
