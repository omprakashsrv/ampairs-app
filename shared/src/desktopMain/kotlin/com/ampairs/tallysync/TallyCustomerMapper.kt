package com.ampairs.tallysync

import com.ampairs.customer.data.db.CustomerEntity
import com.ampairs.customer.data.db.CustomerGroupEntity
import com.ampairs.tally.model.master.Group
import com.ampairs.tally.model.master.Ledger

internal object TallyCustomerMapper {

    const val ENTITY_LEDGER = "ledger"
    const val ENTITY_ACCOUNT_GROUP = "account_group"

    fun Group.toCustomerGroupEntity(): CustomerGroupEntity? {
        val id = tallyId(guid, name) ?: return null
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
        )
    }

    fun Ledger.toCustomerEntity(groupIdByName: Map<String, String>): CustomerEntity? {
        val id = tallyId(guid, name) ?: return null
        val customerName = name ?: return null

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
        val pincode = mailingDetails?.pinCode?.trim()?.takeIf { it.isNotBlank() }
            ?: pinCode?.trim()?.takeIf { it.isNotBlank() }
        val country = mailingDetails?.countryName?.trim()?.takeIf { it.isNotBlank() }
            ?: countryName?.trim()?.takeIf { it.isNotBlank() }
            ?: "India"

        // LEDGSTREGDETAILS.LIST has the actual GSTIN for GST-registered parties;
        // top-level PARTYGSTIN may be empty even when the party is GST-registered
        val gstin = gstRegDetailList?.firstOrNull()?.gstin?.trim()?.takeIf { it.isNotBlank() }
            ?: partyGstin?.trim()?.takeIf { it.isNotBlank() }

        return CustomerEntity(
            id = id,
            name = customerName,
            email = null,
            phone = ledgerMobile?.trim()?.takeIf { it.isNotBlank() },
            landline = ledgerPhone?.trim()?.takeIf { it.isNotBlank() },
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
            billing_address_json = null,
            shipping_address_json = null,
            active = true,
            created_at = null,
            updated_at = null,
            synced = false,
            last_sync = 0,
        )
    }

    private fun tallyId(guid: String?, name: String?): String? = when {
        !guid.isNullOrBlank() -> guid
        !name.isNullOrBlank() -> "TALLY_${name.hashCode()}"
        else -> null
    }
}
