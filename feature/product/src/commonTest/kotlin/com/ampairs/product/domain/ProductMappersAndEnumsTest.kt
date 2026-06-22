package com.ampairs.product.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Pure enum display names + the [Product.toSummary] mapper. No DB/coroutines.
 */
class ProductMappersAndEnumsTest {

    @Test
    fun `ProductType display names`() {
        assertEquals("Retail", ProductType.RETAIL.displayName)
        assertEquals("Wholesale", ProductType.WHOLESALE.displayName)
        assertEquals("Service", ProductType.SERVICE.displayName)
        assertEquals(3, ProductType.entries.size)
    }

    @Test
    fun `ServiceType display names`() {
        assertEquals("Physical Product", ServiceType.PHYSICAL.displayName)
        assertEquals("Service", ServiceType.SERVICE.displayName)
        assertEquals("Digital Product", ServiceType.DIGITAL.displayName)
        assertEquals(3, ServiceType.entries.size)
    }

    @Test
    fun `TaxType and TaxSpec values`() {
        assertEquals(listOf("HSN", "SAC"), TaxType.entries.map { it.name })
        assertEquals(listOf("INTER", "INTRA"), TaxSpec.entries.map { it.name })
    }

    @Test
    fun `toSummary copies identity, pricing and classification, with primaryImageUrl null`() {
        val product = Product(
            id = "P1",
            name = "Widget",
            code = "C1",
            mrp = 100.0,
            dp = 80.0,
            sellingPrice = 90.0,
            stockQuantity = 5.0,
            lowStockAlert = 2.0,
            taxCode = "HSN1",
            productType = ProductType.RETAIL,
            serviceType = ServiceType.PHYSICAL,
            hasVariants = true,
            categoryName = "Tools",
            brandName = "Acme",
            baseUnitId = "U1",
        )

        val summary = product.toSummary()

        assertEquals("P1", summary.id)
        assertEquals("Widget", summary.name)
        assertEquals(100.0, summary.mrp)
        assertEquals(90.0, summary.sellingPrice)
        assertEquals(5.0, summary.stockQuantity)
        assertEquals("HSN1", summary.taxCode)
        assertEquals(ProductType.RETAIL, summary.productType)
        assertEquals(ServiceType.PHYSICAL, summary.serviceType)
        assertEquals("Tools", summary.categoryName)
        assertEquals("Acme", summary.brandName)
        assertEquals("U1", summary.baseUnitId)
        assertNull(summary.primaryImageUrl, "summary never carries an image url from the domain model")
    }
}
