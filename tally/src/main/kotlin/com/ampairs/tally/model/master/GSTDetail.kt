package com.ampairs.tally.model.master


import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
data class GSTDetail(
    @XmlElement(true)
    @XmlSerialName("APPLICABLEFROM")
    var applicableFrom: String? = null,

    @XmlElement(true)
    @XmlSerialName("HSNMASTERNAME")
    var hsnMasterName: String? = null,

    @XmlElement(true)
    @XmlSerialName("HSNCODE")
    var hsnCode: String? = null,

    @XmlElement(true)
    @XmlSerialName("TAXABILITY")
    var taxability: String? = null,

    @XmlElement(true)
    @XmlSerialName("ISREVERSECHARGEAPPLICABLE")
    var isReverseChargeApplicable: String? = null,

    @XmlElement(true)
    @XmlSerialName("ISNONGSTGOODS")
    var isNonGstGoods: String? = null,

    @XmlElement(true)
    @XmlSerialName("GSTINELIGIBLEITC")
    var gstInEligibleitc: String? = null,

    @XmlElement(true)
    @XmlSerialName("STATEWISEDETAILS.LIST")
    var stateWiseDetailsList: List<StateWiseDetail>? = null,
)
