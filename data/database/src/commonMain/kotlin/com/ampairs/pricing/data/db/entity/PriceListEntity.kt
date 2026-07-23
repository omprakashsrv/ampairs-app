package com.ampairs.pricing.data.db.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * Room entity for a price-list header. Items live in PriceListItemEntity (their own `/sync` feed).
 * `attribute_predicates_json` stores the predicate list as JSON text (vendor-portable).
 */
@Entity(
    tableName = "price_lists",
    indices = [
        Index(value = ["id"], unique = true, name = "price_list_id_idx"),
        Index(value = ["channel"], name = "price_list_channel_idx"),
        Index(value = ["customer_group_id"], name = "price_list_group_idx"),
    ]
)
data class PriceListEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "ref_id") val refId: String? = null,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "channel") val channel: String,
    @ColumnInfo(name = "customer_group_id") val customerGroupId: String? = null,
    @ColumnInfo(name = "customer_type") val customerType: String? = null,
    @ColumnInfo(name = "customer_id") val customerId: String? = null,
    @ColumnInfo(name = "brand_id") val brandId: String? = null,
    @ColumnInfo(name = "category_id") val categoryId: String? = null,
    @ColumnInfo(name = "product_group_id") val productGroupId: String? = null,
    @ColumnInfo(name = "geo_zone_id") val geoZoneId: String? = null,
    @ColumnInfo(name = "attribute_predicates_json") val attributePredicatesJson: String? = null,
    @ColumnInfo(name = "currency") val currency: String = "INR",
    @ColumnInfo(name = "priority") val priority: Int = 0,
    @ColumnInfo(name = "status") val status: String = "DRAFT",
    @ColumnInfo(name = "starts_at") val startsAt: String? = null,
    @ColumnInfo(name = "ends_at") val endsAt: String? = null,
    @ColumnInfo(name = "active") val active: Boolean = true,
    @ColumnInfo(name = "synced") val synced: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: String? = null,
    @ColumnInfo(name = "updated_at") val updatedAt: String? = null,
)
