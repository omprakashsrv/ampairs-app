package com.ampairs.analytics.data.api

import com.ampairs.common.model.PageResponse

/** Analytics REST surface. Forecasts are server-generated and pulled read-only (no push). */
interface AnalyticsApi {

    suspend fun getForecastsSync(
        lastSync: String,
        page: Int = 0,
        size: Int = 100,
        sortBy: String = "updatedAt",
        sortDir: String = "ASC",
    ): PageResponse<DemandForecastResponse>

    /**
     * Deep-history KPI totals for a `[fromDate, toDate]` (inclusive ISO dates) date range — used only
     * for the slice of a period that predates the device's local sync window (T030/T030a).
     */
    suspend fun getKpis(fromDate: String, toDate: String, period: String, metricGroup: String): KpiResponseDto

    /** Deep-history daily/period sales trend for a `[fromDate, toDate]` range (T030). */
    suspend fun getTrend(fromDate: String, toDate: String, period: String, metricId: String): List<TrendPointDto>
}
