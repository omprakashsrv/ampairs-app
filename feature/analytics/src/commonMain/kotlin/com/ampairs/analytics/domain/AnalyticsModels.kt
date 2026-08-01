package com.ampairs.analytics.domain

/** Reporting period grain for the dashboard (bucketed in the workspace business timezone). */
enum class Period { DAY, WEEK, MONTH }

/** A single computed KPI value for display. */
data class KpiValue(
    val metricId: String,
    val unit: String,
    val value: Double,
)

/** Mobile-side mirror of a server demand forecast (pulled read-only). */
data class DemandForecast(
    val productId: String,
    val periodStart: String, // ISO date
    val horizon: Int,
    val meanQty: Double,
    val stdDevQty: Double,
    val method: String,
    val confidence: String,
)
