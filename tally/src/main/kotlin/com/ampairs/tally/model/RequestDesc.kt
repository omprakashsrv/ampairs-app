package com.ampairs.tally.model


import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
data class RequestDesc(
    @XmlElement(true)
    @XmlSerialName("REPORTNAME")
    var reportName: String? = null,

    @XmlElement(true)
    @XmlSerialName("STATICVARIABLES")
    var staticVariables: StaticVariables? = null,
)
