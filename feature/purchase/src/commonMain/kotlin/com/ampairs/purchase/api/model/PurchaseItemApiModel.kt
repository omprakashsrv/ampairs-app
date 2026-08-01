package com.ampairs.purchase.api.model

import com.ampairs.purchase.db.entity.PurchaseItemEntity
import com.ampairs.purchase.domain.Discount
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class PurchaseItemApiModel(
    @SerialName("id") var id: String = "",
    @SerialName("item_no") var itemNo: Int = 0,
    @SerialName("description") var description: String = "",
    @SerialName("quantity") var quantity: Double = 0.0,
    @SerialName("price") var price: Double = 0.0,
    @SerialName("product_price") var productPrice: Double = 0.0,
    @SerialName("mrp") var mrp: Double = 0.0,
    @SerialName("dp") var dp: Double = 0.0,
    @SerialName("total_cost") var totalCost: Double = 0.0,
    @SerialName("total_tax") var totalTax: Double = 0.0,
    @SerialName("base_price") var basePrice: Double = 0.0,
    @SerialName("purchase_id") var purchaseId: String = "",
    @SerialName("product_id") var productId: String = "",
    @SerialName("tax_code") var taxCode: String = "",
    @SerialName("unit_id") var unitId: String = "",
    @SerialName("base_quantity") var baseQuantity: Double = 0.0,
    @SerialName("variant_sku") var variantSku: String? = null,
    @SerialName("active") var active: Boolean = true,
    @SerialName("soft_deleted") var softDeleted: Boolean = false,
    @SerialName("discount") var discount: List<Discount>? = null,
    @SerialName("tax_infos") val taxInfoApiModels: List<TaxInfoApiModel> = arrayListOf(),
)

/** Build wire items from local item rows (sync push). */
fun List<PurchaseItemEntity>.toApiModel(): List<PurchaseItemApiModel> {
    return map { item ->
        PurchaseItemApiModel(
            id = item.id,
            itemNo = item.item_no,
            description = item.description,
            quantity = item.quantity,
            price = item.price,
            productPrice = item.product_price,
            mrp = item.mrp,
            dp = item.dp,
            totalCost = item.total_cost,
            totalTax = item.total_tax,
            basePrice = item.base_price,
            purchaseId = item.purchase_id,
            productId = item.product_id,
            taxCode = item.tax_code,
            unitId = item.unit_id,
            baseQuantity = item.base_quantity,
            variantSku = item.variant_sku,
            active = item.active == 1,
            softDeleted = item.soft_deleted == 1,
            discount = item.discount?.takeIf { it.isNotBlank() }?.let { Json.decodeFromString(it) },
            taxInfoApiModels = item.tax_info?.takeIf { it.isNotBlank() }?.let { Json.decodeFromString(it) }
                ?: emptyList(),
        )
    }
}

/** Map nested wire items into local item rows (sync pull). */
fun List<PurchaseApiModel>.asItemDatabaseModel(): List<PurchaseItemEntity> {
    val items = mutableListOf<PurchaseItemEntity>()
    forEach { api ->
        items.addAll(api.purchaseItems.map { item ->
            PurchaseItemEntity(
                seq_id = 0,
                id = item.id,
                description = item.description,
                product_id = item.productId,
                total_cost = item.totalCost,
                base_price = item.basePrice,
                product_price = item.productPrice,
                total_tax = item.totalTax,
                purchase_id = item.purchaseId.ifBlank { api.id },
                tax_code = item.taxCode,
                quantity = item.quantity,
                item_no = item.itemNo,
                price = item.price,
                mrp = item.mrp,
                dp = item.dp,
                tax_info = Json.encodeToString(item.taxInfoApiModels),
                discount = item.discount?.let { Json.encodeToString(it) },
                active = if (item.active) 1 else 0,
                soft_deleted = if (item.softDeleted) 1 else 0,
                unit_id = item.unitId,
                base_quantity = item.baseQuantity,
                variant_sku = item.variantSku,
            )
        })
    }
    return items
}
