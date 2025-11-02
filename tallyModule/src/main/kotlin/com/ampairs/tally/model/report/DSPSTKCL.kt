package com.ampairs.tally.model.report

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName


@Serializable
data class DSPSTKCL(
    @XmlElement(true)
    @XmlSerialName("DSPCLQTY")
    var qty: String? = null,

    @XmlElement(true)
    @XmlSerialName("DSPCLRATE")
    var rate: String? = null,

    @XmlElement(true)
    @XmlSerialName("DSPCLAMTA")
    var amount: String? = null,

    )