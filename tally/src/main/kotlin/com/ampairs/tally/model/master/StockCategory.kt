package com.ampairs.tally.model.master


import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
data class StockCategory(
    @XmlElement(false)
    @XmlSerialName("NAME")
    var name: String? = null,

    @XmlElement(false)
    @XmlSerialName("RESERVEDNAME")
    var reservedName: String? = null,

    @XmlElement(true)
    @XmlSerialName("GUID")
    var guid: String? = null,

    @XmlElement(true)
    @XmlSerialName("LANGUAGENAME.LIST")
    var languageNameList: List<NameList>? = null,

    @XmlElement(true)
    @XmlSerialName("PARENT")
    var parent: String? = null,

    @XmlElement(true)
    @XmlSerialName("ALTERID")
    var alterId: String? = null,
)
