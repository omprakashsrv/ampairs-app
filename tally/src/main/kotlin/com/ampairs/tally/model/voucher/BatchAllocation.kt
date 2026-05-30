package com.ampairs.tally.model.voucher


import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
//@XmlType(propOrder = ["godownName", "batchName", "destinationGodownName", "amount", "actualQty", "billedQty"])
data class BatchAllocation(
    @XmlElement(true)
    @XmlSerialName("GODOWNNAME")
    var godownName: String? = null,

    @XmlElement(true)
    @XmlSerialName("BATCHNAME")
    var batchName: String? = null,

    @XmlElement(true)
    @XmlSerialName("DESTINATIONGODOWNNAME")
    var destinationGodownName: String? = null,

    @XmlElement(true)
    @XmlSerialName("AMOUNT")
    var amount: String? = null,

    @XmlElement(true)
    @XmlSerialName("ACTUALQTY")
    var actualQty: String? = null,

    @XmlElement(true)
    @XmlSerialName("BILLEDQTY")
    var billedQty: String? = null,
)
