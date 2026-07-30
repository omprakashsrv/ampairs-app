package com.ampairs.analytics.domain

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

/** Unit tests for the pure receivables-aging bucketing (feature 022, T019). */
class AgingBucketsTest {

    private val today = LocalDate(2026, 7, 1)

    @Test
    fun `entries land in the correct age bucket and amounts sum`() {
        val report = AgingBuckets.compute(
            listOf(
                AgingBuckets.Entry("2026-06-25", 100.0), // 6 days  → 0–30
                AgingBuckets.Entry("2026-06-01", 50.0),  // 30 days → 0–30 (boundary)
                AgingBuckets.Entry("2026-05-31", 200.0), // 31 days → 31–60
                AgingBuckets.Entry("2026-04-01", 400.0), // ~91 days → 90+
            ),
            today,
        )
        val byLabel = report.buckets.associateBy { it.label }
        assertEquals(4, report.buckets.size)
        assertEquals(2, byLabel.getValue("0–30 days").count)
        assertEquals(150.0, byLabel.getValue("0–30 days").amount, 1e-9)
        assertEquals(1, byLabel.getValue("31–60 days").count)
        assertEquals(200.0, byLabel.getValue("31–60 days").amount, 1e-9)
        assertEquals(0, byLabel.getValue("61–90 days").count)
        assertEquals(1, byLabel.getValue("90+ days").count)
        assertEquals(400.0, byLabel.getValue("90+ days").amount, 1e-9)
        assertEquals(650.0, report.totalOutstanding, 1e-9)
    }

    @Test
    fun `settled invoices (nothing owed) are skipped`() {
        val report = AgingBuckets.compute(
            listOf(AgingBuckets.Entry("2026-06-25", 0.0), AgingBuckets.Entry("2026-06-25", 0.005)),
            today,
        )
        assertEquals(0, report.buckets.sumOf { it.count })
        assertEquals(0.0, report.totalOutstanding, 1e-9)
    }

    @Test
    fun `future and unparseable dates age to zero (youngest bucket)`() {
        assertEquals(0, AgingBuckets.ageInDays("2026-07-10", today)) // future → clamped to 0
        assertEquals(0, AgingBuckets.ageInDays("not-a-date", today)) // unparseable → 0
        val report = AgingBuckets.compute(
            listOf(AgingBuckets.Entry("2026-07-10", 10.0), AgingBuckets.Entry("bad", 20.0)),
            today,
        )
        assertEquals(2, report.buckets.first().count)
        assertEquals(30.0, report.buckets.first().amount, 1e-9)
    }
}
