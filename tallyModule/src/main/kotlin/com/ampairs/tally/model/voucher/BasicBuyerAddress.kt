package com.ampairs.tally.model.voucher


import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
data class BasicBuyerAddress(
    @XmlElement(true)
    @XmlSerialName("BASICBUYERADDRESS")
    var basicBuyerAddress: String? = null,
)
