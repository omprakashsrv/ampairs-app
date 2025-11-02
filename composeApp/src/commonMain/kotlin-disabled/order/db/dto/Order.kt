package com.ampairs.order.db.dto

import com.ampairs.order.api.model.OrderApiModel
import com.ampairs.order.db.entity.OrderEntity
import com.ampairs.order.db.entity.OrderItemEntity
import com.ampairs.order.domain.Discount
import kotlinx.serialization.json.Json


data class Order(
    val id: String,
    val orderNumber: String,
    val orderDate: String,
    val status: String,
    val fromCustomerId: String,
    val fromCustomerName: String,
    val toCustomerName: String,
    val fromCustomerGst: String,
    val toCustomerGst: String,
    val toCustomerId: String,
    val invoiceRefId: String?,
    val totalCost: Double,
    val totalTax: Double,
    val totalItems: Long,
    val totalQuantity: Double,
    val basePrice: Double,
    val taxInfo: String?,
    val billingAddress: String?,
    val shippingAddress: String?,
    val active: Boolean,
    val softDeleted: Boolean,
    val discount: List<Discount>?
)

fun OrderEntity.asDomainModel(): Order {
    return Order(
        id = this.id,
        orderNumber = this.order_number,
        orderDate = this.order_date,
        fromCustomerId = this.from_customer_id,
        invoiceRefId = this.invoice_ref_id,
        fromCustomerName = this.from_customer_name,
        fromCustomerGst = this.from_customer_gst,
        toCustomerId = this.to_customer_id,
        toCustomerName = this.to_customer_name,
        toCustomerGst = this.to_customer_gst,
        totalCost = this.total_cost,
        basePrice = this.base_price,
        totalTax = this.total_tax,
        status = this.status,
        totalItems = this.total_items,
        active = this.active == 1L,
        softDeleted = this.soft_deleted == 1L,
        totalQuantity = this.total_quantity,
        taxInfo = this.tax_info,
        shippingAddress = this.shipping_address,
        billingAddress = this.billing_address,
        discount = this.discount?.let { Json.decodeFromString(it) }
    )
}

fun List<OrderApiModel>.asDatabaseModel(): List<OrderEntity> {
    return map { orderApiModel ->
        OrderEntity(
            seq_id = 0,
            id = orderApiModel.id,
            order_number = orderApiModel.orderNumber,
            order_date = orderApiModel.orderDate,
            from_customer_id = orderApiModel.fromCustomerId,
            from_customer_name = orderApiModel.fromCustomerName,
            invoice_ref_id = orderApiModel.invoiceRefId,
            from_customer_gst = orderApiModel.fromCustomerGst,
            to_customer_id = orderApiModel.toCustomerId,
            to_customer_name = orderApiModel.toCustomerName,
            to_customer_gst = orderApiModel.toCustomerGst,
            total_cost = orderApiModel.totalCost,
            base_price = orderApiModel.basePrice,
            total_tax = orderApiModel.totalTax,
            status = orderApiModel.status.name,
            total_items = orderApiModel.totalItems.toLong(),
            active = if (orderApiModel.active) 1L else 0,
            soft_deleted = if (orderApiModel.softDeleted) 1L else 0,
            total_quantity = orderApiModel.totalQuantity,
            billing_address = if (orderApiModel.billingAddress != null) Json.encodeToString(
                orderApiModel.billingAddress
            ) else null,
            shipping_address = if (orderApiModel.billingAddress != null) Json.encodeToString(
                orderApiModel.shippingAddress
            ) else null,
            tax_info = Json.encodeToString(orderApiModel.taxInfoApiModels),
            last_updated = orderApiModel.lastUpdated,
            synced = 1,
            created_by = orderApiModel.created_by,
            updated_by = orderApiModel.updated_by,
            discount = orderApiModel.discount?.let { Json.encodeToString(it) }
        )
    }
}


fun List<OrderApiModel>.asItemDatabaseModel(): List<OrderItemEntity> {
    val orderItems = mutableListOf<OrderItemEntity>()
    this.forEach { orderApiModel ->
        orderItems.addAll(orderApiModel.orderItems.map { item ->
            OrderItemEntity(
                seq_id = 0,
                id = item.id,
                total_cost = item.totalCost,
                base_price = item.basePrice,
                total_tax = item.totalTax,
                active = if (item.active) 1 else 0,
                soft_deleted = if (item.softDeleted) 1 else 0,
                tax_info = Json.encodeToString(item.taxInfoApiModels),
                description = item.description,
                mrp = item.mrp,
                dp = item.dp,
                selling_price = item.price,
                product_price = item.productPrice,
                item_no = 0,
                order_id = item.orderId,
                product_id = item.productId,
                quantity = item.quantity,
                tax_code = item.taxCode,
                discount = item.discount?.let { Json.encodeToString(it) }
            )
        })
    }
    return orderItems
}