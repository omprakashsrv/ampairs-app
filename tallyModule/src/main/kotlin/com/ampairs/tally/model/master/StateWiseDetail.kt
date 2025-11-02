package com.ampairs.tally.model.master


import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
data class StateWiseDetail(
    @XmlElement(true)
    @XmlSerialName("STATENAME")
    var stateName: String? = null,

    @XmlElement(true)
    @XmlSerialName("RATEDETAILS.LIST")
    var rateDetailsList: List<RateDetail>? = null,

    @XmlElement(true)
    @XmlSerialName("GSTSLABRATES.LIST")
    var gstsLabrates: String? = null,
)
