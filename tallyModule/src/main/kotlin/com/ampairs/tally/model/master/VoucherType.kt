package com.ampairs.tally.model.master

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
data class VoucherType(
    @XmlElement(false)
    @XmlSerialName("NAME")
    var name: String? = null,

    @XmlElement(false)
    @XmlSerialName("RESERVEDNAME")
    var reservedName: String? = null,

    @XmlElement(true)
    @XmlSerialName("NAME.LIST")
    var nameList: List<Name>? = null,

    @XmlElement(true)
    @XmlSerialName("ADDITIONALNAME")
    var additionalName: String? = null,

    @XmlElement(true)
    @XmlSerialName("PARENT")
    var parent: String? = null,

    @XmlElement(true)
    @XmlSerialName("NUMBERINGMETHOD")
    var numberingMethod: String? = null,

    @XmlElement(true)
    @XmlSerialName("ISDEEMEDPOSITIVE")
    var isDeemedPositive: String? = null,

    @XmlElement(true)
    @XmlSerialName("AFFECTSTOCK")
    var affectStock: String? = null,

    @XmlElement(true)
    @XmlSerialName("PREVENTDUPLICATES")
    var preventDuplicates: String? = null,

    @XmlElement(true)
    @XmlSerialName("PREFILLZERO")
    var prefillZero: String? = null,

    @XmlElement(true)
    @XmlSerialName("PRINTAFTERSAVE")
    var printAfterSave: String? = null,

    @XmlElement(true)
    @XmlSerialName("FORMALRECEIPT")
    var formalReceipt: String? = null,

    @XmlElement(true)
    @XmlSerialName("ISOPTIONAL")
    var isOptional: String? = null,

    @XmlElement(true)
    @XmlSerialName("ASMFGJRNL")
    var asmfgjrnl: String? = null,

    @XmlElement(true)
    @XmlSerialName("EFFECTIVEDATE")
    var effectiveDate: String? = null,

    @XmlElement(true)
    @XmlSerialName("COMMONNARRATION")
    var commonNarration: String? = null,

    @XmlElement(true)
    @XmlSerialName("MULTINARRATION")
    var multiNarration: String? = null,

    @XmlElement(true)
    @XmlSerialName("ISTAXINVOICE")
    var isTaxInvoice: String? = null,

    @XmlElement(true)
    @XmlSerialName("USEFORPOSINVOICE")
    var useForPosInvoice: String? = null,

    @XmlElement(true)
    @XmlSerialName("USEFOREXCISETRADINGINVOICE")
    var useForExciseTradingInvoice: String? = null,

    @XmlElement(true)
    @XmlSerialName("SORTPOSITION")
    var sortPosition: String? = null,

    @XmlElement(true)
    @XmlSerialName("BEGINNINGNUMBER")
    var beginningNumber: String? = null,
)
