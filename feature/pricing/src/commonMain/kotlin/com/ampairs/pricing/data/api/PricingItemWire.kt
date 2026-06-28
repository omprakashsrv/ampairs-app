package com.ampairs.pricing.data.api

import com.ampairs.pricing.domain.model.MoneyDto
import com.ampairs.pricing.domain.model.PriceListItem
import com.ampairs.pricing.domain.model.PriceTier
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire DTOs for price-list items. The backend item contract is asymmetric:
 *  - PULL ([PriceListItemResponse]) returns money as a `MoneyDto { amount_minor, currency }`.
 *  - PUSH ([PriceListItemPush]) sends money as a bare `unit_price_minor: Long` (currency inherited
 *    from the parent list, server-side).
 */

@Serializable
data class PriceTierResponse(
    @SerialName("min_qty") val minQty: Double,
    @SerialName("unit_price") val unitPrice: MoneyDto,
)

@Serializable
data class PriceListItemResponse(
    val uid: String,
    @SerialName("ref_id") val refId: String? = null,
    @SerialName("price_list_id") val priceListId: String,
    @SerialName("product_id") val productId: String,
    @SerialName("variant_sku") val variantSku: String? = null,
    @SerialName("unit_price") val unitPrice: MoneyDto,
    val moq: Double? = null,
    val tiers: List<PriceTierResponse> = emptyList(),
    @SerialName("effective_from") val effectiveFrom: String? = null,
    @SerialName("effective_to") val effectiveTo: String? = null,
    val active: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class PriceTierPush(
    @SerialName("min_qty") val minQty: Double,
    @SerialName("unit_price_minor") val unitPriceMinor: Long,
)

@Serializable
data class PriceListItemPush(
    val uid: String,
    @SerialName("ref_id") val refId: String? = null,
    @SerialName("price_list_id") val priceListId: String,
    @SerialName("product_id") val productId: String,
    @SerialName("variant_sku") val variantSku: String? = null,
    @SerialName("unit_price_minor") val unitPriceMinor: Long,
    val moq: Double? = null,
    val tiers: List<PriceTierPush> = emptyList(),
    @SerialName("effective_from") val effectiveFrom: String? = null,
    @SerialName("effective_to") val effectiveTo: String? = null,
    val active: Boolean = true,
)

fun PriceListItemResponse.toDomain(): PriceListItem = PriceListItem(
    uid = uid,
    refId = refId,
    priceListId = priceListId,
    productId = productId,
    variantSku = variantSku,
    unitPriceMinor = unitPrice.amountMinor,
    currency = unitPrice.currency,
    moq = moq,
    tiers = tiers.map { PriceTier(minQty = it.minQty, unitPriceMinor = it.unitPrice.amountMinor) },
    effectiveFrom = effectiveFrom,
    effectiveTo = effectiveTo,
    active = active,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun PriceListItem.toPush(): PriceListItemPush = PriceListItemPush(
    uid = uid,
    refId = refId,
    priceListId = priceListId,
    productId = productId,
    variantSku = variantSku,
    unitPriceMinor = unitPriceMinor,
    moq = moq,
    tiers = tiers.map { PriceTierPush(minQty = it.minQty, unitPriceMinor = it.unitPriceMinor) },
    effectiveFrom = effectiveFrom,
    effectiveTo = effectiveTo,
    active = active,
)
