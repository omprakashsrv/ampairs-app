package com.ampairs.analytics.domain

import kotlin.test.Test
import kotlin.test.assertEquals

/** Unit tests for the deep-history merge (feature 022, T030a). */
class DashboardMergeTest {

    private fun kpis() = DashboardKpis(
        grossSales = 1000.0,
        netSales = 900.0,
        totalTax = 100.0,
        invoiceCount = 10,
        averageInvoiceValue = 100.0,
        collectionsReceived = 500.0,
        stockValue = 7777.0,
        lowStockCount = 3,
        outstandingReceivable = 250.0,
        inventoryTurns = 1.5,
    )

    @Test
    fun `additive KPIs sum and average recomputes`() {
        val local = DashboardData(kpis = kpis())
        val slice = DeepHistorySlice(
            grossSales = 500.0,
            netSales = 450.0,
            totalTax = 50.0,
            invoiceCount = 5,
            collectionsReceived = 200.0,
        )
        val merged = local.mergePriorSlice(slice).kpis
        assertEquals(1500.0, merged.grossSales, 1e-9)
        assertEquals(1350.0, merged.netSales, 1e-9)
        assertEquals(150.0, merged.totalTax, 1e-9)
        assertEquals(15, merged.invoiceCount)
        assertEquals(700.0, merged.collectionsReceived, 1e-9)
        assertEquals(100.0, merged.averageInvoiceValue, 1e-9) // 1500 / 15
    }

    @Test
    fun `snapshot KPIs are untouched by the merge`() {
        val local = DashboardData(kpis = kpis())
        val merged = local.mergePriorSlice(DeepHistorySlice(grossSales = 10.0, invoiceCount = 1)).kpis
        assertEquals(7777.0, merged.stockValue, 1e-9)
        assertEquals(3, merged.lowStockCount)
        assertEquals(250.0, merged.outstandingReceivable, 1e-9)
        assertEquals(1.5, merged.inventoryTurns, 1e-9)
    }

    @Test
    fun `disjoint trend buckets concatenate in date order`() {
        val local = DashboardData(
            trend = listOf(SalesTrendPoint("2026-02-01", 200.0), SalesTrendPoint("2026-02-02", 300.0)),
        )
        val slice = DeepHistorySlice(
            trend = listOf(SalesTrendPoint("2026-01-30", 50.0), SalesTrendPoint("2026-01-31", 75.0)),
        )
        val merged = local.mergePriorSlice(slice).trend
        assertEquals(listOf("2026-01-30", "2026-01-31", "2026-02-01", "2026-02-02"), merged.map { it.bucket })
        assertEquals(listOf(50.0, 75.0, 200.0, 300.0), merged.map { it.total })
    }

    @Test
    fun `overlapping trend buckets sum`() {
        val local = DashboardData(trend = listOf(SalesTrendPoint("2026-01-31", 100.0)))
        val slice = DeepHistorySlice(trend = listOf(SalesTrendPoint("2026-01-31", 40.0)))
        val merged = local.mergePriorSlice(slice).trend
        assertEquals(1, merged.size)
        assertEquals(140.0, merged.first().total, 1e-9)
    }

    @Test
    fun `zero average when no invoices`() {
        val merged = DashboardData().mergePriorSlice(DeepHistorySlice()).kpis
        assertEquals(0.0, merged.averageInvoiceValue, 1e-9)
    }
}
