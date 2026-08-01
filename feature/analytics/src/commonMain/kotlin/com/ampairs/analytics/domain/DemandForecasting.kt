package com.ampairs.analytics.domain

/**
 * On-device demand forecast fallback (feature 022, T044). Pure, dependency-free EWMA used for the
 * dashboard "expected demand" figure **only when the server-computed [DemandForecastEntity] mirror is
 * empty** (offline first launch, or the nightly backend batch has not run yet).
 *
 * This deliberately does NOT re-implement the backend's additive Holt-Winters (that lives server-side
 * in `DemandForecasting.summarize`); a single exponentially-weighted moving average over a short
 * trailing window is enough for an at-a-glance offline estimate and is trivially unit-testable. When a
 * server forecast exists it always wins — this is the floor, not a competitor.
 *
 * Units match the backend contract: [expectedDemand] returns a **horizon-total** expected quantity
 * (per-day level × horizon), so it slots into the same field as the server `mean_qty`.
 */
object DemandForecasting {

    /** Smoothing factor. Higher = more weight on recent days. 0.4 tracks recent demand without noise. */
    const val DEFAULT_ALPHA: Double = 0.4

    /** Forecast window used when a caller has no server horizon to borrow (matches the backend default). */
    const val DEFAULT_HORIZON_DAYS: Int = 7

    /**
     * Exponentially-weighted moving average of a daily [series] (oldest → newest, zero-filled gaps).
     * Returns the smoothed **per-day** level, clamped non-negative. Empty series → 0.
     */
    fun ewma(series: List<Double>, alpha: Double = DEFAULT_ALPHA): Double {
        require(alpha > 0.0 && alpha <= 1.0) { "alpha must be in (0, 1], was $alpha" }
        if (series.isEmpty()) return 0.0
        var level = series.first()
        for (i in 1 until series.size) {
            level = alpha * series[i] + (1.0 - alpha) * level
        }
        return level.coerceAtLeast(0.0)
    }

    /**
     * Horizon-total expected demand = smoothed per-day level × [horizonDays]. Slots into the same
     * position as the server forecast's `mean_qty` (horizon-total). Non-positive horizon → 0.
     */
    fun expectedDemand(
        dailySeries: List<Double>,
        horizonDays: Int = DEFAULT_HORIZON_DAYS,
        alpha: Double = DEFAULT_ALPHA,
    ): Double {
        if (horizonDays <= 0) return 0.0
        return ewma(dailySeries, alpha) * horizonDays
    }
}
