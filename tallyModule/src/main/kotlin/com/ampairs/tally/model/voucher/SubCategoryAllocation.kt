package com.ampairs.tally.model.voucher


import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
class SubCategoryAllocation(

    @XmlElement(true)
    @XmlSerialName("SUBCATEGORY")
    var subCategory: String? = null,

    @XmlElement(true)
    @XmlSerialName("DUTYLEDGER")
    var dutyLedger: String? = null,

    @XmlElement(true)
    @XmlSerialName("TAXRATE")
    val taxRate: String? = null,

    @XmlElement(true)
    @XmlSerialName("ASSESSABLEAMOUNT")
    val assessableAmount: String? = null,

    @XmlElement(true)
    @XmlSerialName("TAX")
    val tax: String? = null,
)
