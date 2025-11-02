package com.ampairs.tally.model.voucher


import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName


@Serializable
data class InventoryList(
    @XmlElement(true)
    @XmlSerialName("STOCKITEMNAME")
    var stockItemName: String? = null,

    @XmlElement(true)
    @XmlSerialName("ISDEEMEDPOSITIVE")
    var isDeemedPositive: String? = null,

    @XmlElement(true)
    @XmlSerialName("ISLASTDEEMEDPOSITIVE")
    var isLastDeemedPositive: String? = null,

    @XmlElement(true)
    @XmlSerialName("RATE")
    var rate: String? = null,

    @XmlElement(true)
    @XmlSerialName("AMOUNT")
    var amount: String? = null,

    @XmlElement(true)
    @XmlSerialName("ACTUALQTY")
    var actualQty: String? = null,

    @XmlElement(true)
    @XmlSerialName("BILLEDQTY")
    var billedQty: String? = null,

    @XmlElement(true)
    @XmlSerialName("GODOWNNAME")
    var godownName: String? = null,

    @XmlElement(true)
    @XmlSerialName("DISOCUNT")
    var discount: Double,

    @XmlElement(true)
    @XmlSerialName("ACCOUNTINGALLOCATIONS.LIST")
    var accountingAllocationList: List<AccountingAllocation>? = null,

    @XmlElement(true)
    @XmlSerialName("BATCHALLOCATIONS.LIST")
    var batchAllocationsList: List<BatchAllocation>? = null,

    )
