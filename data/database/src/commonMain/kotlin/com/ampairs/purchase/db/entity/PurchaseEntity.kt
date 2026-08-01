package com.ampairs.purchase.db.entity

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "purchaseEntity",
    indices = [Index(value = ["id"], unique = true, name = "purchase_id_idx")]
)
data class PurchaseEntity(
    @PrimaryKey(autoGenerate = true)
    val seq_id: Long = 0,
    val id: String,
    val purchase_number: String,
    val purchase_date: String,
    val status: String,
    // Single supplier (the buyer is the implicit current workspace). Name/GST/phone are snapshotted
    // on the document for the wire contract; supplier_invoice_number is the supplier's own bill no.
    val supplier_id: String,
    val supplier_name: String,
    val supplier_gst: String,
    val supplier_phone: String? = null,
    val supplier_invoice_number: String? = null,
    val purchase_type: String? = null,
    // Place of supply: supplier/origin state vs buyer/destination state. IGST iff they differ.
    val place_of_supply: String? = null,
    val total_cost: Double,
    val total_tax: Double,
    val total_items: Long,
    val total_quantity: Double,
    val base_price: Double,
    val tax_info: String? = null,
    val billing_address: String? = null,
    val shipping_address: String? = null,
    val discount: String? = null,
    val active: Long = 1,
    val soft_deleted: Long = 0,
    val synced: Long = 0,
    val last_updated: Long = 0,
    // document tax/discount mode (mirrors order spec 010 C1/C2)
    val price_mode: String = "TAX_EXCLUSIVE",
    val overall_discount_mode: String = "POST_TAX_REDUCTION"
)
