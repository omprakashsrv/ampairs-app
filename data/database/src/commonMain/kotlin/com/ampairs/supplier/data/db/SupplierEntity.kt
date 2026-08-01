package com.ampairs.supplier.data.db

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "suppliers",
    indices = [
        Index(value = ["id"], unique = true),
        Index(value = ["name"]),
        Index(value = ["ref_id"], name = "supplier_ref_idx"),
        Index(value = ["state"], name = "supplier_state_idx"),
        Index(value = ["supplier_type"], name = "supplier_type_idx"),
        Index(value = ["supplier_group"], name = "supplier_group_idx")
    ]
)
data class SupplierEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String?,
    val phone: String?,
    val landline: String?,
    val country_code: Int,
    val supplier_type: String?,
    val supplier_group: String?,
    val gstNumber: String?,
    val panNumber: String?,
    val creditLimit: Double?,
    val creditDays: Int?,
    val outstandingAmount: Double?,
    val address: String?,
    val street: String?,
    val street2: String?,
    val city: String?,
    val state: String?,
    val pincode: String?,
    val country: String,
    val status: String?,
    val latitude: Double?,
    val longitude: Double?,
    val billing_address_json: String?,
    val shipping_address_json: String?,
    val attributes_json: String? = null,
    val active: Boolean,
    val created_at: String?,
    val updated_at: String?,
    val synced: Boolean = false,
    val last_sync: Long = 0,
    val ref_id: String? = null
)
