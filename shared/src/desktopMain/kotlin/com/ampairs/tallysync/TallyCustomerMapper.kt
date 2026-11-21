package com.ampairs.tallysync

import com.ampairs.customer.data.db.CustomerEntity
import com.ampairs.customer.data.db.CustomerGroupEntity
import com.ampairs.customer.domain.CustomerAddress
import com.ampairs.supplier.data.db.SupplierEntity
import com.ampairs.supplier.domain.SupplierAddress
import com.ampairs.tally.model.master.Address
import com.ampairs.tally.model.master.Group
import com.ampairs.tally.model.master.Ledger
import com.ampairs.tally.model.master.LedgerGstRegDetails
import com.ampairs.tally.model.master.LedgerMailingDetails
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal object TallyCustomerMapper {

    const val ENTITY_LEDGER = "ledger"
    const val ENTITY_ACCOUNT_GROUP = "account_group"
    // Suppliers and customers both come from Tally ledgers (getLedgers), so the supplier intake keeps
    // its own alterId high-water mark — otherwise advancing the customer checkpoint would starve it.
    const val ENTITY_SUPPLIER_LEDGER = "supplier_ledger"

    // Tally's reserved primary groups that root the debtor (customer) vs creditor (supplier) trees.
    private const val SUNDRY_DEBTORS = "Sundry Debtors"
    private const val SUNDRY_CREDITORS = "Sundry Creditors"

    /**
     * What a Tally ledger represents for party-balance/payment sync, decided by its account-group
     * lineage: a customer (debtor), a supplier (creditor), or [OTHER] — anything else (bank, cash,
     * Duties & Taxes / GST, Profit & Loss and other expense/income ledgers). Only DEBTOR/CREDITOR
     * ledgers carry a party balance and receive payments; OTHER ledgers are skipped entirely.
     */
    enum class LedgerPartyType { DEBTOR, CREDITOR, OTHER }

    /** name → parent map over Tally account groups, for walking a ledger's group lineage. */
    fun buildGroupParentIndex(groups: List<Group>): Map<String, String> =
        groups.mapNotNull { g ->
            val n = g.name?.trim()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            n to (g.parent?.trim().orEmpty())
        }.toMap()

    /**
     * Classifies [groupName]'s lineage as DEBTOR (roots at "Sundry Debtors"), CREDITOR (roots at
     * "Sundry Creditors"), or OTHER (anything else — bank/cash/tax/P&L). Walks parents via
     * [parentIndex]; reserved primaries may not appear in the groups collection, so the direct name
     * compare on each hop is the real check. Defaulting unknown lineages to OTHER (not DEBTOR) is what
     * keeps non-party ledgers — ICICI Bank, CGST/SGST/IGST, Profit & Loss — out of customers/suppliers.
     */
    fun ledgerPartyType(groupName: String?, parentIndex: Map<String, String>): LedgerPartyType {
        var cur = groupName?.trim().orEmpty()
        var guard = 0
        while (cur.isNotBlank() && guard++ < 64) {
            if (cur.equals(SUNDRY_CREDITORS, ignoreCase = true)) return LedgerPartyType.CREDITOR
            if (cur.equals(SUNDRY_DEBTORS, ignoreCase = true)) return LedgerPartyType.DEBTOR
            cur = parentIndex[cur] ?: break
        }
        return LedgerPartyType.OTHER
    }

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

    fun Group.toCustomerGroupEntity(id: String): CustomerGroupEntity? {
        val groupName = name ?: return null
        val derivedCode = groupName
            .filter { it.isLetterOrDigit() || it == ' ' }
            .trim().replace(' ', '_').uppercase().take(20)
            .ifBlank { id.takeLast(8).uppercase() }
        return CustomerGroupEntity(
            id = id,
            name = groupName,
            description = parent?.takeIf { it.isNotBlank() },
            groupCode = derivedCode,
            displayOrder = null,
            defaultDiscountPercentage = null,
            priorityLevel = null,
            metadata = null,
            active = true,
            synced = false,
            createdAt = null,
            updatedAt = null,
            ref_id = groupName,
        )
    }

    fun Ledger.toCustomerEntity(
        id: String,
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
            ref_id = guid?.takeIf { it.isNotBlank() },
        )
    }

    /**
     * Maps a Tally creditor ledger to a [SupplierEntity] (the buy-side mirror of [toCustomerEntity]).
     * The payable balance is NOT carried here — it is derived from posted PURCHASE_BILL ledger entries
     * (see PurchaseLedgerPoster), so [SupplierEntity.outstandingAmount] is left null.
     */
    fun Ledger.toSupplierEntity(id: String): SupplierEntity? {
        val supplierName = name?.trim()?.takeIf { it.length >= 2 } ?: return null

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

        val gstin = (gstRegDetailList?.firstOrNull()?.gstin?.trim()?.takeIf { it.isNotBlank() }
            ?: partyGstin?.trim()?.takeIf { it.isNotBlank() })?.validGstin()

        val addressObj = SupplierAddress(
            street = street ?: "",
            city = city?.takeIf { it != street } ?: "",
            state = state ?: "",
            pincode = pincode ?: "",
            country = country,
        )
        val addressJson = Json.encodeToString(addressObj)

        return SupplierEntity(
            id = id,
            name = supplierName,
            email = null,
            phone = ledgerMobile?.trim()?.takeIf { it.isNotBlank() }?.extractMobilePhone(),
            landline = ledgerPhone?.trim()?.takeIf { it.isNotBlank() }?.cleanLandline(),
            country_code = 91,
            supplier_type = null,
            // Suppliers store the Tally group name directly (free text), unlike customer_group (an id).
            supplier_group = parent?.trim()?.takeIf { it.isNotBlank() },
            gstNumber = gstin,
            panNumber = null,
            creditLimit = null,
            creditDays = null,
            outstandingAmount = null,
            address = fullAddress,
            street = street,
            street2 = null,
            city = city?.takeIf { it != street },
            state = state,
            pincode = pincode,
            country = country,
            status = null,
            latitude = null,
            longitude = null,
            billing_address_json = addressJson,
            shipping_address_json = addressJson,
            active = true,
            created_at = null,
            updated_at = null,
            synced = false,
            last_sync = 0,
            ref_id = guid?.takeIf { it.isNotBlank() },
        )
    }

    // --- Push direction (Room → Tally), mirrors the field choices above -------------------------

    /**
     * Maps a [CustomerEntity] to a Tally [Ledger] for the master push (the reverse of
     * [toCustomerEntity]). [parentGroupName] is resolved by the caller — the custom
     * [CustomerEntity.customer_group] name when set, else the reserved primary "Sundry Debtors" —
     * since the group itself may need pushing first (see [TallyAccountGroupPushService]).
     */
    fun CustomerEntity.toTallyLedger(parentGroupName: String): Ledger = Ledger(
        name = name,
        parent = parentGroupName,
        guid = ref_id,
        ledgerMobile = phone,
        ledgerPhone = landline,
        partyGstin = gstNumber,
        gstRegDetailList = gstNumber?.takeIf { it.isNotBlank() }?.let {
            listOf(LedgerGstRegDetails(gstin = it))
        },
        mailingDetailList = listOf(
            LedgerMailingDetails(
                mailingName = name,
                addressList = listOfNotNull(street, address).distinct()
                    .filter { it.isNotBlank() }
                    .map { Address(address = it) }
                    .ifEmpty { null },
                stateName = state,
                countryName = country,
                pinCode = pincode,
            ),
        ),
    )

    /**
     * Maps a [SupplierEntity] to a Tally [Ledger] (the reverse of [toSupplierEntity]).
     * [parentGroupName] is the supplier's own `supplier_group` free-text name when set, else the
     * reserved primary "Sundry Creditors" — resolved by the caller (see [TallyLedgerPushService]).
     */
    fun SupplierEntity.toTallyLedger(parentGroupName: String): Ledger = Ledger(
        name = name,
        parent = parentGroupName,
        guid = ref_id,
        ledgerMobile = phone,
        ledgerPhone = landline,
        partyGstin = gstNumber,
        gstRegDetailList = gstNumber?.takeIf { it.isNotBlank() }?.let {
            listOf(LedgerGstRegDetails(gstin = it))
        },
        mailingDetailList = listOf(
            LedgerMailingDetails(
                mailingName = name,
                addressList = listOfNotNull(street, address).distinct()
                    .filter { it.isNotBlank() }
                    .map { Address(address = it) }
                    .ifEmpty { null },
                stateName = state,
                countryName = country,
                pinCode = pincode,
            ),
        ),
    )

    /** Maps a custom customer sub-group to a Tally [Group] rooted under "Sundry Debtors". */
    fun CustomerGroupEntity.toTallyGroup(): Group = Group(
        name = name,
        parent = SUNDRY_DEBTORS,
        guid = ref_id,
    )

    /** Maps a supplier's free-text group name to a Tally [Group] rooted under "Sundry Creditors". */
    fun toTallySupplierGroup(groupName: String): Group = Group(
        name = groupName,
        parent = SUNDRY_CREDITORS,
    )
}
