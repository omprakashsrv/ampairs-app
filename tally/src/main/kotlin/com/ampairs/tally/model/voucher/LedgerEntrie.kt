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

    // "Yes"/"No" — Tally's own Debit/Credit flag for this ledger line. Unlike AMOUNT (whose sign
    // convention differs between contexts — e.g. OPENINGBALANCE is the reverse of voucher lines),
    // this flag reliably means Debit when "Yes", Credit when "No".
    @XmlElement(true)
    @XmlSerialName("ISDEEMEDPOSITIVE")
    var isDeemedPositive: String? = null,

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

    // A receipt/payment can settle multiple bills, so this is a list (one BILLALLOCATIONS.LIST per bill).
    @XmlElement(true)
    @XmlSerialName("BILLALLOCATIONS.LIST")
    var billAllocationList: List<BillAllocation>? = null,

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