package com.ampairs.pricing.domain.model

import kotlinx.serialization.Serializable

/** Lifecycle of an offer/promotion. */
@Serializable
enum class OfferStatus { DRAFT, SCHEDULED, ACTIVE, INACTIVE, EXPIRED }

/** What has to happen for an offer to fire. */
@Serializable
enum class OfferConditionType { NONE, CART_MIN, QUANTITY_MIN, FIRST_ORDER, COUPON }

/** What the customer gets when an offer fires. */
@Serializable
enum class OfferRewardType { PERCENT, FLAT, BOGO, FREE_SHIPPING, FREE_GIFT, TIERED }

/**
 * A promotion/offer that applies on top of resolved prices. Mirrors the design's offer builder.
 *
 * NOTE: there is no backend offer resource yet, so offers are **local-only** (persisted in the
 * workspace-scoped OffersDatabase, no sync delegate). Money is held in minor units (paise/cents),
 * consistent with the price-list model. When the backend lands, add @SerialName mappings + a
 * SyncEntity.OFFER delegate; the domain shape is already wire-friendly.
 */
@Serializable
data class Offer(
    val uid: String = "",
    val name: String,
    val channel: SalesChannel = SalesChannel.RETAIL,
    val status: OfferStatus = OfferStatus.DRAFT,
    val priority: Int = 0,
    val startsAt: String? = null,
    val endsAt: String? = null,
    // Targeting — same dimensions as a price list.
    val customerGroupId: String? = null,
    val customerType: String? = null,
    val brandId: String? = null,
    val categoryId: String? = null,
    val geoZoneId: String? = null,
    // Trigger / condition.
    val conditionType: OfferConditionType = OfferConditionType.NONE,
    val cartMinMinor: Long? = null,
    val quantityMin: Double? = null,
    val couponCode: String? = null,
    val couponLimit: Int? = null,
    // Reward.
    val rewardType: OfferRewardType = OfferRewardType.PERCENT,
    val rewardPercent: Double? = null,
    val rewardFlatMinor: Long? = null,
    val rewardCapMinor: Long? = null,
    val bogoBuyQty: Int? = null,
    val bogoGetQty: Int? = null,
    // Stacking & limits.
    val stackable: Boolean = false,
    val exclusive: Boolean = false,
    val perCustomerLimit: Int? = null,
    val totalLimit: Int? = null,
    val usedCount: Int = 0,
    val active: Boolean = true,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)
