package com.ampairs.supplier.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ampairs.supplier.domain.Supplier
import com.ampairs.supplier.domain.SupplierAddress
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

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

@OptIn(ExperimentalTime::class)
fun Supplier.toEntity(): SupplierEntity = SupplierEntity(
    id = uid,
    ref_id = refId,
    name = name,
    email = email,
    phone = phone,
    landline = landline,
    country_code = if (countryCode == 0) 91 else countryCode,
    supplier_type = supplierType,
    supplier_group = supplierGroup,
    gstNumber = gstNumber,
    panNumber = panNumber,
    creditLimit = creditLimit,
    creditDays = creditDays,
    outstandingAmount = outstandingAmount,
    address = address,
    street = street,
    street2 = street2,
    city = city,
    state = state,
    pincode = pincode,
    country = country,
    status = status,
    latitude = latitude,
    longitude = longitude,
    billing_address_json = billingAddress?.let { Json.encodeToString(it) },
    shipping_address_json = shippingAddress?.let { Json.encodeToString(it) },
    attributes_json = attributes?.takeIf { it.isNotEmpty() }?.let { Json.encodeToString(it) },
    active = active,
    created_at = createdAt,
    updated_at = updatedAt,
    synced = false,
    last_sync = Clock.System.now().toEpochMilliseconds()
)

fun SupplierEntity.toDomain(): Supplier = Supplier(
    uid = id,
    refId = ref_id,
    name = name,
    email = email,
    phone = phone,
    landline = landline,
    countryCode = if (country_code == 0) 91 else country_code,
    supplierType = supplier_type,
    supplierGroup = supplier_group,
    gstNumber = gstNumber,
    panNumber = panNumber,
    creditLimit = creditLimit,
    creditDays = creditDays,
    outstandingAmount = outstandingAmount,
    address = address,
    street = street,
    street2 = street2,
    city = city,
    state = state,
    pincode = pincode,
    country = country,
    status = status,
    latitude = latitude,
    longitude = longitude,
    billingAddress = billing_address_json?.let {
        try {
            Json.decodeFromString<SupplierAddress>(it)
        } catch (e: Exception) {
            null
        }
    },
    shippingAddress = shipping_address_json?.let {
        try {
            Json.decodeFromString<SupplierAddress>(it)
        } catch (e: Exception) {
            null
        }
    },
    attributes = attributes_json?.let {
        try {
            Json.decodeFromString<Map<String, String>>(it)
        } catch (e: Exception) {
            null
        }
    },
    active = active,
    createdAt = created_at,
    updatedAt = updated_at
)
