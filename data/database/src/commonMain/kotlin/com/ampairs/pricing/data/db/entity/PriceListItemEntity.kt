package com.ampairs.pricing.data.db.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * Room entity for a price-list item. Money is stored in minor units (matches backend).
 * `tiers_json` holds the slab list (minQty + unitPriceMinor) as JSON text.
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
    @ColumnInfo(name = "ref_id") val refId: String? = null,
    @ColumnInfo(name = "price_list_id") val priceListId: String,
    @ColumnInfo(name = "product_id") val productId: String,
    @ColumnInfo(name = "variant_sku") val variantSku: String? = null,
    @ColumnInfo(name = "unit_price_minor") val unitPriceMinor: Long,
    @ColumnInfo(name = "currency") val currency: String? = null,
    @ColumnInfo(name = "moq") val moq: Double? = null,
    @ColumnInfo(name = "tiers_json") val tiersJson: String? = null,
    @ColumnInfo(name = "effective_from") val effectiveFrom: String? = null,
    @ColumnInfo(name = "effective_to") val effectiveTo: String? = null,
    @ColumnInfo(name = "active") val active: Boolean = true,
    @ColumnInfo(name = "synced") val synced: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: String? = null,
    @ColumnInfo(name = "updated_at") val updatedAt: String? = null,
)
