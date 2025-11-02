package com.ampairs.tally.model.voucher


import com.ampairs.tally.model.master.RateDetail
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName


@Serializable
class LedgerEntrie(
    @XmlElement(true)
    @XmlSerialName("LEDGERNAME")
    var ledgerName: String? = null,

    @XmlElement(true)
    @XmlSerialName("ISDEEMEDPOSITIVE")
    private var isDeemedPositive: String? = null,

    @XmlElement(true)
    @XmlSerialName("ISPARTYLEDGER")
    var isPartyLedger: String? = null,

    @XmlElement(true)
    @XmlSerialName("ISLASTDEEMEDPOSITIVE")
    private var isLastDeemedPositive: String? = null,

    @XmlElement(true)
    @XmlSerialName("AMOUNT")
    var amount: String? = null,

    @XmlElement(true)
    @XmlSerialName("GSTOVRDNNATURE")
    var gstOvrdnNature: String? = null,

    @XmlElement(true)
    @XmlSerialName("BANKALLOCATIONS.LIST")
    var bankAllocationList: List<BankAllocation>? = null,

    @XmlElement(true)
    @XmlSerialName("BILLALLOCATIONS.LIST")
    var billAllocation: BillAllocation? = null,

    @XmlElement(true)
    @XmlSerialName("TAXOBJECTALLOCATIONS.LIST")
    var taxAllocation: TaxAllocation? = null,

    @XmlElement(true)
    @XmlSerialName("BASICRATEOFINVOICETAX.LIST")
    var basicRateOfInvoiceTax: BasicRateOfInvoiceTax? = null,

    @XmlElement(true)
    @XmlSerialName("INVENTORYALLOCATIONS.LIST")
    var inventoryAllocations: List<InventoryAllocation>? = null,

    @XmlElement(true)
    @XmlSerialName("RATEDETAILS.LIST")
    var rateDetailsList: List<RateDetail>? = null,

    )