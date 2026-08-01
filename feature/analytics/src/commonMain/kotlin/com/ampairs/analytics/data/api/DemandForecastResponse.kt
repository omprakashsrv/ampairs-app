package com.ampairs.analytics.data.api

import com.ampairs.analytics.data.db.entity.DemandForecastEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Server DTO for a demand forecast, pulled read-only from `/api/analytics/v1/forecasts/sync`.
 * Fields mirror the backend `DemandForecastResponse` (global SNAKE_CASE); all default so a partial
 * payload never fails deserialization.
 */
@Serializable
data class DemandForecastResponse(
    val uid: String = "",
    @SerialName("product_id") val productId: String = "",
    @SerialName("period_start") val periodStart: String = "",
    val horizon: Int = 0,
    @SerialName("mean_qty") val meanQty: Double = 0.0,
    @SerialName("std_dev_qty") val stdDevQty: Double = 0.0,
    val method: String = "",
    val confidence: String = "",
    @SerialName("generated_at") val generatedAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    val active: Boolean = true,
)

fun DemandForecastResponse.toEntity(): DemandForecastEntity = DemandForecastEntity(
    uid = uid,
    productId = productId,
    periodStart = periodStart,
    horizon = horizon,
    meanQty = meanQty,
    stdDevQty = stdDevQty,
    method = method,
    confidence = confidence,
    generatedAt = generatedAt,
    updatedAt = updatedAt,
)
