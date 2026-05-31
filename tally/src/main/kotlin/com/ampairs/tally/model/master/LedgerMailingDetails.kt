package com.ampairs.tally.model.master

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
data class LedgerMailingDetails(
    @XmlElement(true)
    @XmlSerialName("MAILINGNAME")
    var mailingName: String? = null,

    @XmlElement(true)
    @XmlSerialName("ADDRESS.LIST")
    var addressList: List<Address>? = null,

    @XmlElement(true)
    @XmlSerialName("STATENAME")
    var stateName: String? = null,

    @XmlElement(true)
    @XmlSerialName("COUNTRYNAME")
    var countryName: String? = null,

    @XmlElement(true)
    @XmlSerialName("PINCODE")
    var pinCode: String? = null,
)
