package com.ampairs.product.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for an effective-dated standard purchase cost (the cost-side mirror of
 * [PriceListItemEntity]). One row per cost version: a product (optionally a specific variant) has a
 * [costPrice] effective from [effectiveFrom] (inclusive) up to [effectiveTo] (exclusive); a null
 * bound is open on that side. Money is a plain [Double] in major units (matches the product/purchase
 * modules, which use Double — NOT minor units).
 */
@Entity(
    tableName = "product_standard_cost",
    indices = [
        Index(value = ["id"], unique = true, name = "product_standard_cost_id_idx"),
        Index(value = ["product_id"], name = "product_standard_cost_product_idx"),
    ]
)
data class ProductStandardCostEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "ref_id") val refId: String? = null,
    @ColumnInfo(name = "product_id") val productId: String,
    @ColumnInfo(name = "variant_sku") val variantSku: String? = null,
    @ColumnInfo(name = "cost_price") val costPrice: Double,
    @ColumnInfo(name = "effective_from") val effectiveFrom: String? = null,
    @ColumnInfo(name = "effective_to") val effectiveTo: String? = null,
    @ColumnInfo(name = "active") val active: Boolean = true,
    @ColumnInfo(name = "synced") val synced: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: String? = null,
    @ColumnInfo(name = "updated_at") val updatedAt: String? = null,
)

/**
 * Plain domain model for a standard-cost version, used by the resolver's pure effective-window logic
 * (no Room/DI types so it stays testable). Mirrors [com.ampairs.pricing.domain.model.PriceListItem]'s
 * effective-dating fields.
 */
data class ProductStandardCost(
    val uid: String,
    val refId: String? = null,
    val productId: String,
    val variantSku: String? = null,
    val costPrice: Double,
    val effectiveFrom: String? = null,
    val effectiveTo: String? = null,
    val active: Boolean = true,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

fun ProductStandardCostEntity.toDomain(): ProductStandardCost = ProductStandardCost(
    uid = id,
    refId = refId,
    productId = productId,
    variantSku = variantSku,
    costPrice = costPrice,
    effectiveFrom = effectiveFrom,
    effectiveTo = effectiveTo,
    active = active,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun ProductStandardCost.toEntity(): ProductStandardCostEntity = ProductStandardCostEntity(
    id = uid,
    refId = refId,
    productId = productId,
    variantSku = variantSku,
    costPrice = costPrice,
    effectiveFrom = effectiveFrom,
    effectiveTo = effectiveTo,
    active = active,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
