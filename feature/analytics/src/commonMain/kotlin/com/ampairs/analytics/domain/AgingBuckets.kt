package com.ampairs.analytics.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

/**
 * Pure receivables-aging bucketing (feature 022, T019). Extracted from `DashboardReadFacade` so the
 * age→bucket math is unit-testable without a DB. Buckets are ordered youngest-first; the last is the
 * catch-all. An invoice's age is whole days between its date and [today] in the caller's (business)
 * calendar — the facade already resolves [today] in the business timezone.
 */
object AgingBuckets {

    /** One open invoice's date (ISO `yyyy-MM-dd…`) and its outstanding amount (major units). */
    data class Entry(val invoiceDate: String, val outstanding: Double)

    private data class Bucket(val label: String, val maxDays: Int)

    // Ordered youngest-first; the last bucket is the catch-all (maxDays = Int.MAX_VALUE).
    private val BUCKETS = listOf(
        Bucket("0–30 days", 30),
        Bucket("31–60 days", 60),
        Bucket("61–90 days", 90),
        Bucket("90+ days", Int.MAX_VALUE),
    )

    /** Bucket [entries] by age; amounts of `<= 0.01` (nothing owed) are skipped. */
    fun compute(entries: List<Entry>, today: LocalDate): AgingReport {
        val counts = IntArray(BUCKETS.size)
        val amounts = DoubleArray(BUCKETS.size)
        entries.forEach { entry ->
            if (entry.outstanding <= 0.01) return@forEach
            val idx = bucketIndex(ageInDays(entry.invoiceDate, today))
            counts[idx]++
            amounts[idx] += entry.outstanding
        }
        return AgingReport(
            buckets = BUCKETS.indices.map { i ->
                AgingBucket(label = BUCKETS[i].label, count = counts[i], amount = amounts[i])
            },
        )
    }

    /** Whole days between the invoice date (first 10 chars) and [today]; unparseable/future → 0. */
    fun ageInDays(invoiceDate: String, today: LocalDate): Int {
        val invoiced = runCatching { LocalDate.parse(invoiceDate.take(10)) }.getOrNull() ?: return 0
        return invoiced.daysUntil(today).coerceAtLeast(0)
    }

    private fun bucketIndex(ageDays: Int): Int {
        BUCKETS.forEachIndexed { i, b -> if (ageDays <= b.maxDays) return i }
        return BUCKETS.lastIndex
    }
}
