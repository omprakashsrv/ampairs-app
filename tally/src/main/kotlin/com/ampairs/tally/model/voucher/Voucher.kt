package com.ampairs.tally.model.voucher

import com.ampairs.tally.model.master.Address
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
data class Voucher(
    @XmlElement(false)
    @XmlSerialName("REMOTEID")
    var remoteId: String? = null,

    // VCHTYPE / ACTION are kept as raw strings, not enums: a single VOUCHER collection returns every
    // voucher type (incl. custom names like "GST Sales") and a strict enum would crash deserialization
    // on the first unknown value. Classification is done by keyword in TallyVoucherMapper.
    @XmlElement(false)
    @XmlSerialName("VCHTYPE")
    var vchType: String? = null,

    @XmlElement(false)
    @XmlSerialName("ACTION")
    var action: String? = null,

    @XmlElement(true)
    @XmlSerialName("DATE")
    var date: String? = null,

    @XmlElement(true)
    @XmlSerialName("REFERENCEDATE")
    var referenceDate: String? = null,

    @XmlElement(true)
    @XmlSerialName("INVDELIVERYDATE")
    var invoiceDeliveryDate: String? = null,

    @XmlElement(true)
    @XmlSerialName("GUID")
    var guid: String? = null,

    @XmlElement(true)
    @XmlSerialName("STATENAME")
    var stateName: String? = null,

    @XmlElement(true)
    @XmlSerialName("COUNTRYOFRESIDENCE")
    var countryOfResidence: String? = null,

    @XmlElement(true)
    @XmlSerialName("PARTYGSTIN")
    var partyGstin: String? = null,

    @XmlElement(true)
    @XmlSerialName("PARTYNAME")
    var partyName: String? = null,

    @XmlElement(true)
    @XmlSerialName("VOUCHERTYPENAME")
    var voucherTypeName: String? = null,

    @XmlElement(true)
    @XmlSerialName("REFERENCE")
    var referenceNumber: String? = null,

    @XmlElement(true)
    @XmlSerialName("VOUCHERNUMBER")
    var voucherNumber: String? = null,

    @XmlElement(true)
    @XmlSerialName("PARTYLEDGERNAME")
    var partyLedgerName: String? = null,

    @XmlElement(true)
    @XmlSerialName("BASICBASEPARTYNAME")
    var basicBasePartyName: String? = null,

    @XmlElement(true)
    @XmlSerialName("PLACEOFSUPPLY")
    var placeOfSupply: String? = null,

    @XmlElement(true)
    @XmlSerialName("CONSIGNEEGSTIN")
    var consigneeGstin: String? = null,

    @XmlElement(true)
    @XmlSerialName("BASICBUYERNAME")
    var basicBuyerName: String? = null,

    @XmlElement(true)
    @XmlSerialName("BASICDATETIMEOFINVOICE")
    var basicDateTimeOfInvoice: String? = null,

    @XmlElement(true)
    @XmlSerialName("BASICDATETIMEOFREMOVAL")
    var basicDateTimeOfRemoval: String? = null,

    @XmlElement(true)
    @XmlSerialName("CONSIGNEEPINNUMBER")
    var consigneePinNumber: String? = null,

    @XmlElement(true)
    @XmlSerialName("CONSIGNEESTATENAME")
    var consigneeStateName: String? = null,

    @XmlElement(true)
    @XmlSerialName("EFFECTIVEDATE")
    var effectiveDate: String? = null,

    @XmlElement(true)
    @XmlSerialName("ISINVOICE")
    var isInvoice: String? = null,

    @XmlElement(true)
    @XmlSerialName("ALTERID")
    var alterId: String? = null,

    @XmlElement(true)
    @XmlSerialName("LEDGERENTRIES.LIST")
    var ledgerEntrieList: List<LedgerEntrie>? = null,

    @XmlElement(true)
    @XmlSerialName("ALLLEDGERENTRIES.LIST")
    var allLedgerEntriesList: List<LedgerEntrie>? = null,

    @XmlElement(true)
    @XmlSerialName("ALLINVENTORYENTRIES.LIST")
    var inventoryList: List<InventoryList>? = null,

    @XmlElement(true)
    @XmlSerialName("INVENTORYALLOCATIONS.LIST")
    var inventoryAllocations: List<InventoryAllocation>? = null,

    @XmlElement(true)
    @XmlSerialName("ADDRESS.LIST")
    var addressList: List<Address>? = null,

    @XmlElement(true)
    @XmlSerialName("BASICBUYERADDRESS.LIST")
    var basicBuyerAddressList: List<BasicBuyerAddress>? = null,

    @XmlElement(true)
    @XmlSerialName("NARRATION")
    var narration: String? = null,

    @XmlElement(true)
    @XmlSerialName("GSTNATUREOFRETURN")
    var natureOfReturn: String? = null,
)
