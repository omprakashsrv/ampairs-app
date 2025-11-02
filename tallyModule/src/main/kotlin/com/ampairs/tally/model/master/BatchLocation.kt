package com.ampairs.tally.model.master

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName


@Serializable
data class BatchLocation(
    @XmlElement(true)
    @XmlSerialName("GODOWNNAME")
    var godownName: String? = null,

    @XmlElement(true)
    @XmlSerialName("BATCHNAME")
    var batchName: String? = null,

    @XmlElement(true)
    @XmlSerialName("OPENINGBALANCE")
    var openingBalance: String? = null,

    @XmlElement(true)
    @XmlSerialName("OPENINGVALUE")
    var openingValue: String? = null,

    @XmlElement(true)
    @XmlSerialName("OPENINGRATE")
    var openingRate: String? = null,
)
