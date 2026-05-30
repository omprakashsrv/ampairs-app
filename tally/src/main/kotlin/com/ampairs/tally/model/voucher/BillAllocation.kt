package com.ampairs.tally.model.voucher

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
//@XmlType(propOrder = ["name", "billType", "billCreditPeriod", "tdsDeducteeIsSpecialRate", "amount"])
data class BillAllocation(
    @XmlElement(true)
    @XmlSerialName("NAME")
    var name: String? = null,

    @XmlElement(true)
    @XmlSerialName("BILLTYPE")
    var billType: String? = null,

    @XmlElement(true)
    @XmlSerialName("BILLCREDITPERIOD")
    var billCreditPeriod: String? = null,

    @XmlElement(true)
    @XmlSerialName("TDSDEDUCTEEISSPECIALRATE")
    var tdsDeducteeIsSpecialRate: String? = null,

    @XmlElement(true)
    @XmlSerialName("AMOUNT")
    var amount: String? = null,
)
