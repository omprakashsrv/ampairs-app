package com.ampairs.tally.model.master


import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
data class RateDetail(
    @XmlElement(true)
    @XmlSerialName("GSTRATEDUTYHEAD")
    var gstRateDutyHead: String? = null,

    @XmlElement(true)
    @XmlSerialName("GSTRATEVALUATIONTYPE")
    var gstRateValuationType: String? = null,

    @XmlElement(true)
    @XmlSerialName("GSTRATE")
    var gstRate: String? = null,
)
