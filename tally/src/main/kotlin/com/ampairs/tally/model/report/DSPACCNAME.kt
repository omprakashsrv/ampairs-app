package com.ampairs.tally.model.report

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
data class DSPACCNAME(
    @XmlElement(true)
    @XmlSerialName("DSPDISPNAME")
    var name: String? = null,
)