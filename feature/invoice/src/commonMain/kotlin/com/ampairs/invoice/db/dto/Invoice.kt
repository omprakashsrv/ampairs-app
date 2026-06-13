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
    val customerId: String,
    val customerName: String,
    val customerGst: String,
    val customerPhone: String?,
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
    val discount: List<Discount>?,
    val synced: Boolean = true,
    val orderRefId: String? = null,
)

fun InvoiceEntity.asDomainModel(): Invoice {
    return Invoice(
        id = this.id,
        invoiceNumber = this.invoice_number,
        invoiceDate = this.invoice_date,
        customerId = this.customer_id,
        customerName = this.customer_name,
        customerGst = this.customer_gst,
        customerPhone = this.customer_phone,
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
        discount = this.discount?.let { Json.decodeFromString(it) },
        synced = this.synced == 1L,
        orderRefId = this.order_ref_id,
    )
}

fun List<InvoiceApiModel>.asDatabaseModel(): List<InvoiceEntity> {
    return map { invoiceApiModel ->
        InvoiceEntity(
            seq_id = 0,
            id = invoiceApiModel.id,
            invoice_number = invoiceApiModel.invoiceNumber,
            invoice_date = invoiceApiModel.invoiceDate,
            customer_id = invoiceApiModel.customerId,
            customer_name = invoiceApiModel.customerName,
            customer_gst = invoiceApiModel.customerGst,
            customer_phone = invoiceApiModel.customerPhone,
            seller_name = invoiceApiModel.sellerName,
            seller_address = invoiceApiModel.sellerAddress,
            seller_gst = invoiceApiModel.sellerGst,
            place_of_supply = invoiceApiModel.placeOfSupply,
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