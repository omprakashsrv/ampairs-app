package com.ampairs.product.domain

import com.ampairs.product.db.entity.ProductVariantEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pure domain logic for [ProductVariant] — computed properties (display names, stock predicates,
 * attribute map) and the entity ↔ domain mappers. No DB or coroutines: these are deterministic
 * functions, so the assertions pin the exact behaviour of the source as written.
 */
class ProductVariantTest {

    private fun variant(
        attribute1Name: String? = null,
        attribute1Value: String? = null,
        attribute2Name: String? = null,
        attribute2Value: String? = null,
        attribute3Name: String? = null,
        attribute3Value: String? = null,
        variantName: String = "Shirt",
        stockQuantity: Double = 5.0,
        lowStockAlert: Double? = null,
        mrp: Double? = null,
        dealerPrice: Double? = null,
        sellingPrice: Double? = null,
    ) = ProductVariant(
        id = "V1",
        productId = "P1",
        sku = "SKU1",
        variantName = variantName,
        attribute1Name = attribute1Name,
        attribute1Value = attribute1Value,
        attribute2Name = attribute2Name,
        attribute2Value = attribute2Value,
        attribute3Name = attribute3Name,
        attribute3Value = attribute3Value,
        mrp = mrp,
        dealerPrice = dealerPrice,
        sellingPrice = sellingPrice,
        stockQuantity = stockQuantity,
        lowStockAlert = lowStockAlert,
    )

    @Test
    fun `displayName joins present attribute values with comma`() {
        val v = variant(
            attribute1Name = "Size", attribute1Value = "Large",
            attribute2Name = "Color", attribute2Value = "Blue",
            attribute3Name = "Material", attribute3Value = "Cotton",
        )
        assertEquals("Large, Blue, Cotton", v.displayName)
    }

    @Test
    fun `displayName skips null values and keeps order`() {
        val v = variant(
            attribute1Name = "Size", attribute1Value = "Large",
            attribute3Name = "Material", attribute3Value = "Cotton",
        )
        assertEquals("Large, Cotton", v.displayName)
    }

    @Test
    fun `displayName falls back to variantName when no attribute values`() {
        val v = variant(variantName = "Plain Shirt")
        assertEquals("Plain Shirt", v.displayName)
    }

    @Test
    fun `fullDisplayName prefixes variantName when displayName differs`() {
        val v = variant(
            variantName = "Shirt",
            attribute1Name = "Size", attribute1Value = "Large",
        )
        assertEquals("Shirt - Large", v.fullDisplayName)
    }

    @Test
    fun `fullDisplayName collapses to variantName when no attributes`() {
        // displayName == variantName, so the prefix branch is skipped.
        val v = variant(variantName = "Shirt")
        assertEquals("Shirt", v.fullDisplayName)
    }

    @Test
    fun `isLowStock is false when no alert threshold set`() {
        assertFalse(variant(stockQuantity = 0.0, lowStockAlert = null).isLowStock)
    }

    @Test
    fun `isLowStock true at or below threshold, false above`() {
        assertTrue(variant(stockQuantity = 2.0, lowStockAlert = 2.0).isLowStock, "boundary is inclusive")
        assertTrue(variant(stockQuantity = 1.0, lowStockAlert = 2.0).isLowStock)
        assertFalse(variant(stockQuantity = 3.0, lowStockAlert = 2.0).isLowStock)
    }

    @Test
    fun `isOutOfStock at or below zero`() {
        assertTrue(variant(stockQuantity = 0.0).isOutOfStock)
        assertTrue(variant(stockQuantity = -1.0).isOutOfStock)
        assertFalse(variant(stockQuantity = 0.5).isOutOfStock)
    }

    @Test
    fun `attributes map includes only complete name-value pairs`() {
        val v = variant(
            attribute1Name = "Size", attribute1Value = "L",
            attribute2Name = "Color", attribute2Value = null, // value missing -> excluded
            attribute3Name = null, attribute3Value = "Cotton", // name missing -> excluded
        )
        assertEquals(mapOf("Size" to "L"), v.attributes)
    }

    @Test
    fun `effective prices prefer variant override, else fall back to base`() {
        val overridden = variant(mrp = 100.0, dealerPrice = 80.0, sellingPrice = 90.0)
        assertEquals(100.0, overridden.getEffectiveMrp(999.0))
        assertEquals(80.0, overridden.getEffectiveDealerPrice(999.0))
        assertEquals(90.0, overridden.getEffectiveSellingPrice(999.0))

        val noOverride = variant(mrp = null, dealerPrice = null, sellingPrice = null)
        assertEquals(50.0, noOverride.getEffectiveMrp(50.0))
        assertEquals(40.0, noOverride.getEffectiveDealerPrice(40.0))
        assertEquals(45.0, noOverride.getEffectiveSellingPrice(45.0))
    }

    @Test
    fun `entity to domain maps Int flags to Boolean`() {
        val entity = ProductVariantEntity(
            id = "V1",
            productId = "P1",
            sku = "SKU1",
            variantName = "Shirt",
            attribute1Name = "Size",
            attribute1Value = "L",
            active = 0,
            synced = 1,
            stockQuantity = 7.0,
        )
        val domain = entity.toDomain()
        assertEquals("V1", domain.id)
        assertEquals("SKU1", domain.sku)
        assertFalse(domain.active, "active = 0 -> false")
        assertTrue(domain.synced, "synced = 1 -> true")
        assertEquals(7.0, domain.stockQuantity)
        assertEquals("Size", domain.attribute1Name)
    }

    @Test
    fun `domain to entity round-trips flags and preserves provided lastUpdated`() {
        val domain = variant(
            attribute1Name = "Size", attribute1Value = "L",
        ).copy(active = false, synced = true, lastUpdated = 12345L)
        val entity = domain.toEntity()
        assertEquals(0, entity.active, "active false -> 0")
        assertEquals(1, entity.synced, "synced true -> 1")
        assertEquals(12345L, entity.lastUpdated, "provided lastUpdated is kept (not overwritten by now)")
        assertEquals("SKU1", entity.sku)
    }

    @Test
    fun `toDomainList maps every element`() {
        val entities = listOf(
            ProductVariantEntity(id = "A", productId = "P", sku = "s1", variantName = "n1"),
            ProductVariantEntity(id = "B", productId = "P", sku = "s2", variantName = "n2"),
        )
        val domains = entities.toDomainList()
        assertEquals(listOf("A", "B"), domains.map { it.id })
    }
}
