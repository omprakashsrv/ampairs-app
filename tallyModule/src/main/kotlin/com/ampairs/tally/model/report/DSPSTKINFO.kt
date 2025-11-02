package com.ampairs.tally.model.report

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName


@Serializable
data class DSPSTKINFO(
    @XmlElement(true)
    @XmlSerialName("DSPSTKCL")
    var closingStock: DSPSTKCL? = null
)