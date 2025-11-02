package com.ampairs.tally.model.master


import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
data class Group(
    @XmlElement(false)
    @XmlSerialName("NAME")
    var name: String? = null,

    @XmlElement(false)
    @XmlSerialName("RESERVEDNAME")
    var reservedName: String? = null,

    @XmlElement(true)
    @XmlSerialName("PARENT")
    var parent: String? = null,

    @XmlElement(true)
    @XmlSerialName("ISBILLWISEON")
    var isBillwiseOn: String? = null,

    @XmlElement(true)
    @XmlSerialName("ASORIGINAL")
    var asOriginal: String? = null,

    @XmlElement(true)
    @XmlSerialName("ISSUBLEDGER")
    var isSubLedger: String? = null,

    @XmlElement(true)
    @XmlSerialName("TRACKNEGATIVEBALANCES")
    var trackNegativeBalances: String? = null,

    @XmlElement(true)
    @XmlSerialName("LANGUAGENAME.LIST")
    var nameList: List<NameList>? = null,

    @XmlElement(true)
    @XmlSerialName("ADDITIONALNAME")
    var additionalName: String? = null,
)
