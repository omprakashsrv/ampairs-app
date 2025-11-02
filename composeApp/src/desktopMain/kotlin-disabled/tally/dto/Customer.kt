package com.ampairs.tally.dto

import com.ampairs.customer.api.model.CustomerApiModel
import com.ampairs.tally.model.master.Ledger

fun List<Ledger>.toCustomers(): List<CustomerApiModel> {
    return map {
        CustomerApiModel(
            id = "",
            name = it.name ?: "",
            refId = it.guid ?: "",
            address = it.addressList?.joinToString(" ") ?: "",
            phone = parsePhone(it.ledgerMobile),
            landline = parsePhone(it.ledgerPhone),
            pincode = it.pinCode ?: "",
            gstin = it.partyGstin ?: "",
            state = it.ledStateName ?: "",
            email = "",
            countryCode = 91,
            latitude = null,
            longitude = null,
            active = true,
            softDeleted = false,
            city = "",
            country = "India",
            street = "",
            street2 = ""
        )
    }
}

private fun parsePhone(phone: String?): String {
    var phone = phone?.replace(" ", "") ?: phone
    phone = if (phone?.contains(",") == true) phone.split(",")[0] else
        (if (phone?.contains("/") == true) phone.split("/")[0] else
            (if (phone?.contains("\\") == true) phone.split("\\")[0] else phone))
            ?: ""
    phone = if (phone.startsWith("0")) phone.drop(1) else phone
    phone = "[^0-9]".toRegex().replace(phone, "")
    return phone
}