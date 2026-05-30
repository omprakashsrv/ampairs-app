package com.ampairs.tally.model.master


import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
data class CategoryDetail(
    @XmlElement(true)
    @XmlSerialName("APPLICABLEFROM")
    var applicableFrom: String? = null,

    @XmlElement(true)
    @XmlSerialName("")
    var isZeroRated: String? = null,

    @XmlElement(true)
    @XmlSerialName("ISNORMALRATEAPPLICABLE")
    var isNormalRateApplicable: String? = null,

    @XmlElement(true)
    @XmlSerialName("RATEOFTAX")
    var rateOfTax: String? = null,

    @XmlElement(true)
    @XmlSerialName("OTHERRATE")
    var otherRate: String? = null,

    @XmlElement(true)
    @XmlSerialName("NOPANRATEOFTAX")
    var noPANRateOfTax: String? = null,

    @XmlElement(true)
    @XmlSerialName("NOPANOTHERRATE")
    var noPANOtherRate: String? = null,
)
