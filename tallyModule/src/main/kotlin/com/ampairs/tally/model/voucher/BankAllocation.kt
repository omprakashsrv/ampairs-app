package com.ampairs.tally.model.voucher

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
data class BankAllocation(
    @XmlElement(false)
    @XmlSerialName("DATE")
    var date: String? = null,

    @XmlElement(false)
    @XmlSerialName("INSTRUMENTDATE")
    var instrumentDate: String? = null,

    @XmlElement(false)
    @XmlSerialName("TRANSACTIONTYPE")
    var transactionType: String? = null,

    @XmlElement(false)
    @XmlSerialName("BANKNAME")
    var bankName: String? = null,

    @XmlElement(false)
    @XmlSerialName("PAYMENTFAVOURING")
    var paymentFavouring: String? = null,

    @XmlElement(false)
    @XmlSerialName("INSTRUMENTNUMBER")
    var instrumentNumber: String? = null,

    @XmlElement(false)
    @XmlSerialName("UNIQUEREFERENCENUMBER")
    var uniqueReferenceNumber: String? = null,

    @XmlElement(false)
    @XmlSerialName("PAYMENTMODE")
    var paymentMode: String? = null,

    @XmlElement(false)
    @XmlSerialName("STATUS")
    var status: String? = null,

    @XmlElement(false)
    @XmlSerialName("BANKPARTYNAME")
    var bankPartyName: String? = null,

    @XmlElement(false)
    @XmlSerialName("AMOUNT")
    var amount: String? = null,
)
