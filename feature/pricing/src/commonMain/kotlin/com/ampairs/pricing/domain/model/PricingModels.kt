package com.ampairs.pricing.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Sales channel a price list prices in. Mirrors backend `core` SalesChannel. */
enum class SalesChannel { RETAIL, WHOLESALE }

/** Lifecycle of a price list. */
enum class PriceListStatus { DRAFT, ACTIVE, INACTIVE }

/** Source of a resolved price (mirrors backend PriceResolutionResponse.source). */
enum class PriceSource { PRICE_LIST, CATALOG_FALLBACK }

/** Money on the wire: `{ amount_minor, currency }` (matches backend core MoneyDto). */
@Serializable
data class MoneyDto(
    @SerialName("amount_minor") val amountMinor: Long = 0,
    val currency: String? = null,
)

/** Structured attribute predicate (lowest-precedence targeting). */
@Serializable
data class AttributePredicate(
    val field: String,
    val operator: String,
    val value: String,
)

/**
 * Price-list header. Mirrors backend `PriceListResponse` (items are a SEPARATE sync feed —
 * see [PriceListItem]). Also serialized as the push body; the backend `PriceListRequest` ignores
 * the extra `created_at`/`updated_at` fields.
 */
@Serializable
data class PriceList(
    val uid: String = "",
    @SerialName("ref_id") val refId: String? = null,
    val name: String,
    val channel: SalesChannel = SalesChannel.RETAIL,
    @SerialName("customer_group_id") val customerGroupId: String? = null,
    @SerialName("customer_type") val customerType: String? = null,
    @SerialName("customer_id") val customerId: String? = null,
    @SerialName("brand_id") val brandId: String? = null,
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("product_group_id") val productGroupId: String? = null,
    @SerialName("geo_zone_id") val geoZoneId: String? = null,
    @SerialName("attribute_predicates") val attributePredicates: List<AttributePredicate> = emptyList(),
    val currency: String = "INR",
    val priority: Int = 0,
    val status: PriceListStatus = PriceListStatus.DRAFT,
    @SerialName("starts_at") val startsAt: String? = null,
    @SerialName("ends_at") val endsAt: String? = null,
    val active: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

/** A quantity slab (local/domain form): at `minQty` and above, the unit price is `unitPriceMinor`. */
data class PriceTier(
    val minQty: Double,
    val unitPriceMinor: Long,
)

/**
 * Price-list item (local/domain form). Money is held in minor units to match the backend.
 * Wire mapping is asymmetric (pull = MoneyDto, push = minor) — see the api layer DTOs.
 */
data class PriceListItem(
    val uid: String = "",
    val refId: String? = null,
    val priceListId: String = "",
    val productId: String,
    val variantSku: String? = null,
    val unitPriceMinor: Long,
    val currency: String? = null,
    val moq: Double? = null,
    val tiers: List<PriceTier> = emptyList(),
    /** Effective from this instant (ISO-8601, UTC); null = effective from the beginning (back-compat). */
    val effectiveFrom: String? = null,
    /** Effective until this instant (ISO-8601, UTC, exclusive); null = open-ended (the current price). */
    val effectiveTo: String? = null,
    val active: Boolean = true,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

/** Header + its items, assembled locally for the editor UI (never serialized as one wire object). */
data class PriceListAggregate(
    val header: PriceList,
    val items: List<PriceListItem> = emptyList(),
)

/** Geo-zone membership — mirrors backend `GeoZoneMembers`. */
@Serializable
data class GeoZoneMembers(
    val pincodes: List<String> = emptyList(),
    @SerialName("pincode_ranges") val pincodeRanges: List<PincodeRange> = emptyList(),
    val states: List<String> = emptyList(),
) {
    @Serializable
    data class PincodeRange(val from: String, val to: String)

    /** True if [pincode] falls in this zone (exact list, a numeric range, or — when given — its [state]). */
    fun contains(pincode: String, state: String? = null): Boolean {
        if (pincode in pincodes) return true
        if (state != null && states.any { it.equals(state, ignoreCase = true) }) return true
        val numeric = pincode.toLongOrNull()
        if (numeric != null && pincodeRanges.any { r ->
                val from = r.from.toLongOrNull()
                val to = r.to.toLongOrNull()
                from != null && to != null && numeric in from..to
            }
        ) return true
        return false
    }
}

@Serializable
data class GeoZone(
    val uid: String = "",
    @SerialName("ref_id") val refId: String? = null,
    val name: String,
    val members: GeoZoneMembers = GeoZoneMembers(),
    val active: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

/** Result of resolving an effective unit price (major units, for the order/invoice line seam). */
data class PriceResolution(
    val effectiveUnitPrice: Double,
    val currency: String,
    val source: PriceSource,
    val matchedPriceListUid: String? = null,
    val appliedTierMinQty: Double? = null,
    val belowMoq: Boolean = false,
)

/**
 * One active price list considered during resolution, with the verdict the Price Tester's
 * "Explain — why this price won" panel renders. [reason] is a short, plain-language sentence.
 */
data class PriceCandidate(
    val uid: String,
    val name: String,
    val channel: SalesChannel,
    val matched: Boolean,
    val won: Boolean,
    val hasItemForProduct: Boolean,
    val specificity: Int,
    val priority: Int,
    val reason: String,
)

/**
 * A resolution plus the ranked candidate trace behind it — the data model for the Price Tester.
 * [candidates] are ordered exactly as the resolver ranks them (specificity → priority).
 */
data class PriceResolutionTrace(
    val resolution: PriceResolution,
    val candidates: List<PriceCandidate>,
    val evaluatedCount: Int,
)
