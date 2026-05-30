package com.ampairs.tally.model.master


import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
data class NameList(
    @XmlElement(true)
    @XmlSerialName("NAME.LIST")
    var nameList: List<Name>? = null,
)
