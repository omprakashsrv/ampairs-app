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
}
