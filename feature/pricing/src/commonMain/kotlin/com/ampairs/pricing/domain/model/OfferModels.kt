package com.ampairs.pricing.domain.model

import kotlinx.serialization.SerialName
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
 * A promotion/offer that applies on top of resolved prices. Mirrors the design's offer builder and
 * the backend `OfferResponse` field-for-field (global SNAKE_CASE on the wire → `@SerialName` here).
 *
 * Serialized directly as both the pull row and the push body (the backend `OfferRequest` ignores the
 * extra `created_at`/`updated_at`). Money is held in minor units (paise/cents), consistent with the
 * price-list model. Synced via `SyncEntity.OFFER` / `OfferSyncDelegate`.
 */
@Serializable
data class Offer(
    val uid: String = "",
    val name: String,
    val channel: SalesChannel = SalesChannel.RETAIL,
    val status: OfferStatus = OfferStatus.DRAFT,
    val priority: Int = 0,
    @SerialName("starts_at") val startsAt: String? = null,
    @SerialName("ends_at") val endsAt: String? = null,
    // Targeting — same dimensions as a price list.
    @SerialName("customer_group_id") val customerGroupId: String? = null,
    @SerialName("customer_type") val customerType: String? = null,
    @SerialName("brand_id") val brandId: String? = null,
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("geo_zone_id") val geoZoneId: String? = null,
    // Trigger / condition.
    @SerialName("condition_type") val conditionType: OfferConditionType = OfferConditionType.NONE,
    @SerialName("cart_min_minor") val cartMinMinor: Long? = null,
    @SerialName("quantity_min") val quantityMin: Double? = null,
    @SerialName("coupon_code") val couponCode: String? = null,
    @SerialName("coupon_limit") val couponLimit: Int? = null,
    // Reward.
    @SerialName("reward_type") val rewardType: OfferRewardType = OfferRewardType.PERCENT,
    @SerialName("reward_percent") val rewardPercent: Double? = null,
    @SerialName("reward_flat_minor") val rewardFlatMinor: Long? = null,
    @SerialName("reward_cap_minor") val rewardCapMinor: Long? = null,
    @SerialName("bogo_buy_qty") val bogoBuyQty: Int? = null,
    @SerialName("bogo_get_qty") val bogoGetQty: Int? = null,
    // Stacking & limits.
    val stackable: Boolean = false,
    val exclusive: Boolean = false,
    @SerialName("per_customer_limit") val perCustomerLimit: Int? = null,
    @SerialName("total_limit") val totalLimit: Int? = null,
    @SerialName("used_count") val usedCount: Int = 0,
    val active: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)
