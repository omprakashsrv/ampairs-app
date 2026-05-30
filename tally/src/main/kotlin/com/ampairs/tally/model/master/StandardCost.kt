package com.ampairs.tally.model.master


import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
data class StandardCost(
    @XmlElement(true)
    @XmlSerialName("DATE")
    var date: String? = null,

    @XmlElement(true)
    @XmlSerialName("RATE")
    var rate: String? = null,
)
