package com.ampairs.tally.model.master


import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
data class Address(
    @XmlElement(true)
    @XmlSerialName("ADDRESS")
    var address: String? = null,
) {
    override fun toString(): String {
        return "$address"
    }
}
