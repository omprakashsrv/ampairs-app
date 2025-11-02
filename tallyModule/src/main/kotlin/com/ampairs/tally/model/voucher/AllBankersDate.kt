package com.ampairs.tally.model.voucher


import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
data class AllBankersDate(
    @XmlElement(false)
    @XmlSerialName("TYPE")
    var type: String? = null,

    @XmlElement(true)
    @XmlSerialName("BASICBANKERSDATE")
    var basicBankersDate: String? = null,
)