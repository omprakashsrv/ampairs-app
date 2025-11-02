package com.ampairs.tally.model


import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@XmlSerialName("REQUESTDATA")
@Serializable
data class RequestData(
    @XmlElement(true)
    @XmlSerialName("TALLYMESSAGE")
    var tallyMessage: MutableList<com.ampairs.tally.model.TallyMessage?>? = null,
)
