package com.ampairs.customer.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ampairs.customer.domain.Customer
import com.ampairs.customer.domain.CustomerAddress
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Entity(
    tableName = "customers",
    indices = [
        Index(value = ["id"], unique = true),
        Index(value = ["name"])
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
    val active: Boolean,
    val created_at: String?,
    val updated_at: String?,
    val synced: Boolean = false,
    val last_sync: Long = 0
)

@OptIn(ExperimentalTime::class)
fun Customer.toEntity(): CustomerEntity = CustomerEntity(
    id = uid,
    name = name,
    email = email,
    phone = phone,
    landline = landline,
    country_code = countryCode,
    customer_type = customerType,
    customer_group = customerGroup,
    gstNumber = gstNumber,
    address = address,
    street = street,
    city = city,
    state = state,
    pincode = pincode,
    country = country,
    latitude = latitude,
    longitude = longitude,
    billing_address_json = billingAddress?.let { Json.encodeToString(it) },
    shipping_address_json = shippingAddress?.let { Json.encodeToString(it) },
    active = active,
    created_at = createdAt,
    updated_at = updatedAt,
    synced = false,
    last_sync = Clock.System.now().toEpochMilliseconds()
)

fun CustomerEntity.toDomain(): Customer = Customer(
    uid = id,
    name = name,
    email = email,
    phone = phone,
    landline = landline,
    countryCode = country_code,
    customerType = customer_type,
    customerGroup = customer_group,
    gstNumber = gstNumber,
    address = address,
    street = street,
    city = city,
    state = state,
    pincode = pincode,
    country = country,
    latitude = latitude,
    longitude = longitude,
    billingAddress = billing_address_json?.let {
        try {
            Json.decodeFromString<CustomerAddress>(it)
        } catch (e: Exception) {
            null
        }
    },
    shippingAddress = shipping_address_json?.let {
        try {
            Json.decodeFromString<CustomerAddress>(it)
        } catch (e: Exception) {
            null
        }
    },
    active = active,
    createdAt = created_at,
    updatedAt = updated_at
)