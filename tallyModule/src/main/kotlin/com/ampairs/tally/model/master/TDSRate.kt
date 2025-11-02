package com.ampairs.tally.model.master

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
data class TDSRate(
    @XmlElement(false)
    @XmlSerialName("NAME")
    var name: String? = null,

    @XmlElement(false)
    @XmlSerialName("RESERVEDNAME")
    var reservedName: String? = null,

    @XmlElement(true)
    @XmlSerialName("ASORIGINAL")
    var asOriginal: String? = null,

    @XmlElement(true)
    @XmlSerialName("ISACTIVE")
    var isActive: String? = null,

    @XmlElement(true)
    @XmlSerialName("ISFORTCS")
    var isForTCS: String? = null,

    @XmlElement(true)
    @XmlSerialName("NAME.LIST")
    var nameList: List<Name>? = null,

    @XmlElement(true)
    @XmlSerialName("CATEGORYDETAILS.LIST")
    var categoryDetails: List<CategoryDetail>? = null,
)
