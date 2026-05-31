package com.ampairs.tallysync

import com.ampairs.customer.data.db.CustomerEntity
import com.ampairs.customer.data.db.CustomerGroupEntity
import com.ampairs.customer.domain.CustomerAddress
import com.ampairs.tally.model.master.Group
import com.ampairs.tally.model.master.Ledger
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal object TallyCustomerMapper {

    const val ENTITY_LEDGER = "ledger"
    const val ENTITY_ACCOUNT_GROUP = "account_group"

    // Tally often stores multiple numbers in one field: "9876543210/9123456789" or "9876543210,9123456789"
    // Extract first valid 10-digit Indian mobile number (starts with 6-9)
    private fun String.extractMobilePhone(): String? =
        split(Regex("[,/\\\\;|\\s]+"))
            .map { it.trim().filter { c -> c.isDigit() } }
            .firstOrNull { it.length == 10 && it[0] in '6'..'9' }

    // Server pattern for landline: ^[0-9\-+()\s]*$ — strip commas, slashes, backslashes
    private fun String.cleanLandline(): String? {
        val first = split(Regex("[,/\\\\]")).first().trim()
        val cleaned = first.filter { it.isDigit() || it in "-+()" || it == ' ' }.trim()
        return cleaned.takeIf { it.isNotBlank() }
    }

    // Server requires exactly 6 digits
    private fun String.validPincode(): String? {
        val digits = trim().filter { it.isDigit() }
        return if (digits.length == 6) digits else null
    }

    // GSTIN: 2 digits + 5 letters + 4 digits + 1 letter + 1 alphanumeric + Z + 1 alphanumeric = 15 chars
    private val GSTIN_REGEX = Regex("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][A-Z0-9]Z[A-Z0-9]$")
    private fun String.validGstin(): String? {
        val upper = trim().uppercase()
        return if (GSTIN_REGEX.matches(upper)) upper else null
    }

    fun Group.toCustomerGroupEntity(id: String, tallyRefId: String): CustomerGroupEntity? {
        val groupName = name ?: return null
        return CustomerGroupEntity(
            id = id,
            name = groupName,
            description = parent?.takeIf { it.isNotBlank() },
            groupCode = null,
            displayOrder = null,
            defaultDiscountPercentage = null,
            priorityLevel = null,
            metadata = null,
            active = true,
            synced = false,
            createdAt = null,
            updatedAt = null,
            ref_id = tallyRefId,
        )
    }

    fun Ledger.toCustomerEntity(
        id: String,
        tallyRefId: String,
        groupIdByName: Map<String, String>,
    ): CustomerEntity? {
        val customerName = name?.trim()?.takeIf { it.length >= 2 } ?: return null

        // LEDMAILINGDETAILS.LIST has the real address/state/pincode/country;
        // the top-level STATENAME/PINCODE/COUNTRYNAME fields are empty in most Tally exports
        val mailingDetails = mailingDetailList?.firstOrNull()
        val addressLines = (mailingDetails?.addressList ?: addressList)
            ?.mapNotNull { it.address?.trim()?.takeIf { s -> s.isNotBlank() } } ?: emptyList()
        val fullAddress = addressLines.joinToString(", ").takeIf { it.isNotBlank() }
        val street = addressLines.firstOrNull()
        val city = if (addressLines.size > 1) addressLines.last() else null

        val state = mailingDetails?.stateName?.trim()?.takeIf { it.isNotBlank() }
            ?: stateName?.trim()?.takeIf { it.isNotBlank() }
        val pincode = (mailingDetails?.pinCode?.trim()?.takeIf { it.isNotBlank() }
            ?: pinCode?.trim()?.takeIf { it.isNotBlank() })?.validPincode()
        val country = mailingDetails?.countryName?.trim()?.takeIf { it.isNotBlank() }
            ?: countryName?.trim()?.takeIf { it.isNotBlank() }
            ?: "India"

        // LEDGSTREGDETAILS.LIST has the actual GSTIN for GST-registered parties;
        // top-level PARTYGSTIN may be empty even when the party is GST-registered
        val gstin = (gstRegDetailList?.firstOrNull()?.gstin?.trim()?.takeIf { it.isNotBlank() }
            ?: partyGstin?.trim()?.takeIf { it.isNotBlank() })?.validGstin()

        val addressObj = CustomerAddress(
            street = street ?: "",
            city = city?.takeIf { it != street } ?: "",
            state = state ?: "",
            pincode = pincode ?: "",
            country = country,
        )
        val addressJson = Json.encodeToString(addressObj)

        return CustomerEntity(
            id = id,
            name = customerName,
            email = null,
            phone = ledgerMobile?.trim()?.takeIf { it.isNotBlank() }?.extractMobilePhone(),
            landline = ledgerPhone?.trim()?.takeIf { it.isNotBlank() }?.cleanLandline(),
            country_code = 91,
            customer_type = null,
            customer_group = parent?.let { groupIdByName[it] },
            gstNumber = gstin,
            address = fullAddress,
            street = street,
            city = city?.takeIf { it != street },
            state = state,
            pincode = pincode,
            country = country,
            latitude = null,
            longitude = null,
            billing_address_json = addressJson,
            shipping_address_json = addressJson,
            active = true,
            created_at = null,
            updated_at = null,
            synced = false,
            last_sync = 0,
            ref_id = tallyRefId,
        )
    }
}
