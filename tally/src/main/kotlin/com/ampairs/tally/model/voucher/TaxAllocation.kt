package com.ampairs.tally.model.voucher


import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
data class TaxAllocation(
    @XmlElement(true)
    @XmlSerialName("CATEGORY")
    var category: String? = null,

    @XmlElement(true)
    @XmlSerialName("TAXTYPE")
    var taxType: String? = null,

    @XmlElement(true)
    @XmlSerialName("TAXNAME")
    var taxName: String? = null,

    @XmlElement(true)
    @XmlSerialName("PARTYLEDGER")
    var partyLedger: String? = null,

    @XmlElement(true)
    @XmlSerialName("EXPENSES")
    var expenses: String? = null,

    @XmlElement(true)
    @XmlSerialName("REFTYPE")
    var refType: String? = null,

    @XmlElement(true)
    @XmlSerialName("ISPANVALID")
    var isPanValid: String? = null,

    @XmlElement(true)
    @XmlSerialName("ISPANNOTAVAILABLE")
    var isPanNotAvailable: String? = null,

    @XmlElement(true)
    @XmlSerialName("SUBCATEGORYALLOCATION.LIST")
    var subCategoryAllocation: List<SubCategoryAllocation>? = null,
)
