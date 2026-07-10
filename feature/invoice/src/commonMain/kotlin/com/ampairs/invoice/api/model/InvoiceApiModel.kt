package com.ampairs.invoice.api.model

import com.ampairs.common.model.DateTimeAdapter
import com.ampairs.invoice.db.entity.InvoiceEntity
import com.ampairs.invoice.db.entity.InvoiceItemEntity
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
    @SerialName("status") var status: InvoiceStatus = InvoiceStatus.DRAFT,
    @SerialName("total_items") var totalItems: Int = 0,
    @SerialName("active") var active: Boolean = true,
    @SerialName("last_updated") var lastUpdated: Long = 0,
    @SerialName("updated_at") val updatedAt: String? = null,
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
        invoiceDate = this.invoiceDate.toString(), // ISO-8601 — backend invoice_date is an Instant
        invoiceNumber = this.invoiceNumber ?: "",
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
        customer_id = this.customerId,
        customer_phone = this.customerPhone,
        seller_name = this.sellerName,
        seller_address = this.sellerAddress,
        seller_gst = this.sellerGst,
        place_of_supply = this.placeOfSupply,
        seller_place_of_supply = this.sellerPlaceOfSupply,
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
        order_ref_id = this.order_ref_id,
        discount = this.discount?.let { Json.encodeToString(this.discount) }
    )
}

// --- Entity -> API (for offline sync push, spec 010). Reads product_id/tax_code straight off the
// entity, so no product lookup is needed; reverses toInvoiceDatabaseModel. ---

/** Normalize a stored date string ("yyyy-MM-dd HH:mm:ss" or ISO) to an ISO-8601 Instant the backend
 *  (`java.time.Instant`) can parse. The entity stores invoice_date space-formatted; sync must send ISO. */
@OptIn(ExperimentalTime::class)
private fun String.toIsoInstantOrSelf(): String =
    if (isBlank()) this else DateTimeAdapter.fromDateTimeString(this)?.toString() ?: this

private fun String?.decodeTaxInfos(): List<TaxInfoApiModel>? =
    this?.takeIf { it.isNotBlank() && it != "null" }
        ?.let { runCatching { Json.decodeFromString<List<TaxInfoApiModel>>(it) }.getOrNull() }

private fun String?.decodeDiscounts(): List<Discount>? =
    this?.takeIf { it.isNotBlank() && it != "null" }
        ?.let { runCatching { Json.decodeFromString<List<Discount>>(it) }.getOrNull() }

fun InvoiceItemEntity.toApiModel(): InvoiceItemApiModel = InvoiceItemApiModel(
    id = id,
    description = description,
    quantity = quantity,
    price = selling_price,
    productPrice = product_price,
    mrp = mrp,
    dp = dp,
    totalCost = total_cost,
    totalTax = total_tax,
    basePrice = base_price,
    invoiceId = invoice_id,
    productId = product_id,
    taxCode = tax_code,
    active = active == 1L,
    softDeleted = soft_deleted == 1L,
    discount = discount.decodeDiscounts(),
    taxInfoApiModels = tax_info.decodeTaxInfos() ?: arrayListOf(),
    unitId = unit_id,
    baseQuantity = base_quantity,
    variantSku = variant_sku,
    resolvedUnitPriceMinor = resolved_unit_price_minor,
    currency = currency,
    priceSource = price_source,
    matchedPriceListUid = matched_price_list_uid,
    appliedTierMinQty = applied_tier_min_qty,
    belowMoq = below_moq == 1,
)

fun InvoiceEntity.toApiModel(items: List<InvoiceItemEntity>): InvoiceApiModel = InvoiceApiModel(
    id = id,
    invoiceDate = invoice_date.toIsoInstantOrSelf(),
    invoiceNumber = invoice_number,
    order_ref_id = order_ref_id,
    customerId = customer_id,
    customerName = customer_name,
    customerPhone = customer_phone,
    customerGst = customer_gst,
    sellerName = seller_name,
    sellerAddress = seller_address,
    sellerGst = seller_gst,
    placeOfSupply = place_of_supply,
    sellerPlaceOfSupply = seller_place_of_supply,
    created_by = created_by,
    updated_by = updated_by,
    totalCost = total_cost,
    basePrice = base_price,
    totalTax = total_tax,
    status = runCatching { InvoiceStatus.valueOf(status) }.getOrDefault(InvoiceStatus.DRAFT),
    totalItems = total_items.toInt(),
    active = active == 1L,
    lastUpdated = last_updated,
    softDeleted = soft_deleted == 1L,
    totalQuantity = total_quantity,
    discount = discount.decodeDiscounts(),
    invoiceItems = items.map { it.toApiModel() },
    taxInfoApiModels = tax_info.decodeTaxInfos(),
)