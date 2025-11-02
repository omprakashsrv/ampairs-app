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
    @SerialName("from_customer_id") var fromCustomerId: String = "",
    @SerialName("invoice_ref_id") var invoiceRefId: String? = "",
    @SerialName("from_customer_name") var fromCustomerName: String = "",
    @SerialName("to_customer_id") var toCustomerId: String = "",
    @SerialName("to_customer_name") var toCustomerName: String = "",
    @SerialName("from_customer_gst") var fromCustomerGst: String = "",
    @SerialName("to_customer_gst") var toCustomerGst: String = "",
    @SerialName("created_by") var created_by: String = "",
    @SerialName("updated_by") var updated_by: String = "",
    @SerialName("total_cost") var totalCost: Double = 0.0,
    @SerialName("base_price") var basePrice: Double = 0.0,
    @SerialName("total_tax") var totalTax: Double = 0.0,
    @SerialName("status") var status: OrderStatus = OrderStatus.DRAFT,
    @SerialName("total_items") var totalItems: Int = 0,
    @SerialName("active") var active: Boolean = true,
    @SerialName("last_updated") var lastUpdated: Long = 0,
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
        orderDate = DateTimeAdapter.toDateTimeString(this.orderDate),
        orderNumber = this.orderNumber ?: "",
        fromCustomerId = this.fromCustomer?.id ?: "",
        fromCustomerName = this.fromCustomer?.name ?: "",
        toCustomerId = this.toCustomer?.id ?: "",
        toCustomerName = this.toCustomer?.name ?: "",
        fromCustomerGst = this.fromCustomer?.gstin ?: "",
        toCustomerGst = this.toCustomer?.gstin ?: "",
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
        from_customer_id = this.fromCustomerId,
        to_customer_id = this.toCustomerId,
        invoice_ref_id = this.invoiceRefId,
        total_cost = this.totalCost,
        base_price = this.basePrice,
        total_items = this.totalItems.toLong(),
        total_quantity = this.totalQuantity,
        status = this.status.name,
        from_customer_name = this.fromCustomerName,
        to_customer_name = this.toCustomerName,
        from_customer_gst = this.fromCustomerGst,
        to_customer_gst = this.toCustomerGst,
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