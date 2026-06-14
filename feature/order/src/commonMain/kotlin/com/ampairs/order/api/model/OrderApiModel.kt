package com.ampairs.order.api.model

import com.ampairs.common.model.DateTimeAdapter
import com.ampairs.order.db.entity.OrderEntity
import com.ampairs.order.domain.Address
import com.ampairs.order.domain.Discount
import com.ampairs.order.domain.Order
import com.ampairs.order.domain.OrderStatus
import com.ampairs.order.domain.toApiModel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.ExperimentalTime

@Serializable
data class OrderApiModel(
    @SerialName("id") val id: String = "",
    @SerialName("order_date") val orderDate: String = "",
    @SerialName("order_number") val orderNumber: String = "",
    @SerialName("invoice_ref_id") var invoiceRefId: String? = "",
    @SerialName("customer_id") var customerId: String = "",
    @SerialName("customer_name") var customerName: String = "",
    @SerialName("customer_phone") var customerPhone: String? = null,
    @SerialName("customer_gst") var customerGst: String = "",
    @SerialName("seller_name") var sellerName: String? = null,
    @SerialName("seller_address") var sellerAddress: String? = null,
    @SerialName("seller_gst") var sellerGst: String? = null,
    @SerialName("place_of_supply") var placeOfSupply: String? = null,
    @SerialName("seller_place_of_supply") var sellerPlaceOfSupply: String? = null,
    @SerialName("created_by") var created_by: String = "",
    @SerialName("updated_by") var updated_by: String = "",
    @SerialName("total_cost") var totalCost: Double = 0.0,
    @SerialName("base_price") var basePrice: Double = 0.0,
    @SerialName("total_tax") var totalTax: Double = 0.0,
    @SerialName("status") var status: OrderStatus = OrderStatus.DRAFT,
    @SerialName("total_items") var totalItems: Int = 0,
    @SerialName("active") var active: Boolean = true,
    @SerialName("last_updated") var lastUpdated: Long = 0,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("soft_deleted") var softDeleted: Boolean = false,
    @SerialName("total_quantity") var totalQuantity: Double = 0.0,
    @SerialName("billing_address") var billingAddress: Address? = null,
    @SerialName("shipping_address") var shippingAddress: Address? = null,
    @SerialName("discount") var discount: List<Discount>? = null,
    @SerialName("order_items") var orderItems: List<OrderItemApiModel> = arrayListOf(),
    @SerialName("tax_infos") val taxInfoApiModels: List<TaxInfoApiModel>? = null,
)

@OptIn(ExperimentalTime::class)
fun Order.toApiModel(): OrderApiModel {
    return OrderApiModel(
        id = this.id,
        orderDate = this.orderDate.toString(), // ISO-8601 — backend order_date is an Instant
        orderNumber = this.orderNumber ?: "",
        customerId = this.customer?.uid ?: "",
        customerName = this.customer?.name ?: "",
        customerPhone = this.customer?.phone,
        customerGst = this.customer?.gstNumber ?: "",
        sellerName = this.sellerName,
        sellerAddress = this.sellerAddress,
        sellerGst = this.sellerGst,
        placeOfSupply = this.placeOfSupply,
        sellerPlaceOfSupply = this.sellerPlaceOfSupply,
        totalCost = this.totalCost,
        basePrice = this.basePrice,
        totalTax = this.totalTax,
        status = this.status,
        totalItems = this.totalItems,
        totalQuantity = this.totalQuantity,
        billingAddress = null,
        shippingAddress = null,
        taxInfoApiModels = if (this.taxInfos != null) this.taxInfos?.toApiModel() else null,
        orderItems = this.items.toApiModel(this),
        discount = this.discount,
        invoiceRefId = this.invoiceRefId
    )
}

fun OrderApiModel.toOrderDatabaseModel(): OrderEntity {
    return OrderEntity(
        seq_id = 0,
        id = this.id,
        order_number = this.orderNumber,
        order_date = this.orderDate,
        customer_id = this.customerId,
        customer_phone = this.customerPhone,
        seller_name = this.sellerName,
        seller_address = this.sellerAddress,
        seller_gst = this.sellerGst,
        place_of_supply = this.placeOfSupply,
        seller_place_of_supply = this.sellerPlaceOfSupply,
        invoice_ref_id = this.invoiceRefId,
        total_cost = this.totalCost,
        base_price = this.basePrice,
        total_items = this.totalItems.toLong(),
        total_quantity = this.totalQuantity,
        status = this.status.name,
        customer_name = this.customerName,
        customer_gst = this.customerGst,
        billing_address = if (this.billingAddress != null) Json.encodeToString(this.billingAddress) else null,
        shipping_address = if (this.shippingAddress != null) Json.encodeToString(this.shippingAddress) else null,
        tax_info = Json.encodeToString(this.taxInfoApiModels),
        total_tax = this.totalTax,
        active = if (this.active) 1 else 0,
        soft_deleted = if (this.softDeleted) 1 else 0,
        last_updated = this.lastUpdated,
        synced = 1,
        created_by = this.created_by,
        updated_by = this.updated_by,
        discount = this.discount?.let { Json.encodeToString(this.discount) }
    )
}