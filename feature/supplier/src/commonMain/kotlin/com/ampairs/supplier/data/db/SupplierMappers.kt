package com.ampairs.supplier.data.db

import com.ampairs.supplier.domain.Supplier
import com.ampairs.supplier.domain.SupplierAddress
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

// Entity <-> domain mappers. The @Entity class lives in :data:database (same package); these
// mappers stay in the feature module because they reference the supplier domain models.

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
