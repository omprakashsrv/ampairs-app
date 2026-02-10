package com.ampairs.invoice.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "invoiceItemEntity",
    indices = [Index(value = ["id"], unique = true, name = "invoice_item_id_idx")]
)
data class InvoiceItemEntity(
    @PrimaryKey(autoGenerate = true)
    val seq_id: Long = 0,
    val id: String,
    val description: String,
    val product_id: String,
    val total_cost: Double,
    val base_price: Double,
    val product_price: Double,
    val total_tax: Double,
    val invoice_id: String,
    val tax_code: String,
    val quantity: Double,
    val item_no: Long,
    val selling_price: Double,
    val mrp: Double,
    val dp: Double,
    val tax_info: String? = null,
    val discount: String? = null,
    val active: Long = 1,
    val soft_deleted: Long = 0
)