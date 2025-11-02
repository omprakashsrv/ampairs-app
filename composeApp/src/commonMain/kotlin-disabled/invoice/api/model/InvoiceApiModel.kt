package com.ampairs.invoice.api.model

import com.ampairs.common.model.DateTimeAdapter
import com.ampairs.invoice.db.entity.InvoiceEntity
import com.ampairs.invoice.domain.Address
import com.ampairs.invoice.domain.Discount
import com.ampairs.invoice.domain.Invoice
import com.ampairs.invoice.domain.InvoiceStatus
import com.ampairs.invoice.domain.toApiModel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.ExperimentalTime

@Serializable
data class InvoiceApiModel(
    @SerialName("id") val id: String = "",
    @SerialName("invoice_date") val invoiceDate: String = "",
    @SerialName("invoice_number") val invoiceNumber: String = "",
    @SerialName("order_ref_id") val order_ref_id: String? = null,
    @SerialName("from_customer_id") var fromCustomerId: String = "",
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
    @SerialName("status") var status: InvoiceStatus = InvoiceStatus.DRAFT,
    @SerialName("total_items") var totalItems: Int = 0,
    @SerialName("active") var active: Boolean = true,
    @SerialName("last_updated") var lastUpdated: Long = 0,
    @SerialName("soft_deleted") var softDeleted: Boolean = false,
    @SerialName("total_quantity") var totalQuantity: Double = 0.0,
    @SerialName("billing_address") var billingAddress: Address? = null,
    @SerialName("shipping_address") var shippingAddress: Address? = null,
    @SerialName("discount") var discount: List<Discount>? = null,
    @SerialName("invoice_items") var invoiceItems: List<InvoiceItemApiModel> = arrayListOf(),
    @SerialName("tax_infos") val taxInfoApiModels: List<TaxInfoApiModel>? = null,
)

@OptIn(ExperimentalTime::class)
fun Invoice.toApiModel(): InvoiceApiModel {
    return InvoiceApiModel(
        id = this.id,
        invoiceDate = DateTimeAdapter.toDateTimeString(this.invoiceDate),
        invoiceNumber = this.invoiceNumber ?: "",
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
        invoiceItems = this.items.toApiModel(this),
        discount = this.discount
    )
}

fun InvoiceApiModel.toInvoiceDatabaseModel(): InvoiceEntity {
    return InvoiceEntity(
        seq_id = 0,
        id = this.id,
        invoice_number = this.invoiceNumber,
        invoice_date = this.invoiceDate,
        from_customer_id = this.fromCustomerId,
        to_customer_id = this.toCustomerId,
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
        order_ref_id = this.order_ref_id,
        discount = this.discount?.let { Json.encodeToString(this.discount) }
    )
}