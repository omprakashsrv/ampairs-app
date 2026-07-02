package com.ampairs.product.resolver

import com.ampairs.product.db.entity.ProductStandardCost
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Pure-logic tests for the offline standard-purchase-cost resolver helpers (no Room/DI). Mirrors the
 * sales-side `PriceResolutionLogicTest`: effective-window (from-inclusive / to-exclusive), null
 * bounds (legacy + open-ended), variant > base precedence, and most-recently-effective selection.
 */
@OptIn(ExperimentalTime::class)
class PurchaseCostResolutionLogicTest {

    private fun cost(
        uid: String,
        costPrice: Double,
        variantSku: String? = null,
        effectiveFrom: String? = null,
        effectiveTo: String? = null,
    ) = ProductStandardCost(
        uid = uid,
        productId = "PRD1",
        variantSku = variantSku,
        costPrice = costPrice,
        effectiveFrom = effectiveFrom,
        effectiveTo = effectiveTo,
    )

    @Test
    fun `effective window includes from (inclusive) and excludes to (exclusive)`() {
        val item = cost(
            uid = "c1",
            costPrice = 100.0,
            effectiveFrom = "2026-03-01T00:00:00Z",
            effectiveTo = "2026-06-01T00:00:00Z",
        )
        assertFalse(item.isEffectiveAt(Instant.parse("2026-02-28T23:59:59Z")), "before from -> dormant")
        assertTrue(item.isEffectiveAt(Instant.parse("2026-03-01T00:00:00Z")), "exactly at from -> effective (inclusive)")
        assertTrue(item.isEffectiveAt(Instant.parse("2026-04-15T10:00:00Z")), "inside window")
        assertFalse(item.isEffectiveAt(Instant.parse("2026-06-01T00:00:00Z")), "exactly at to -> not effective (exclusive)")
        assertFalse(item.isEffectiveAt(Instant.parse("2026-07-01T00:00:00Z")), "after to -> expired")
    }

    @Test
    fun `null effective bounds mean always effective (legacy rows)`() {
        val legacy = cost(uid = "c1", costPrice = 80.0)
        assertTrue(legacy.isEffectiveAt(Instant.parse("2000-01-01T00:00:00Z")))
        assertTrue(legacy.isEffectiveAt(Instant.parse("2099-12-31T23:59:59Z")))

        val openEnded = cost(uid = "c2", costPrice = 90.0, effectiveFrom = "2026-03-01T00:00:00Z")
        assertFalse(openEnded.isEffectiveAt(Instant.parse("2026-02-01T00:00:00Z")), "before from")
        assertTrue(openEnded.isEffectiveAt(Instant.parse("2030-01-01T00:00:00Z")), "open-ended stays current")
    }

    @Test
    fun `pick returns null when nothing is effective at the as-of instant`() {
        val future = cost(uid = "c1", costPrice = 100.0, effectiveFrom = "2030-01-01T00:00:00Z")
        assertNull(pickStandardCost(listOf(future), variantSku = null, asOf = Instant.parse("2026-04-01T00:00:00Z")))
        assertNull(pickStandardCost(emptyList(), variantSku = null, asOf = Instant.parse("2026-04-01T00:00:00Z")))
    }

    @Test
    fun `pick selects the most recently effective version at the as-of instant`() {
        val older = cost(uid = "c1", costPrice = 100.0, effectiveFrom = "2026-01-01T00:00:00Z")
        val newer = cost(uid = "c2", costPrice = 120.0, effectiveFrom = "2026-03-01T00:00:00Z")
        val future = cost(uid = "c3", costPrice = 140.0, effectiveFrom = "2026-09-01T00:00:00Z")
        val picked = pickStandardCost(
            listOf(older, newer, future),
            variantSku = null,
            asOf = Instant.parse("2026-04-01T00:00:00Z"),
        )
        assertEquals("c2", picked?.uid, "most-recently-effective version (not the dormant future one) wins")
        assertEquals(120.0, picked?.costPrice)
    }

    @Test
    fun `pick prefers a variant match over the base product`() {
        val base = cost(uid = "base", costPrice = 100.0)
        val variant = cost(uid = "var", costPrice = 110.0, variantSku = "SKU-A")
        val picked = pickStandardCost(
            listOf(base, variant),
            variantSku = "SKU-A",
            asOf = Instant.parse("2026-04-01T00:00:00Z"),
        )
        assertEquals("var", picked?.uid, "variant-specific cost outranks the base cost")
        assertEquals(110.0, picked?.costPrice)
    }

    @Test
    fun `pick falls back to base when the requested variant has no cost`() {
        val base = cost(uid = "base", costPrice = 100.0)
        val otherVariant = cost(uid = "var", costPrice = 110.0, variantSku = "SKU-B")
        val picked = pickStandardCost(
            listOf(base, otherVariant),
            variantSku = "SKU-A",
            asOf = Instant.parse("2026-04-01T00:00:00Z"),
        )
        assertEquals("base", picked?.uid, "no SKU-A cost -> fall back to the base product cost")
    }
}
