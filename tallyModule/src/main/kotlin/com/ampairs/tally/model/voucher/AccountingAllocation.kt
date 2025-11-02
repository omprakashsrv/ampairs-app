package com.ampairs.tally.model.voucher


import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
data class AccountingAllocation(
    @XmlElement(true)
    @XmlSerialName("LEDGERNAME")
    var ledgerName: String? = null,

    @XmlElement(true)
    @XmlSerialName("ISDEEMEDPOSITIVE")
    private var isDeemedPositive: String? = null,

    @XmlElement(true)
    @XmlSerialName("ISPARTYLEDGER")
    var isPartyLedger: String? = null,

    @XmlElement(true)
    @XmlSerialName("ISLASTDEEMEDPOSITIVE")
    private var isLastDeemedPositive: String? = null,

    @XmlElement(true)
    @XmlSerialName("AMOUNT")
    var amount: String? = null,
)
