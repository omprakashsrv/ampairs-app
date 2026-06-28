package com.ampairs.pricing.domain.offer

import com.ampairs.pricing.domain.model.Offer
import com.ampairs.pricing.domain.model.OfferConditionType
import com.ampairs.pricing.domain.model.OfferRewardType
import com.ampairs.pricing.domain.model.OfferStatus
import com.ampairs.pricing.domain.model.SalesChannel
import kotlin.math.floor

/** One resolved cart line. `unitPriceMinor` is the post-price-resolution unit price in minor units. */
data class CartLine(
    val productId: String,
    val variantSku: String? = null,
    val brandId: String? = null,
    val categoryId: String? = null,
    val productGroupId: String? = null,
    val quantity: Double = 1.0,
    val unitPriceMinor: Long = 0,
)

/** Buyer segment + cart for offer application. `zoneId` is the pincode-resolved geo-zone uid (caller resolves). */
data class CartContext(
    val channel: SalesChannel = SalesChannel.RETAIL,
    val customerId: String? = null,
    val customerGroupId: String? = null,
    val customerType: String? = null,
    val zoneId: String? = null,
    val firstOrder: Boolean = false,
    val couponCode: String? = null,
    val currency: String = "INR",
    val lines: List<CartLine> = emptyList(),
)

data class AppliedOffer(
    val offerUid: String,
    val name: String,
    val rewardType: OfferRewardType,
    val discountMinor: Long,
)

data class FreeGood(
    val productId: String,
    val variantSku: String?,
    val quantity: Double,
    val sourceOfferUid: String,
)

data class OfferApplicationResult(
    val appliedOffers: List<AppliedOffer> = emptyList(),
    val freeGoods: List<FreeGood> = emptyList(),
    val totalDiscountMinor: Long = 0,
)

data class RewardPreview(val discountMinor: Long, val capHit: Boolean)

/**
 * Pure, single-sourced promotion engine — the offline KMP mirror of the backend
 * `OfferApplicationServiceImpl`. Same eligibility (channel, customer group/type, brand/category
 * scope, geo-zone, in-window, trigger conditions), same rewards (PERCENT capped, FLAT clamped, BOGO
 * integer sets), and the same priority-ordered stacking/exclusivity — so the merchant app (offline)
 * and the ecom server produce identical results from identical inputs.
 *
 * Money is minor units (paise/cents). commonMain has no BigDecimal, so percentages round HALF_UP via
 * floor(x + 0.5), matching the backend's `setScale(0, HALF_UP)` for non-negative amounts.
 */
object OfferApplicationEngine {

    /** Apply all eligible offers to [ctx]. [nowIso] is the current instant as an ISO-8601 string
     *  (offers store window bounds as ISO strings, which sort lexicographically). */
    fun apply(offers: List<Offer>, ctx: CartContext, nowIso: String): OfferApplicationResult {
        val eligible = offers
            .filter {
                it.active && it.status == OfferStatus.ACTIVE && it.channel == ctx.channel &&
                    withinWindow(it, nowIso) && targetingMatches(it, ctx) && conditionMet(it, ctx)
            }
            .sortedWith(compareByDescending<Offer> { it.priority }.thenBy { it.uid })

        val selected = selectWithStacking(eligible)

        val applied = mutableListOf<AppliedOffer>()
        val freeGoods = mutableListOf<FreeGood>()
        var total = 0L

        for (offer in selected) {
            val scope = scopedLines(offer, ctx.lines)
            when (offer.rewardType) {
                OfferRewardType.PERCENT -> {
                    val d = capped(percentOf(subtotalMinor(scope), offer.rewardPercent ?: 0.0), offer.rewardCapMinor)
                    if (d > 0) { total += d; applied += appliedOf(offer, d) }
                }
                OfferRewardType.FLAT -> {
                    val d = minOf(offer.rewardFlatMinor ?: 0L, subtotalMinor(scope))
                    if (d > 0) { total += d; applied += appliedOf(offer, d) }
                }
                OfferRewardType.BOGO -> {
                    val buy = offer.bogoBuyQty ?: continue
                    val get = offer.bogoGetQty ?: continue
                    if (buy <= 0 || get <= 0) continue
                    var fired = false
                    for (line in scope) {
                        val freeQty = floor(line.quantity / buy) * get
                        if (freeQty > 0) {
                            freeGoods += FreeGood(line.productId, line.variantSku, freeQty, offer.uid)
                            fired = true
                        }
                    }
                    if (fired) applied += appliedOf(offer, 0L)
                }
                else -> Unit // FREE_SHIPPING / FREE_GIFT / TIERED reserved for a later increment.
            }
        }
        return OfferApplicationResult(applied, freeGoods, total)
    }

