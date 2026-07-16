package com.ampairs.customer.data.db

import com.ampairs.customer.domain.Customer
import com.ampairs.customer.domain.CustomerAddress
import com.ampairs.customer.domain.CustomerGroup
import com.ampairs.customer.domain.CustomerType
import com.ampairs.customer.domain.State
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

// Entity <-> domain mappers. The @Entity classes live in :data:database (same package); these
// mappers stay in the feature module because they reference the feature's domain models.

@OptIn(ExperimentalTime::class)
fun Customer.toEntity(): CustomerEntity = CustomerEntity(
    id = uid,
    ref_id = refId,
    name = name,
    email = email,
    phone = phone,
    landline = landline,
    country_code = if (countryCode == 0) 91 else countryCode,
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
    attributes_json = attributes?.takeIf { it.isNotEmpty() }?.let { Json.encodeToString(it) },
    active = active,
    created_at = createdAt,
    updated_at = updatedAt,
    synced = false,
    last_sync = Clock.System.now().toEpochMilliseconds()
)

fun CustomerEntity.toDomain(): Customer = Customer(
    uid = id,
    refId = ref_id,
    name = name,
    email = email,
    phone = phone,
    landline = landline,
    countryCode = if (country_code == 0) 91 else country_code,
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

fun CustomerGroupEntity.toCustomerGroup(): CustomerGroup = CustomerGroup(
    uid = id,
    name = name,
    description = description,
    groupCode = groupCode,
    displayOrder = displayOrder,
    defaultDiscountPercentage = defaultDiscountPercentage,
    priorityLevel = priorityLevel,
    metadata = metadata,
    active = active,
    refId = ref_id,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun CustomerGroup.toEntity(): CustomerGroupEntity = CustomerGroupEntity(
    id = uid,
    name = name,
    description = description,
    groupCode = groupCode,
    displayOrder = displayOrder,
    defaultDiscountPercentage = defaultDiscountPercentage,
    priorityLevel = priorityLevel,
    metadata = metadata,
    active = active,
    ref_id = refId,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun CustomerTypeEntity.toCustomerType(): CustomerType = CustomerType(
    uid = id,
    name = name,
    description = description,
    typeCode = typeCode,
    displayOrder = displayOrder,
    defaultCreditLimit = defaultCreditLimit,
    defaultCreditDays = defaultCreditDays,
    metadata = metadata,
    active = active,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun CustomerType.toEntity(): CustomerTypeEntity = CustomerTypeEntity(
    id = uid,
    name = name,
    description = description,
    typeCode = typeCode,
    displayOrder = displayOrder,
    defaultCreditLimit = defaultCreditLimit,
    defaultCreditDays = defaultCreditDays,
    metadata = metadata,
    active = active,
    createdAt = createdAt,
    updatedAt = updatedAt
)

@OptIn(ExperimentalTime::class)
fun State.toEntity(): StateEntity {
    val now = Clock.System.now().toEpochMilliseconds()
    return StateEntity(
        id = id,
        name = name,
        syncStatus = "SYNCED",
        createdAt = now,
        updatedAt = now
    )
}

fun StateEntity.toDomain(): State {
    return State(
        id = id,
        name = name,
    )
}

fun List<StateEntity>.toDomainList(): List<State> {
    return map { it.toDomain() }
}
