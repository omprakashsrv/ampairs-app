package com.ampairs.tally.model.master

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
data class Ledger(
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
    @XmlSerialName("ADDRESS.LIST")
    var addressList: List<Address>? = null,

    @XmlElement(true)
    @XmlSerialName("ADDITIONALNAME")
    var additionalName: String? = null,

    @XmlElement(true)
    @XmlSerialName("CURRENCYNAME")
    var currencyName: String? = null,

    @XmlElement(true)
    @XmlSerialName("STATENAME")
    var stateName: String? = null,

    @XmlElement(true)
    @XmlSerialName("BANKACCHOLDERNAME")
    var bankAccHolderName: String? = null,

    @XmlElement(true)
    @XmlSerialName("LEDSTATENAME")
    var ledStateName: String? = null,

    @XmlElement(true)
    @XmlSerialName("PARTYGSTIN")
    var partyGstin: String? = null,

    @XmlElement(true)
    @XmlSerialName("GUID")
    var guid: String? = null,

    @XmlElement(true)
    @XmlSerialName("PINCODE")
    var pinCode: String? = null,

    @XmlElement(true)
    @XmlSerialName("COUNTRYNAME")
    var countryName: String? = null,

    @XmlElement(true)
    @XmlSerialName("GSTREGISTRATIONTYPE")
    var gstRegistrationType: String? = null,

    @XmlElement(true)
    @XmlSerialName("IFSCODE")
    var ifsCode: String? = null,

    @XmlElement(true)
    @XmlSerialName("BANKDETAILS")
    var bankDetails: String? = null,

    @XmlElement(true)
    @XmlSerialName("BANKBRANCHNAME")
    var bankBranchName: String? = null,

    @XmlElement(true)
    @XmlSerialName("COUNTRYOFRESIDENCE")
    var countryOfResidence: String? = null,

    @XmlElement(true)
    @XmlSerialName("INCOMETAXNUMBER")
    var incomeTaxNumber: String? = null,

    @XmlElement(true)
    @XmlSerialName("PANAPPLICABLEFROM")
    var panApplicableFrom: String? = null,

    @XmlElement(true)
    @XmlSerialName("NAMEONPAN")
    var nameOnPAN: String? = null,

    @XmlElement(true)
    @XmlSerialName("SALESTAXNUMBER")
    var salesTaxNumber: String? = null,

    @XmlElement(true)
    @XmlSerialName("VATTINNUMBER")
    var vatTinNumber: String? = null,

    @XmlElement(true)
    @XmlSerialName("PARENT")
    var parent: String? = null,

    @XmlElement(true)
    @XmlSerialName("GSTDUTYHEAD")
    var gstDutyHead: String? = null,

    @XmlElement(true)
    @XmlSerialName("RATEOFTAXCALCULATION")
    var rateOfTaxCalculation: String? = null,

    @XmlElement(true)
    @XmlSerialName("SERVICECATEGORY")
    var serviceCategory: String? = null,

    @XmlElement(true)
    @XmlSerialName("TAXTYPE")
    var taxType: String? = null,

    @XmlElement(true)
    @XmlSerialName("GSTAPPLICABLE")
    var gstApplicable: String? = null,

    @XmlElement(true)
    @XmlSerialName("GSTTYPEOFSUPPLY")
    var gstTypeofSupply: String? = null,

    @XmlElement(true)
    @XmlSerialName("TRADERLEDNATUREOFPURCHASE")
    var traderLedNatureOfPurchase: String? = null,

    @XmlElement(true)
    @XmlSerialName("TDSDEDUCTEETYPE")
    var tdsDeducteeType: String? = null,

    @XmlElement(true)
    @XmlSerialName("TDSRATENAME")
    var tdsRateName: String? = null,

    @XmlElement(true)
    @XmlSerialName("LEDGERFBTCATEGORY")
    var ledgerFBTCategory: String? = null,

    @XmlElement(true)
    @XmlSerialName("ISBILLWISEON")
    var isBillWiseOn: String? = null,

    @XmlElement(true)
    @XmlSerialName("ISCOSTCENTRESON")
    var isCostCentresOn: String? = null,

    @XmlElement(true)
    @XmlSerialName("ISINTERESTON")
    var isInterestOn: String? = null,

    @XmlElement(true)
    @XmlSerialName("ALLOWINMOBILE")
    var allowInMobile: String? = null,

    @XmlElement(true)
    @XmlSerialName("ISCONDENSED")
    var isCondensed: String? = null,

    @XmlElement(true)
    @XmlSerialName("AFFECTSSTOCK")
    var affectsStock: String? = null,

    @XmlElement(true)
    @XmlSerialName("FORPAYROLL")
    var forPayRoll: String? = null,

    @XmlElement(true)
    @XmlSerialName("INTERESTONBILLWISE")
    var interestOnBillWise: String? = null,

    @XmlElement(true)
    @XmlSerialName("OVERRIDEINTEREST")
    var overrideInterest: String? = null,

    @XmlElement(true)
    @XmlSerialName("OVERRIDEADVINTEREST")
    var overrideAdvInterest: String? = null,

    @XmlElement(true)
    @XmlSerialName("USEFORVAT")
    var useForVat: String? = null,

    @XmlElement(true)
    @XmlSerialName("ISTCSAPPLICABLE")
    var isTCSApplicable: String? = null,

    @XmlElement(true)
    @XmlSerialName("TCSAPPLICABLE")
    var tcsApplicable: String? = null,

    @XmlElement(true)
    @XmlSerialName("ISTDSAPPLICABLE")
    var isTDSApplicable: String? = null,

    @XmlElement(true)
    @XmlSerialName("ISFBTAPPLICABLE")
    var isFBTApplicable: String? = null,

    @XmlElement(true)
    @XmlSerialName("ISGSTAPPLICABLE")
    var isGSTApplicable: String? = null,

    @XmlElement(true)
    @XmlSerialName("SHOWINPAYSLIP")
    var showInPayslip: String? = null,

    @XmlElement(true)
    @XmlSerialName("USEFORGRATUITY")
    var useForGratuity: String? = null,

    @XmlElement(true)
    @XmlSerialName("FORSERVICETAX")
    var forServiceTax: String? = null,

    @XmlElement(true)
    @XmlSerialName("ISINPUTCREDIT")
    var isInputCredit: String? = null,

    @XmlElement(true)
    @XmlSerialName("ISEXEMPTED")
    var isExempted: String? = null,

    @XmlElement(true)
    @XmlSerialName("TDSDEDUCTEEISSPECIALRATE")
    var tdsDeducteeIsSpecialRate: String? = null,

    @XmlElement(true)
    @XmlSerialName("AUDITED")
    var audited: String? = null,

    @XmlElement(true)
    @XmlSerialName("ASORIGINAL")
    var asOriginal: String? = null,

    @XmlElement(true)
    @XmlSerialName("SORTPOSITION")
    var sortPosition: String? = null,

    @XmlElement(true)
    @XmlSerialName("LEDGERMOBILE")
    var ledgerMobile: String? = null,

    @XmlElement(true)
    @XmlSerialName("LEDGERPHONE")
    var ledgerPhone: String? = null,

    @XmlElement(true)
    @XmlSerialName("TCSCATEGORYDETAILS.LIST")
    private var tcsCategoryDetailList: MutableList<TCSCategoryDetail>? = null,
)
