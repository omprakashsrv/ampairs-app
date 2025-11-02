package com.ampairs.tally.model

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
data class Header(
    @XmlElement(true)
    @XmlSerialName("VERSION")
    var version: String? = "1",
    @XmlElement(true)
    @XmlSerialName("TALLYREQUEST")
    var tallyRequest: String = "EXPORT",
    @XmlElement(true)
    @XmlSerialName("TYPE")
    var type: String? = "COLLECTION",
    @XmlElement(true)
    @XmlSerialName("STATUS")
    var status: String? = null,
    @XmlElement(true)
    @XmlSerialName("ID")
    var id: String = "",
)
