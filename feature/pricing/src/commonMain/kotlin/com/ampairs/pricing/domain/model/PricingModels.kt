package com.ampairs.pricing.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Sales channel a price list prices in. Mirrors backend `core` SalesChannel. */
enum class SalesChannel { RETAIL, WHOLESALE }

/** Lifecycle of a price list. */
enum class PriceListStatus { DRAFT, ACTIVE, INACTIVE }

/** Source of a resolved price (mirrors backend PriceResolutionResponse.source). */
enum class PriceSource { PRICE_LIST, CATALOG_FALLBACK }

/** A quantity slab: at `minQty` and above (until the next tier), the unit price is `unitPrice`. */
@Serializable
data class PriceTier(
    @SerialName("min_qty") val minQty: Double,
    @SerialName("unit_price") val unitPrice: Double,
)

/** Structured attribute predicate (lowest-precedence targeting). */
@Serializable
data class AttributePredicate(
    val field: String,
    val operator: String,
    val value: String,
)

@Serializable
data class PriceListItem(
    val uid: String = "",
    @SerialName("price_list_id") val priceListId: String = "",
    @SerialName("product_id") val productId: String,
    @SerialName("variant_sku") val variantSku: String? = null,
    @SerialName("unit_price") val unitPrice: Double,
    val moq: Double? = null,
    val tiers: List<PriceTier> = emptyList(),
    val active: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class PriceList(
    val uid: String = "",
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
    val items: List<PriceListItem> = emptyList(),
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class GeoZone(
    val uid: String = "",
    val name: String,
    /** Members: pincodes, ranges ("560001-560010"), or state codes. */
    val members: List<String> = emptyList(),
    val active: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

/** Result of resolving an effective unit price for a product+quantity in a channel/segment. */
data class PriceResolution(
    val effectiveUnitPrice: Double,
    val currency: String,
    val source: PriceSource,
    val matchedPriceListUid: String? = null,
    val appliedTierMinQty: Double? = null,
    val belowMoq: Boolean = false,
)
