package com.ampairs.tally.model.master

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
data class LedgerGstRegDetails(
    @XmlElement(true)
    @XmlSerialName("APPLICABLEFROM")
    var applicableFrom: String? = null,

    @XmlElement(true)
    @XmlSerialName("GSTREGISTRATIONTYPE")
    var gstRegistrationType: String? = null,

    @XmlElement(true)
    @XmlSerialName("STATE")
    var state: String? = null,

    @XmlElement(true)
    @XmlSerialName("PLACEOFSUPPLY")
    var placeOfSupply: String? = null,

    @XmlElement(true)
    @XmlSerialName("GSTIN")
    var gstin: String? = null,
)
