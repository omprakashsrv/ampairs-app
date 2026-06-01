package com.ampairs.tally.model.master

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
data class HsnDetail(
    @XmlElement(true)
    @XmlSerialName("APPLICABLEFROM")
    var applicableFrom: String? = null,

    @XmlElement(true)
    @XmlSerialName("HSNCODE")
    var hsnCode: String? = null,

    @XmlElement(true)
    @XmlSerialName("HSNMASTERNAME")
    var hsnMasterName: String? = null,

    @XmlElement(true)
    @XmlSerialName("SRCOFHSNDETAILS")
    var srcOfHsnDetails: String? = null,
)
