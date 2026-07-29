package com.ampairs.analytics.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTOs for the backend deep-history dashboard reads (feature 022, T030): the KPI + trend endpoints
 * under `/api/analytics/v1/dashboard/*`. Used only for the portion of a requested period that falls
 * BEFORE the device's local sync window (T030a). Fields mirror the backend `AnalyticsResponses.kt`
 * (global SNAKE_CASE); `BigDecimal` serializes as a JSON number → `Double`, `LocalDate`/`Instant` as
 * ISO strings. All default so a partial payload never fails deserialization.
 */
@Serializable
data class KpiValueDto(
    @SerialName("metric_id") val metricId: String = "",
    val unit: String = "",
    val value: Double = 0.0,
)

@Serializable
data class KpiResponseDto(
    @SerialName("metric_group") val metricGroup: String = "",
    val period: String = "",
    @SerialName("from_date") val fromDate: String = "",
    @SerialName("to_date") val toDate: String = "",
    @SerialName("currency_code") val currencyCode: String = "",
    val values: List<KpiValueDto> = emptyList(),
    @SerialName("computed_from") val computedFrom: String? = null,
)

@Serializable
data class TrendPointDto(
    @SerialName("bucket_start") val bucketStart: String = "",
    @SerialName("bucket_label") val bucketLabel: String = "",
    val value: Double = 0.0,
)
