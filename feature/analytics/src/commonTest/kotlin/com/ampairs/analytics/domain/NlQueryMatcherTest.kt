package com.ampairs.analytics.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/** Unit tests for the deterministic NL → KPI matcher (feature 022, T049). */
class NlQueryMatcherTest {

    private val kpis = DashboardKpis(
        grossSales = 1000.0,
        netSales = 900.0,
        totalTax = 100.0,
        invoiceCount = 12,
        averageInvoiceValue = 83.3,
        collectionsReceived = 700.0,
        stockValue = 5000.0,
        lowStockCount = 4,
        outstandingReceivable = 300.0,
        inventoryTurns = 2.0,
    )

    private fun answered(q: String): NlAnswer.Answered {
        val a = NlQueryMatcher.match(q, kpis)
        assertIs<NlAnswer.Answered>(a)
        return a
    }

    @Test
    fun `net sales beats the broader sales match`() {
        assertEquals(NlMetric.NET_SALES, answered("what were net sales this month").metric)
    }

    @Test
    fun `common sales phrasings map to gross`() {
        assertEquals(NlMetric.GROSS_SALES, answered("total sales").metric)
        assertEquals(NlMetric.GROSS_SALES, answered("show revenue").metric)
        assertEquals(NlMetric.GROSS_SALES, answered("turnover").metric)
    }

    @Test
    fun `invoices tax outstanding collections low-stock map correctly`() {
        assertEquals(NlMetric.INVOICES, answered("how many invoices").metric)
        assertEquals(NlMetric.TAX, answered("gst collected").metric)
        assertEquals(NlMetric.OUTSTANDING, answered("what is outstanding").metric)
        assertEquals(NlMetric.COLLECTIONS, answered("payments received").metric)
        assertEquals(NlMetric.LOW_STOCK, answered("low stock items").metric)
        assertEquals(NlMetric.STOCK_VALUE, answered("stock value").metric)
    }

    @Test
    fun `answered value comes from the KPIs`() {
        assertEquals(300.0, answered("outstanding").value, 1e-9)
        assertEquals(12.0, answered("invoices").value, 1e-9)
    }

    @Test
    fun `unmatched and blank questions are unanswered`() {
        assertEquals(NlAnswer.Unanswered, NlQueryMatcher.match("what is the meaning of life", kpis))
        assertEquals(NlAnswer.Unanswered, NlQueryMatcher.match("   ", kpis))
    }
}
