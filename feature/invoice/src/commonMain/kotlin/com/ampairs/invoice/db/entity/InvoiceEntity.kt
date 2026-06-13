package com.ampairs.invoice.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "invoiceEntity",
    indices = [Index(value = ["id"], unique = true, name = "invoice_id_idx")]
)
data class InvoiceEntity(
    @PrimaryKey(autoGenerate = true)
    val seq_id: Long = 0,
    val id: String,
    val invoice_number: String,
    val invoice_date: String,
    val status: String,
    // Single buyer (the seller is the implicit current workspace). Name/GST are snapshotted on the
    // tax invoice (frozen at issue time); phone is carried for the wire contract.
    val customer_id: String,
    val customer_name: String,
    val customer_gst: String,
    val customer_phone: String? = null,
    // Seller (issuing business) snapshot — self-contained + frozen at issue; no render-time fetch.
    val seller_name: String? = null,
    val seller_address: String? = null,
    val seller_gst: String? = null,
    // Place of supply (state) vs the seller state decides CGST+SGST (intra) vs IGST (inter).
    val place_of_supply: String? = null,
    val total_cost: Double,
    val total_tax: Double,
    val total_items: Long,
    val total_quantity: Double,
    val base_price: Double,
    val tax_info: String? = null,
    val created_by: String = "",
    val updated_by: String = "",
    val billing_address: String? = null,
    val shipping_address: String? = null,
    val discount: String? = null,
    val active: Long = 1,
    val soft_deleted: Long = 0,
    val synced: Long = 0,
    val last_updated: Long = 0,
    val order_ref_id: String? = null,
    // spec 010: document tax/discount mode (C1/C2) + client-assigned GST number series (C4/C5)
    val price_mode: String = "TAX_EXCLUSIVE",
    val overall_discount_mode: String = "POST_TAX_REDUCTION",
    val series: String = "DEFAULT",
    val sequence_number: Long = 0
)