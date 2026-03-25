package com.ampairs.invoice.db.dto

import com.ampairs.invoice.api.model.InvoiceApiModel
import com.ampairs.invoice.db.entity.InvoiceEntity
import com.ampairs.invoice.db.entity.InvoiceItemEntity
import com.ampairs.invoice.domain.Discount
import kotlinx.serialization.json.Json


data class Invoice(
    val id: String,
    val invoiceNumber: String,
    val invoiceDate: String,
    val status: String,
    val fromCustomerId: String,
    val fromCustomerName: String,
    val toCustomerName: String,
    val fromCustomerGst: String,
    val toCustomerGst: String,
    val toCustomerId: String,
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

fun InvoiceEntity.asDomainModel(): Invoice {
    return Invoice(
        id = this.id,
        invoiceNumber = this.invoice_number,
        invoiceDate = this.invoice_date,
        fromCustomerId = this.from_customer_id,
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

fun List<InvoiceApiModel>.asDatabaseModel(): List<InvoiceEntity> {
    return map { invoiceApiModel ->
        InvoiceEntity(
            seq_id = 0,
            id = invoiceApiModel.id,
            invoice_number = invoiceApiModel.invoiceNumber,
            invoice_date = invoiceApiModel.invoiceDate,
            from_customer_id = invoiceApiModel.fromCustomerId,
            from_customer_name = invoiceApiModel.fromCustomerName,
            from_customer_gst = invoiceApiModel.fromCustomerGst,
            to_customer_id = invoiceApiModel.toCustomerId,
            to_customer_name = invoiceApiModel.toCustomerName,
            to_customer_gst = invoiceApiModel.toCustomerGst,
            total_cost = invoiceApiModel.totalCost,
            base_price = invoiceApiModel.basePrice,
            total_tax = invoiceApiModel.totalTax,
            status = invoiceApiModel.status.name,
            total_items = invoiceApiModel.totalItems.toLong(),
            active = if (invoiceApiModel.active) 1L else 0,
            soft_deleted = if (invoiceApiModel.softDeleted) 1L else 0,
            total_quantity = invoiceApiModel.totalQuantity,
            billing_address = if (invoiceApiModel.billingAddress != null) Json.encodeToString(
                invoiceApiModel.billingAddress
            ) else null,
            shipping_address = if (invoiceApiModel.shippingAddress != null) Json.encodeToString(
                invoiceApiModel.shippingAddress
            ) else null,
            tax_info = Json.encodeToString(invoiceApiModel.taxInfoApiModels),
            last_updated = invoiceApiModel.lastUpdated,
            synced = 1,
            created_by = invoiceApiModel.created_by,
            updated_by = invoiceApiModel.updated_by,
            order_ref_id = invoiceApiModel.order_ref_id,
            discount = invoiceApiModel.discount?.let { Json.encodeToString(it) }
        )
    }
}


fun List<InvoiceApiModel>.asItemDatabaseModel(): List<InvoiceItemEntity> {
    val invoiceItems = mutableListOf<InvoiceItemEntity>()
    this.forEach { invoiceApiModel ->
        invoiceItems.addAll(invoiceApiModel.invoiceItems.map { item ->
            InvoiceItemEntity(
                seq_id = 0,
                id = item.id,
                total_cost = item.totalCost,
                base_price = item.basePrice,
                total_tax = item.totalTax,
                active = if (item.active) 1L else 0,
                soft_deleted = if (item.softDeleted) 1L else 0,
                tax_info = Json.encodeToString(item.taxInfoApiModels),
                description = item.description,
                mrp = item.mrp,
                dp = item.dp,
                selling_price = item.price,
                product_price = item.productPrice,
                item_no = 0L,
                invoice_id = item.invoiceId,
                product_id = item.productId,
                quantity = item.quantity,
                tax_code = item.taxCode,
                discount = item.discount?.let { Json.encodeToString(it) }
            )
        })
    }
    return invoiceItems
}