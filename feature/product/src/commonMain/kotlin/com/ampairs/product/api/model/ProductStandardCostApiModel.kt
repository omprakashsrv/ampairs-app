package com.ampairs.product.api.model

import com.ampairs.product.db.entity.ProductStandardCostEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire model for the canonical `/product/v1/standard-costs/sync` resource. Global snake_case via
 * [SerialName]. `cost_price` is major units (Double); `active = false` carries an in-band soft-delete
 * on push (and the pull feed includes soft-deleted rows).
 */
@Serializable
data class ProductStandardCostApiModel(
    @SerialName("uid") val uid: String,
    @SerialName("ref_id") val refId: String? = null,
    @SerialName("product_id") val productId: String,
    @SerialName("variant_sku") val variantSku: String? = null,
    @SerialName("cost_price") val costPrice: Double,
    @SerialName("effective_from") val effectiveFrom: String? = null,
    @SerialName("effective_to") val effectiveTo: String? = null,
    @SerialName("active") val active: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

fun ProductStandardCostApiModel.toEntity(): ProductStandardCostEntity = ProductStandardCostEntity(
    id = uid,
    refId = refId,
    productId = productId,
    variantSku = variantSku,
    costPrice = costPrice,
    effectiveFrom = effectiveFrom,
    effectiveTo = effectiveTo,
    active = active,
    synced = true, // arrived from the server → already in sync
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun ProductStandardCostEntity.toApiModel(): ProductStandardCostApiModel = ProductStandardCostApiModel(
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

fun List<ProductStandardCostApiModel>.asStandardCostEntities(): List<ProductStandardCostEntity> =
    map { it.toEntity() }

fun List<ProductStandardCostEntity>.asStandardCostApiModels(): List<ProductStandardCostApiModel> =
    map { it.toApiModel() }