    /** Reward-only computation for the preview simulator — ignores eligibility/conditions and just
     *  shows what the offer's reward would do to [subtotalMinor] (PERCENT/FLAT; BOGO has no $ value). */
    fun previewReward(offer: Offer, subtotalMinor: Long): RewardPreview = when (offer.rewardType) {
        OfferRewardType.PERCENT -> {
            val raw = percentOf(subtotalMinor, offer.rewardPercent ?: 0.0)
            val cap = offer.rewardCapMinor
            if (cap != null && cap > 0 && raw > cap) RewardPreview(cap, true)
            else RewardPreview(raw.coerceIn(0L, subtotalMinor), false)
        }
        OfferRewardType.FLAT -> RewardPreview((offer.rewardFlatMinor ?: 0L).coerceIn(0L, subtotalMinor), false)
        else -> RewardPreview(0L, false)
    }

    // ── selection ────────────────────────────────────────────────────────────────────────
    private fun selectWithStacking(eligible: List<Offer>): List<Offer> {
        eligible.firstOrNull { it.exclusive }?.let { return listOf(it) }
        val selected = mutableListOf<Offer>()
        var nonStackableUsed = false
        for (offer in eligible) when {
            offer.stackable -> selected += offer
            !nonStackableUsed -> { selected += offer; nonStackableUsed = true }
        }
        return selected
    }

    // ── eligibility ──────────────────────────────────────────────────────────────────────
    private fun withinWindow(offer: Offer, nowIso: String): Boolean {
        offer.startsAt?.takeIf { it.isNotBlank() }?.let { if (nowIso < it) return false }
        offer.endsAt?.takeIf { it.isNotBlank() }?.let { if (nowIso > it) return false }
        return true
    }

    private fun targetingMatches(offer: Offer, ctx: CartContext): Boolean {
        if (offer.customerGroupId != null && offer.customerGroupId != ctx.customerGroupId) return false
        if (offer.customerType != null && offer.customerType != ctx.customerType) return false
        if (offer.geoZoneId != null && offer.geoZoneId != ctx.zoneId) return false
        return scopedLines(offer, ctx.lines).isNotEmpty()
    }

    private fun scopedLines(offer: Offer, lines: List<CartLine>): List<CartLine> {
        val pinsTaxonomy = offer.brandId != null || offer.categoryId != null
        if (!pinsTaxonomy) return lines
        return lines.filter { line ->
            (offer.brandId == null || offer.brandId == line.brandId) &&
                (offer.categoryId == null || offer.categoryId == line.categoryId)
        }
    }

    private fun conditionMet(offer: Offer, ctx: CartContext): Boolean {
        val scope = scopedLines(offer, ctx.lines)
        return when (offer.conditionType) {
            OfferConditionType.NONE -> true
            OfferConditionType.CART_MIN -> subtotalMinor(scope) >= (offer.cartMinMinor ?: 0L)
            OfferConditionType.QUANTITY_MIN -> scope.sumOf { it.quantity } >= (offer.quantityMin ?: 0.0)
            OfferConditionType.FIRST_ORDER -> ctx.firstOrder
            OfferConditionType.COUPON ->
                !offer.couponCode.isNullOrBlank() &&
                    offer.couponCode.equals(ctx.couponCode?.trim(), ignoreCase = true)
        }
    }

    // ── money / helpers ────────────────────────────────────────────────────────────────────
    private fun appliedOf(offer: Offer, discountMinor: Long) =
        AppliedOffer(offer.uid, offer.name, offer.rewardType, discountMinor)

    private fun subtotalMinor(lines: List<CartLine>): Long =
        lines.sumOf { floor(it.unitPriceMinor * it.quantity + 0.5).toLong() }

    private fun percentOf(subtotalMinor: Long, percent: Double): Long =
        floor(subtotalMinor * percent / 100.0 + 0.5).toLong()

    private fun capped(discount: Long, capMinor: Long?): Long =
        if (capMinor != null && capMinor > 0) minOf(discount, capMinor) else discount
}
