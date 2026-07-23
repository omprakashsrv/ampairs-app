package com.ampairs.pricing

import com.ampairs.pricing.domain.model.Offer
import com.ampairs.pricing.domain.model.OfferConditionType
import com.ampairs.pricing.domain.model.OfferRewardType
import com.ampairs.pricing.domain.model.OfferStatus
import com.ampairs.pricing.domain.model.SalesChannel
import com.ampairs.pricing.domain.offer.CartContext
import com.ampairs.pricing.domain.offer.CartLine
import com.ampairs.pricing.domain.offer.OfferApplicationEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pure-logic tests for the offline offer engine — the KMP mirror of the backend
 * `OfferApplicationServiceTest`. Same scenarios so both engines stay in parity (money is minor units).
 */
class OfferApplicationEngineTest {

    private val now = "2026-06-27T00:00:00"

    private fun offer(uid: String, block: Offer.() -> Offer = { this }) =
        Offer(uid = uid, name = "Offer $uid", channel = SalesChannel.RETAIL, status = OfferStatus.ACTIVE).block()

    private fun ctx(vararg lines: CartLine, block: CartContext.() -> CartContext = { this }) =
        CartContext(channel = SalesChannel.RETAIL, customerGroupId = "RETAILERS", lines = lines.toList()).block()

    private fun line(price: Long, qty: Double) = CartLine(productId = "P", unitPriceMinor = price, quantity = qty)

    @Test
    fun cartMinPercentFiresAboveThreshold() {
        val offers = listOf(offer("O1") {
            copy(conditionType = OfferConditionType.CART_MIN, cartMinMinor = 200_000,
                rewardType = OfferRewardType.PERCENT, rewardPercent = 10.0)
        })
        // 30 x ₹100 = ₹3,000 ≥ ₹2,000 → 10% = ₹300
        val res = OfferApplicationEngine.apply(offers, ctx(line(10_000, 30.0)), now)
        assertEquals(30_000L, res.totalDiscountMinor)
        assertEquals(1, res.appliedOffers.size)
    }

    @Test
    fun cartMinDoesNotFireBelowThreshold() {
        val offers = listOf(offer("O1") {
            copy(conditionType = OfferConditionType.CART_MIN, cartMinMinor = 200_000,
                rewardType = OfferRewardType.PERCENT, rewardPercent = 10.0)
        })
        val res = OfferApplicationEngine.apply(offers, ctx(line(10_000, 5.0)), now)
        assertEquals(0L, res.totalDiscountMinor)
        assertTrue(res.appliedOffers.isEmpty())
    }

    @Test
    fun percentCapApplies() {
        val offers = listOf(offer("O1") {
            copy(rewardType = OfferRewardType.PERCENT, rewardPercent = 50.0, rewardCapMinor = 10_000)
        })
        val res = OfferApplicationEngine.apply(offers, ctx(line(100_000, 1.0)), now)
        assertEquals(10_000L, res.totalDiscountMinor)
    }

    @Test
    fun flatClampsToSubtotal() {
        val offers = listOf(offer("O1") { copy(rewardType = OfferRewardType.FLAT, rewardFlatMinor = 50_000) })
        val res = OfferApplicationEngine.apply(offers, ctx(line(20_000, 1.0)), now)
        assertEquals(20_000L, res.totalDiscountMinor)
    }

    @Test
    fun bogoEmitsFreeGoodsBySets() {
        val offers = listOf(offer("O1") { copy(rewardType = OfferRewardType.BOGO, bogoBuyQty = 2, bogoGetQty = 1) })
        val res = OfferApplicationEngine.apply(offers, ctx(line(10_000, 5.0)), now)
        assertEquals(0L, res.totalDiscountMinor)
        assertEquals(1, res.freeGoods.size)
        assertEquals(2.0, res.freeGoods.first().quantity)
    }

    @Test
    fun targetingDifferentGroupDoesNotApply() {
        val offers = listOf(offer("O1") {
            copy(customerGroupId = "WHOLESALERS", rewardType = OfferRewardType.FLAT, rewardFlatMinor = 5_000)
        })
        val res = OfferApplicationEngine.apply(offers, ctx(line(100_000, 1.0)) { copy(customerGroupId = "RETAILERS") }, now)
        assertTrue(res.appliedOffers.isEmpty())
    }

    @Test
    fun twoStackableOffersApplyCumulatively() {
        val offers = listOf(
            offer("O1") { copy(priority = 10, stackable = true, rewardType = OfferRewardType.PERCENT, rewardPercent = 10.0) },
            offer("O2") { copy(priority = 5, stackable = true, rewardType = OfferRewardType.FLAT, rewardFlatMinor = 5_000) },
        )
        val res = OfferApplicationEngine.apply(offers, ctx(line(100_000, 1.0)), now)
        assertEquals(15_000L, res.totalDiscountMinor)
        assertEquals(2, res.appliedOffers.size)
    }

    @Test
    fun exclusiveOfferBlocksOthers() {
        val offers = listOf(
            offer("EX") { copy(priority = 1, exclusive = true, rewardType = OfferRewardType.FLAT, rewardFlatMinor = 2_000) },
            offer("O2") { copy(priority = 99, stackable = true, rewardType = OfferRewardType.PERCENT, rewardPercent = 50.0) },
        )
        val res = OfferApplicationEngine.apply(offers, ctx(line(100_000, 1.0)), now)
        assertEquals(1, res.appliedOffers.size)
        assertEquals("EX", res.appliedOffers.first().offerUid)
    }

    @Test
    fun couponFiresOnlyWithMatchingCode() {
        val offers = listOf(offer("O1") {
            copy(conditionType = OfferConditionType.COUPON, couponCode = "SAVE10",
                rewardType = OfferRewardType.PERCENT, rewardPercent = 10.0)
        })
        assertTrue(OfferApplicationEngine.apply(offers, ctx(line(100_000, 1.0)), now).appliedOffers.isEmpty())
        val withCode = OfferApplicationEngine.apply(offers, ctx(line(100_000, 1.0)) { copy(couponCode = "save10") }, now)
        assertEquals(10_000L, withCode.totalDiscountMinor)
    }

    @Test
    fun outOfWindowOfferDoesNotApply() {
        val offers = listOf(offer("O1") {
            copy(startsAt = "2030-01-01T00:00:00", rewardType = OfferRewardType.FLAT, rewardFlatMinor = 5_000)
        })
        val res = OfferApplicationEngine.apply(offers, ctx(line(100_000, 1.0)), now)
        assertTrue(res.appliedOffers.isEmpty())
    }
}
