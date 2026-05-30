package com.ampairs.tally.model.voucher


import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
data class BasicRateOfInvoiceTax(
    @XmlElement(true)
    @XmlSerialName("BASICRATEOFINVOICETAX")
    var basicRateOfInvoiceTax: String?,
)
