package com.ampairs.tally.model.master

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
data class StockItem(
    @XmlElement(false)
    @XmlSerialName("NAME")
    var name: String? = null,

    @XmlElement(false)
    @XmlSerialName("RESERVEDNAME")
    var reservedName: String? = null,

    @XmlElement(true)
    @XmlSerialName("GUID")
    var guid: String? = null,

    @XmlElement(true)
    @XmlSerialName("PARENT")
    var parent: String? = null,

    @XmlElement(true)
    @XmlSerialName("CATEGORY")
    var category: String? = null,

    @XmlElement(true)
    @XmlSerialName("GSTAPPLICABLE")
    var gstApplicable: String? = null,

    @XmlElement(true)
    @XmlSerialName("TCSAPPLICABLE")
    var tcsApplicable: String? = null,

    @XmlElement(true)
    @XmlSerialName("TCSCATEGORY")
    var tcsCategory: String? = null,

    @XmlElement(true)
    @XmlSerialName("GSTTYPEOFSUPPLY")
    var gstTypeOfSupply: String? = null,

    @XmlElement(true)
    @XmlSerialName("BASEUNITS")
    var baseUnits: String? = null,

    @XmlElement(true)
    @XmlSerialName("ADDITIONALUNITS")
    var additionalUnits: String? = null,

    @XmlElement(true)
    @XmlSerialName("GSTREPUOM")
    var gstRepUOM: String? = null,

    @XmlElement(true)
    @XmlSerialName("DENOMINATOR")
    var denominator: String? = null,

    @XmlElement(true)
    @XmlSerialName("CONVERSION")
    var conversion: String? = null,

    @XmlElement(true)
    @XmlSerialName("TAXABILITY")
    var taxability: String? = null,

    // Tally exposes the standard cost/price as a DATED HISTORY (one .LIST element per "Applicable
    // From" date), so these are lists — mirror of gstDetailList below. The newest-dated entry is the
    // current rate; the full list drives effective-dated price/cost sync.
    @XmlElement(true)
    @XmlSerialName("STANDARDCOSTLIST.LIST")
    var standardCost: List<StandardCost>? = null,

    @XmlElement(true)
    @XmlSerialName("STANDARDPRICELIST.LIST")
    var standardPrice: List<StandardPrice>? = null,

    @XmlElement(true)
    @XmlSerialName("GSTDETAILS.LIST")
    var gstDetailList: List<GSTDetail>? = null,

    @XmlElement(true)
    @XmlSerialName("HSNDETAILS.LIST")
    var hsnDetailList: List<HsnDetail>? = null,

    @XmlElement(true)
    @XmlSerialName("BATCHALLOCATIONS.LIST")
    var batchLocations: List<BatchLocation>? = null,

    @XmlElement(true)
    @XmlSerialName("TCSCATEGORYDETAILS.LIST")
    var tcsCategoryDetailList: List<TCSCategoryDetail>? = null,

    @XmlElement(true)
    @XmlSerialName("LANGUAGENAME.LIST")
    var languageNameList: List<NameList>? = null,

    @XmlElement(true)
    @XmlSerialName("CLOSINGBALANCE")
    var closingBalance: String? = null,

    @XmlElement(true)
    @XmlSerialName("CLOSINGVALUE")
    var closingValue: String? = null,

    @XmlElement(true)
    @XmlSerialName("ALTERID")
    var alterId: String? = null,
)
