package com.ampairs.customer.ui.create

/**
 * Bridges [CustomerFormState] and the schema renderer's value map for the registry's STANDARD
 * customer fields (spec 011, T032). Keys mirror `CustomerStandardFieldProvider` on the backend;
 * choice values are NAMES (what the customer columns store). Unknown keys are ignored on apply.
 */

private fun Double.asInput(): String = if (this == 0.0) "" else {
    if (this % 1.0 == 0.0) this.toLong().toString() else this.toString()
}

private fun Int.asInput(): String = if (this == 0) "" else this.toString()

internal fun CustomerFormState.toStandardValueMap(): Map<String, Any?> = mapOf(
    "name" to name,
    "customerType" to customerType,
    "customerGroup" to customerGroup,
    "status" to status,
    "phone" to phone,
    "landline" to landline,
    "email" to email,
    "gstNumber" to gstNumber,
    "panNumber" to panNumber,
    "creditLimit" to creditLimit.asInput(),
    "creditDays" to creditDays.asInput(),
    "address" to address,
    "street" to street,
    "city" to city,
    "pincode" to pincode,
    "state" to state,
    "country" to country,
)

internal fun CustomerFormState.applyStandardValues(values: Map<String, Any?>): CustomerFormState {
    var s = this
    fun str(key: String): String? = (values[key] as? String)
    str("name")?.let { s = s.copy(name = it) }
    str("customerType")?.let { s = s.copy(customerType = it, customerTypeName = it) }
    str("customerGroup")?.let { s = s.copy(customerGroup = it, customerGroupName = it) }
    str("status")?.let { v -> if (v.isNotBlank()) s = s.copy(status = v) }
    str("phone")?.let { s = s.copy(phone = it) }
    str("landline")?.let { s = s.copy(landline = it) }
    str("email")?.let { s = s.copy(email = it) }
    str("gstNumber")?.let { s = s.copy(gstNumber = it) }
    str("panNumber")?.let { s = s.copy(panNumber = it) }
    str("creditLimit")?.let { s = s.copy(creditLimit = it.toDoubleOrNull() ?: 0.0) }
    str("creditDays")?.let { s = s.copy(creditDays = it.toIntOrNull() ?: 0) }
    str("address")?.let { s = s.copy(address = it) }
    str("street")?.let { s = s.copy(street = it) }
    str("city")?.let { s = s.copy(city = it) }
    str("pincode")?.let { s = s.copy(pincode = it) }
    str("state")?.let { s = s.copy(state = it) }
    str("country")?.let { v -> if (v.isNotBlank()) s = s.copy(country = v) }
    return s
}
