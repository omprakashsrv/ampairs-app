package com.ampairs.tally.model.master

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
data class Unit(
    @XmlElement(false)
    var name: String? = null,

    @XmlElement(false)
    @XmlSerialName("RESERVEDNAME")
    var reservedName: String? = null,

    @XmlElement(true)
    @XmlSerialName("GUID")
    var guid: String? = null,

    @XmlElement(true)
    @XmlSerialName("NAME")
    var unitName: String? = null,

    @XmlElement(true)
    @XmlSerialName("GSTREPUOM")
    var gstRepUOM: String? = null,

    @XmlElement(true)
    @XmlSerialName("DECIMALPLACES")
    var decimalPlaces: String? = null,

    @XmlElement(true)
    @XmlSerialName("ISSIMPLEUNIT")
    var isSimpleUnit: String? = null,

    @XmlElement(true)
    @XmlSerialName("ALTERID")
    var alterId: String? = null,
)
