package com.ampairs.tally.model.master


import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
data class Name(
    @XmlElement(true)
    @XmlSerialName("NAME")
    var name: String? = null,
)
