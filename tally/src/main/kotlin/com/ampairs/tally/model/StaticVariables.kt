package com.ampairs.tally.model

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
data class StaticVariables(
    @XmlElement(true)
    @XmlSerialName("SVCURRENTCOMPANY")
    var svCurrenCompany: String? = null,
    @XmlElement(true)
    @XmlSerialName("SVEXPORTFORMAT")
    var format: String? = null,
    @XmlElement(true)
    @XmlSerialName("ISITEMWISE")
    var isItemWise: String? = null,
)