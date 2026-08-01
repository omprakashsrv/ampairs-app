package com.ampairs.customer.data.db

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "customers",
    indices = [
        Index(value = ["id"], unique = true),
        Index(value = ["name"]),
        Index(value = ["ref_id"], name = "customer_ref_idx"),
        Index(value = ["state"], name = "customer_state_idx"),
        Index(value = ["customer_type"], name = "customer_type_idx"),
        Index(value = ["customer_group"], name = "customer_group_idx")
    ]
)
data class CustomerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String?,
    val phone: String?,
    val landline: String?,
    val country_code: Int,
    val customer_type: String?,
    val customer_group: String?,
    val gstNumber: String?,
    val address: String?,
    val street: String?,
    val city: String?,
    val state: String?,
    val pincode: String?,
    val country: String,
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
