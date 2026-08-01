package com.ampairs.order.api.model

import com.ampairs.order.domain.Discount
import com.ampairs.order.domain.Order
import com.ampairs.order.domain.OrderItem
import com.ampairs.order.domain.toApiModel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class OrderItemApiModel(
    @SerialName("id") var id: String = "",
    @SerialName("description") var description: String = "",
    @SerialName("quantity") var quantity: Double = 0.0,
    @SerialName("price") var price: Double = 0.0,
    @SerialName("product_price") var productPrice: Double = 0.0,
    @SerialName("mrp") var mrp: Double = 0.0,
    @SerialName("dp") var dp: Double = 0.0,
    @SerialName("total_cost") var totalCost: Double = 0.0,
    @SerialName("total_tax") var totalTax: Double = 0.0,
    @SerialName("base_price") var basePrice: Double = 0.0,
    @SerialName("order_id") var orderId: String = "",
    @SerialName("product_id") var productId: String = "",
    @SerialName("tax_code") var taxCode: String = "",
    @SerialName("active") var active: Boolean,
    @SerialName("soft_deleted") var softDeleted: Boolean,
    @SerialName("discount") var discount: List<Discount>? = null,
    @SerialName("tax_infos") val taxInfoApiModels: List<TaxInfoApiModel> = arrayListOf(),
    // spec 010 FR-014: unit of measure + base-unit quantity, and selected variant. Must ride the
    // /sync wire both ways — omitting them made the pull wipe the local unit after every sync.
    @SerialName("unit_id") var unitId: String = "",
    @SerialName("base_quantity") var baseQuantity: Double = 0.0,
    @SerialName("variant_sku") var variantSku: String? = null,
    // 009 pricing snapshot — persisted verbatim by the backend (no re-resolution)
    @SerialName("resolved_unit_price_minor") var resolvedUnitPriceMinor: Long? = null,
    @SerialName("currency") var currency: String? = null,
    @SerialName("price_source") var priceSource: String? = null,
    @SerialName("matched_price_list_uid") var matchedPriceListUid: String? = null,
    @SerialName("applied_tier_min_qty") var appliedTierMinQty: Double? = null,
    @SerialName("below_moq") var belowMoq: Boolean? = null,
)

fun List<OrderItem>.toApiModel(order: Order): List<OrderItemApiModel> {
    return map {
        OrderItemApiModel(
            id = it.id,
            description = it.description,
            quantity = it.quantity,
            price = it.price,
            productPrice = it.productPrice,
            mrp = it.mrp,
            dp = it.dp,
            totalCost = it.totalCost,
            totalTax = it.totalTax,
            basePrice = it.basePrice,
            orderId = order.id,
            // The sync-delegate push builds items from Room (product = null) — fall back to the
            // line's own snapshot or every pushed line would blank product_id/tax_code on the server.
            productId = it.product?.id ?: it.productId ?: "",
            taxCode = it.product?.taxCode ?: it.taxCode,
            taxInfoApiModels = it.taxInfos.toApiModel(),
            active = it.active,
            softDeleted = it.softDeleted,
            discount = it.discount,
            unitId = it.unitId,
            baseQuantity = it.baseQuantity,
            variantSku = it.variantSku,
            resolvedUnitPriceMinor = it.resolvedUnitPriceMinor,
            currency = it.currency,
            priceSource = it.priceSource,
            matchedPriceListUid = it.matchedPriceListUid,
            appliedTierMinQty = it.appliedTierMinQty,
            belowMoq = it.belowMoq,
        )
    }
}