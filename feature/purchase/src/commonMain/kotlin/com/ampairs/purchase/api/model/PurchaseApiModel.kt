package com.ampairs.purchase.api.model

import com.ampairs.purchase.db.entity.PurchaseEntity
import com.ampairs.purchase.domain.Address
import com.ampairs.purchase.domain.Discount
import com.ampairs.purchase.domain.PurchaseStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class PurchaseApiModel(
    @SerialName("id") val id: String = "",
    @SerialName("purchase_date") val purchaseDate: String = "",
    @SerialName("purchase_number") val purchaseNumber: String = "",
    @SerialName("supplier_id") var supplierId: String = "",
    @SerialName("supplier_name") var supplierName: String = "",
    @SerialName("supplier_phone") var supplierPhone: String? = null,
    @SerialName("supplier_gst") var supplierGst: String = "",
    @SerialName("supplier_invoice_number") var supplierInvoiceNumber: String? = null,
    @SerialName("purchase_type") var purchaseType: String? = null,
    @SerialName("place_of_supply") var placeOfSupply: String? = null,
    @SerialName("total_cost") var totalCost: Double = 0.0,
    @SerialName("base_price") var basePrice: Double = 0.0,
    @SerialName("total_tax") var totalTax: Double = 0.0,
    @SerialName("status") var status: PurchaseStatus = PurchaseStatus.DRAFT,
    @SerialName("total_items") var totalItems: Int = 0,
    @SerialName("total_quantity") var totalQuantity: Double = 0.0,
    @SerialName("active") var active: Boolean = true,
    @SerialName("last_updated") var lastUpdated: Long = 0,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("soft_deleted") var softDeleted: Boolean = false,
    @SerialName("billing_address") var billingAddress: Address? = null,
    @SerialName("shipping_address") var shippingAddress: Address? = null,
    @SerialName("discount") var discount: List<Discount>? = null,
    @SerialName("price_mode") var priceMode: String = "TAX_EXCLUSIVE",
    @SerialName("overall_discount_mode") var overallDiscountMode: String = "POST_TAX_REDUCTION",
    @SerialName("purchase_items") var purchaseItems: List<PurchaseItemApiModel> = arrayListOf(),
    @SerialName("tax_infos") val taxInfoApiModels: List<TaxInfoApiModel>? = null,
)

/** Build the wire parent (without items) from a local row. */
fun PurchaseEntity.toApiModel(items: List<PurchaseItemApiModel>): PurchaseApiModel {
    return PurchaseApiModel(
        id = this.id,
        purchaseDate = this.purchase_date,
        purchaseNumber = this.purchase_number,
        supplierId = this.supplier_id,
        supplierName = this.supplier_name,
        supplierPhone = this.supplier_phone,
        supplierGst = this.supplier_gst,
        supplierInvoiceNumber = this.supplier_invoice_number,
        purchaseType = this.purchase_type,
        placeOfSupply = this.place_of_supply,
        totalCost = this.total_cost,
        basePrice = this.base_price,
        totalTax = this.total_tax,
        status = runCatching { PurchaseStatus.valueOf(this.status.uppercase()) }.getOrDefault(PurchaseStatus.DRAFT),
        totalItems = this.total_items.toInt(),
        totalQuantity = this.total_quantity,
        active = this.active == 1L,
        lastUpdated = this.last_updated,
        softDeleted = this.soft_deleted == 1L,
        billingAddress = this.billing_address?.takeIf { it.isNotBlank() }?.let { Json.decodeFromString(it) },
        shippingAddress = this.shipping_address?.takeIf { it.isNotBlank() }?.let { Json.decodeFromString(it) },
        discount = this.discount?.takeIf { it.isNotBlank() }?.let { Json.decodeFromString(it) },
        priceMode = this.price_mode,
        overallDiscountMode = this.overall_discount_mode,
        taxInfoApiModels = this.tax_info?.takeIf { it.isNotBlank() }?.let { Json.decodeFromString(it) },
        purchaseItems = items,
    )
}

/** Map wire parents into local parent rows (sync pull) — items mapped separately. */
fun List<PurchaseApiModel>.asDatabaseModel(): List<PurchaseEntity> {
    return map { api ->
        PurchaseEntity(
            seq_id = 0,
            id = api.id,
            purchase_number = api.purchaseNumber,
            purchase_date = api.purchaseDate,
            status = api.status.name,
            supplier_id = api.supplierId,
            supplier_name = api.supplierName,
            supplier_gst = api.supplierGst,
            supplier_phone = api.supplierPhone,
            supplier_invoice_number = api.supplierInvoiceNumber,
            purchase_type = api.purchaseType,
            place_of_supply = api.placeOfSupply,
            total_cost = api.totalCost,
            base_price = api.basePrice,
            total_tax = api.totalTax,
            total_items = api.totalItems.toLong(),
            total_quantity = api.totalQuantity,
            tax_info = Json.encodeToString(api.taxInfoApiModels),
            billing_address = api.billingAddress?.let { Json.encodeToString(it) },
            shipping_address = api.shippingAddress?.let { Json.encodeToString(it) },
            discount = api.discount?.let { Json.encodeToString(it) },
            active = if (api.active) 1L else 0L,
            soft_deleted = if (api.softDeleted) 1L else 0L,
            synced = 1,
            last_updated = api.lastUpdated,
            price_mode = api.priceMode,
            overall_discount_mode = api.overallDiscountMode,
        )
    }
}
