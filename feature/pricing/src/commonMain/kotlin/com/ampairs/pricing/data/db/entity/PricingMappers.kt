package com.ampairs.pricing.data.db.entity

import com.ampairs.pricing.domain.model.AttributePredicate
import com.ampairs.pricing.domain.model.GeoZone
import com.ampairs.pricing.domain.model.GeoZoneMembers
import com.ampairs.pricing.domain.model.Offer
import com.ampairs.pricing.domain.model.OfferStatus
import com.ampairs.pricing.domain.model.PriceList
import com.ampairs.pricing.domain.model.PriceListItem
import com.ampairs.pricing.domain.model.PriceListStatus
import com.ampairs.pricing.domain.model.PriceTier
import com.ampairs.pricing.domain.model.SalesChannel
import kotlinx.serialization.builtins.ListSerializer

// Entity <-> domain mappers. The @Entity classes live in :data:database (same package); these
// mappers stay in the feature module because they reference the pricing domain models/enums and
// the feature-internal `PricingJson`.

// --- GeoZone ---
private fun parseMembers(json: String?): GeoZoneMembers =
    if (json.isNullOrBlank()) GeoZoneMembers()
    else runCatching { PricingJson.decodeFromString(GeoZoneMembers.serializer(), json) }
        .getOrDefault(GeoZoneMembers())

private fun encodeMembers(members: GeoZoneMembers): String =
    PricingJson.encodeToString(GeoZoneMembers.serializer(), members)

fun GeoZoneEntity.toGeoZone(): GeoZone = GeoZone(
    uid = id,
    refId = refId,
    name = name,
    members = parseMembers(membersJson),
    active = active,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun GeoZone.toEntity(): GeoZoneEntity = GeoZoneEntity(
    id = uid,
    refId = refId,
    name = name,
    membersJson = encodeMembers(members),
    active = active,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

// --- Offer ---
fun OfferEntity.toOffer(): Offer =
    runCatching { PricingJson.decodeFromString(Offer.serializer(), payloadJson) }
        .getOrElse {
            Offer(
                uid = id,
                name = name,
                channel = runCatching { SalesChannel.valueOf(channel) }.getOrDefault(SalesChannel.RETAIL),
                status = runCatching { OfferStatus.valueOf(status) }.getOrDefault(OfferStatus.DRAFT),
                active = active,
                updatedAt = updatedAt,
            )
        }

fun Offer.toEntity(): OfferEntity = OfferEntity(
    id = uid,
    name = name,
    channel = channel.name,
    status = status.name,
    active = active,
    updatedAt = updatedAt,
    payloadJson = PricingJson.encodeToString(Offer.serializer(), this),
)

// --- PriceList ---
private fun parseChannel(value: String): SalesChannel =
    SalesChannel.entries.firstOrNull { it.name == value } ?: SalesChannel.RETAIL

private fun parseStatus(value: String): PriceListStatus =
    PriceListStatus.entries.firstOrNull { it.name == value } ?: PriceListStatus.DRAFT

private fun parsePredicates(json: String?): List<AttributePredicate> =
    if (json.isNullOrBlank()) emptyList()
    else runCatching { PricingJson.decodeFromString(ListSerializer(AttributePredicate.serializer()), json) }
        .getOrDefault(emptyList())

private fun encodePredicates(predicates: List<AttributePredicate>): String? =
    if (predicates.isEmpty()) null
    else PricingJson.encodeToString(ListSerializer(AttributePredicate.serializer()), predicates)

fun PriceListEntity.toPriceList(): PriceList = PriceList(
    uid = id,
    refId = refId,
    name = name,
    channel = parseChannel(channel),
    customerGroupId = customerGroupId,
    customerType = customerType,
    customerId = customerId,
    brandId = brandId,
    categoryId = categoryId,
    productGroupId = productGroupId,
    geoZoneId = geoZoneId,
    attributePredicates = parsePredicates(attributePredicatesJson),
    currency = currency,
    priority = priority,
    status = parseStatus(status),
    startsAt = startsAt,
    endsAt = endsAt,
    active = active,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun PriceList.toEntity(): PriceListEntity = PriceListEntity(
    id = uid,
    refId = refId,
    name = name,
    channel = channel.name,
    customerGroupId = customerGroupId,
    customerType = customerType,
    customerId = customerId,
    brandId = brandId,
    categoryId = categoryId,
    productGroupId = productGroupId,
    geoZoneId = geoZoneId,
    attributePredicatesJson = encodePredicates(attributePredicates),
    currency = currency,
    priority = priority,
    status = status.name,
    startsAt = startsAt,
    endsAt = endsAt,
    active = active,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

// --- PriceListItem ---
@kotlinx.serialization.Serializable
private data class TierJson(val minQty: Double, val unitPriceMinor: Long)

private fun parseTiers(json: String?): List<PriceTier> =
    if (json.isNullOrBlank()) emptyList()
    else runCatching {
        PricingJson.decodeFromString(ListSerializer(TierJson.serializer()), json)
            .map { PriceTier(it.minQty, it.unitPriceMinor) }
    }.getOrDefault(emptyList())

private fun encodeTiers(tiers: List<PriceTier>): String? =
    if (tiers.isEmpty()) null
    else PricingJson.encodeToString(
        ListSerializer(TierJson.serializer()),
        tiers.map { TierJson(it.minQty, it.unitPriceMinor) },
    )

fun PriceListItemEntity.toPriceListItem(): PriceListItem = PriceListItem(
    uid = id,
    refId = refId,
    priceListId = priceListId,
    productId = productId,
    variantSku = variantSku,
    unitPriceMinor = unitPriceMinor,
    currency = currency,
    moq = moq,
    tiers = parseTiers(tiersJson),
    effectiveFrom = effectiveFrom,
    effectiveTo = effectiveTo,
    active = active,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun PriceListItem.toEntity(priceListId: String = this.priceListId): PriceListItemEntity = PriceListItemEntity(
    id = uid,
    refId = refId,
    priceListId = priceListId,
    productId = productId,
    variantSku = variantSku,
    unitPriceMinor = unitPriceMinor,
    currency = currency,
    moq = moq,
    tiersJson = encodeTiers(tiers),
    effectiveFrom = effectiveFrom,
    effectiveTo = effectiveTo,
    active = active,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
