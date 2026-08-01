package com.ampairs.analytics.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Unit tests for the on-device EWMA demand fallback (feature 022, T044). */
class DemandForecastingTest {

    @Test
    fun `empty series forecasts zero`() {
        assertEquals(0.0, DemandForecasting.ewma(emptyList()))
        assertEquals(0.0, DemandForecasting.expectedDemand(emptyList(), horizonDays = 7))
    }

    @Test
    fun `constant series smooths to the constant`() {
        val level = DemandForecasting.ewma(List(10) { 5.0 })
        assertEquals(5.0, level, 1e-9)
    }

    @Test
    fun `ewma weights recent days more heavily than a flat mean`() {
        // Demand ramps up 1..10; EWMA level should sit above the simple mean (5.5).
        val series = (1..10).map { it.toDouble() }
        val level = DemandForecasting.ewma(series, alpha = 0.5)
        assertTrue(level > series.average(), "EWMA=$level should exceed mean=${series.average()}")
        assertTrue(level < 10.0, "EWMA=$level should stay below the latest value")
    }

    @Test
    fun `expected demand is per-day level times horizon`() {
        val series = List(14) { 4.0 }
        val expected = DemandForecasting.expectedDemand(series, horizonDays = 7)
        assertEquals(28.0, expected, 1e-9) // 4/day × 7 days
    }

    @Test
    fun `non-positive horizon yields zero`() {
        assertEquals(0.0, DemandForecasting.expectedDemand(List(5) { 3.0 }, horizonDays = 0))
    }

    @Test
    fun `level is clamped non-negative`() {
        assertTrue(DemandForecasting.ewma(listOf(-2.0, -1.0, 0.0)) >= 0.0)
    }

    @Test
    fun `alpha out of range is rejected`() {
        assertFailsWith<IllegalArgumentException> { DemandForecasting.ewma(listOf(1.0), alpha = 0.0) }
        assertFailsWith<IllegalArgumentException> { DemandForecasting.ewma(listOf(1.0), alpha = 1.5) }
    }
}
