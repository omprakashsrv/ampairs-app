package com.ampairs.tally.model.voucher

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
//@XmlType(propOrder = ["isDeemedPositive", "isLastDeemedPositive", "stockItemName", "amount", "actualQty", "billedQty", "rate", "batchAllocations", "discount"])
class InventoryAllocation(
    @XmlElement(true)
    @XmlSerialName("ISDEEMEDPOSITIVE")
    var isDeemedPositive: String? = null,

    @XmlElement(true)
    @XmlSerialName("ISLASTDEEMEDPOSITIVE")
    var isLastDeemedPositive: String? = null,

    @XmlElement(true)
    @XmlSerialName("STOCKITEMNAME")
    var stockItemName: String? = null,

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
    @XmlSerialName("RATE")
    var rate: String? = null,

    @XmlElement(true)
    @XmlSerialName("DISCOUNT")
    var discount: String? = null,

    @XmlElement(true)
    @XmlSerialName("BATCHALLOCATIONS.LIST")
    var batchAllocations: List<BatchAllocation>,
)
