package com.ampairs.pricing.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ampairs.pricing.domain.model.PriceListItem
import com.ampairs.pricing.domain.model.PriceTier
import kotlinx.serialization.builtins.ListSerializer

/**
 * Room entity for a price-list item. `tiers_json` holds the slab list as JSON text.
 * Indexed by price_list_id (aggregate assembly) and product_id (resolution lookup).
 */
@Entity(
    tableName = "price_list_items",
    indices = [
        Index(value = ["id"], unique = true, name = "price_list_item_id_idx"),
        Index(value = ["price_list_id"], name = "price_list_item_list_idx"),
        Index(value = ["product_id"], name = "price_list_item_product_idx"),
    ]
)
data class PriceListItemEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "price_list_id") val priceListId: String,
    @ColumnInfo(name = "product_id") val productId: String,
    @ColumnInfo(name = "variant_sku") val variantSku: String? = null,
    @ColumnInfo(name = "unit_price") val unitPrice: Double,
    @ColumnInfo(name = "moq") val moq: Double? = null,
    @ColumnInfo(name = "tiers_json") val tiersJson: String? = null,
    @ColumnInfo(name = "active") val active: Boolean = true,
    @ColumnInfo(name = "synced") val synced: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: String? = null,
    @ColumnInfo(name = "updated_at") val updatedAt: String? = null,
)

private fun parseTiers(json: String?): List<PriceTier> =
    if (json.isNullOrBlank()) emptyList()
    else runCatching { PricingJson.decodeFromString(ListSerializer(PriceTier.serializer()), json) }
        .getOrDefault(emptyList())

private fun encodeTiers(tiers: List<PriceTier>): String? =
    if (tiers.isEmpty()) null
    else PricingJson.encodeToString(ListSerializer(PriceTier.serializer()), tiers)

fun PriceListItemEntity.toPriceListItem(): PriceListItem = PriceListItem(
    uid = id,
    priceListId = priceListId,
    productId = productId,
    variantSku = variantSku,
    unitPrice = unitPrice,
    moq = moq,
    tiers = parseTiers(tiersJson),
    active = active,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun PriceListItem.toEntity(priceListId: String = this.priceListId): PriceListItemEntity = PriceListItemEntity(
    id = uid,
    priceListId = priceListId,
    productId = productId,
    variantSku = variantSku,
    unitPrice = unitPrice,
    moq = moq,
    tiersJson = encodeTiers(tiers),
    active = active,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
