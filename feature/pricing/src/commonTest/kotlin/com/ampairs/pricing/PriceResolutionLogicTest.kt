package com.ampairs.pricing

import com.ampairs.pricing.data.repository.appliedTierMinQty
import com.ampairs.pricing.data.repository.matchesTargeting
import com.ampairs.pricing.data.repository.priceMinorForQuantity
import com.ampairs.pricing.data.repository.specificity
import com.ampairs.pricing.domain.model.GeoZoneMembers
import com.ampairs.pricing.domain.model.PriceList
import com.ampairs.pricing.domain.model.PriceListItem
import com.ampairs.pricing.domain.model.PriceTier
import com.ampairs.pricing.domain.model.SalesChannel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pure-logic tests for the offline price resolver helpers (no Room/DI). Mirrors the backend US1
 * acceptance criteria: a tiered wholesale item (1–9 ₹240 / 10–49 ₹225 / 50+ ₹210, MOQ 10) resolves
 * the right slab by quantity, plus targeting precedence and geo-zone membership.
 */
class PriceResolutionLogicTest {

    // Money is minor units (paise): ₹240.00 = 24000.
    private fun tieredItem() = PriceListItem(
        uid = "pli1",
        priceListId = "pl1",
        productId = "PRD1",
        unitPriceMinor = 24_000L,
        moq = 10.0,
        tiers = listOf(
            PriceTier(minQty = 1.0, unitPriceMinor = 24_000L),
            PriceTier(minQty = 10.0, unitPriceMinor = 22_500L),
            PriceTier(minQty = 50.0, unitPriceMinor = 21_000L),
        ),
    )

    @Test
    fun `tier price resolves by quantity slab`() {
        val item = tieredItem()
        assertEquals(24_000L, item.priceMinorForQuantity(5.0), "qty 5 -> base/first slab ₹240")
        assertEquals(22_500L, item.priceMinorForQuantity(10.0), "qty 10 -> ₹225")
        assertEquals(22_500L, item.priceMinorForQuantity(49.0), "qty 49 -> still ₹225")
        assertEquals(21_000L, item.priceMinorForQuantity(60.0), "qty 60 -> ₹210")
        assertEquals(21_000L, item.priceMinorForQuantity(1000.0), "above top tier -> top slab price")
    }

    @Test
    fun `applied tier min qty reflects the selected slab`() {
        val item = tieredItem()
        assertEquals(1.0, item.appliedTierMinQty(5.0))
        assertEquals(10.0, item.appliedTierMinQty(10.0))
        assertEquals(50.0, item.appliedTierMinQty(60.0))
    }

    @Test
    fun `item with no tiers falls back to base unit price`() {
        val flat = PriceListItem(productId = "PRD1", unitPriceMinor = 18_000L)
        assertEquals(18_000L, flat.priceMinorForQuantity(1.0))
        assertEquals(18_000L, flat.priceMinorForQuantity(999.0))
        assertNull(flat.appliedTierMinQty(5.0))
    }

    @Test
    fun `specificity ranks per-customer above group above broad`() {
        val base = PriceList(name = "L", channel = SalesChannel.WHOLESALE)
        val perCustomer = base.copy(customerId = "C1")
        val perGroup = base.copy(customerGroupId = "G1")
        val perCategory = base.copy(categoryId = "CAT1")
        assertTrue(perCustomer.specificity() > perGroup.specificity())
        assertTrue(perGroup.specificity() > perCategory.specificity())
        assertTrue(perCategory.specificity() > base.specificity())
    }

    @Test
    fun `matchesTargeting enforces each set dimension`() {
        val groupList = PriceList(name = "L", channel = SalesChannel.WHOLESALE, customerGroupId = "G1")
        assertTrue(groupList.matchesTargeting(customerId = null, customerGroupId = "G1", customerType = null, zoneId = null))
        assertFalse(groupList.matchesTargeting(customerId = null, customerGroupId = "G2", customerType = null, zoneId = null))

        val zoneList = PriceList(name = "L", channel = SalesChannel.RETAIL, geoZoneId = "Z1")
        assertTrue(zoneList.matchesTargeting(null, null, null, zoneId = "Z1"))
        assertFalse(zoneList.matchesTargeting(null, null, null, zoneId = "Z2"))

        // An untargeted list matches any context.
        val anyList = PriceList(name = "L")
        assertTrue(anyList.matchesTargeting(null, null, null, null))
    }

    @Test
    fun `geo-zone membership matches exact, range, and state`() {
        val members = GeoZoneMembers(
            pincodes = listOf("560001"),
            pincodeRanges = listOf(GeoZoneMembers.PincodeRange(from = "560100", to = "560200")),
            states = listOf("KA"),
        )
        assertTrue(members.contains("560001"), "exact pincode")
        assertTrue(members.contains("560150"), "inside range")
        assertFalse(members.contains("560201"), "just past range")
        assertFalse(members.contains("110001"), "unrelated pincode")
        assertTrue(members.contains("999999", state = "KA"), "state match")
        assertFalse(members.contains("999999", state = "MH"), "wrong state")
    }
}
