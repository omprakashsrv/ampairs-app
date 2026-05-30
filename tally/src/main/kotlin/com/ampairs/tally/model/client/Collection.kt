package com.ampairs.tally.model.client

import com.ampairs.tally.model.master.GSTClassification
import com.ampairs.tally.model.master.Group
import com.ampairs.tally.model.master.Ledger
import com.ampairs.tally.model.master.StockCategory
import com.ampairs.tally.model.master.StockGroup
import com.ampairs.tally.model.master.StockItem
import com.ampairs.tally.model.master.TDSRate
import com.ampairs.tally.model.master.Unit
import com.ampairs.tally.model.voucher.Voucher
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName


@Serializable
data class Collection(
    @XmlElement(false)
    @XmlSerialName("ISMODIFY")
    var isModify: String = "No",
    @XmlElement(false)
    @XmlSerialName("ISFIXED")
    var isFixed: String = "No",
    @XmlElement(false)
    @XmlSerialName("ISINITIALIZE")
    var isInitialize: String = "No",
    @XmlElement(false)
    @XmlSerialName("ISOPTION")
    var isOption: String = "No",
    @XmlElement(false)
    @XmlSerialName("ISINTERNAL")
    var isInternal: String = "No",
    @XmlElement(false)
    @XmlSerialName("NAME")
    var name: String = "",
    @XmlElement(true)
    @XmlSerialName("TYPE")
    var type: String = "",
    @XmlElement(true)
    @XmlSerialName("NATIVEMETHOD")
    var nativeMethod: List<String> = listOf("*"),


    @XmlElement(true)
    @XmlSerialName("VOUCHER")
    val voucher: List<Voucher>? = null,

    @XmlElement(true)
    @XmlSerialName("LEDGER")
    val ledgers: List<Ledger>? = null,

    @XmlElement(true)
    @XmlSerialName("UNIT")
    val units: List<Unit>? = null,

    @XmlElement(true)
    @XmlSerialName("STOCKGROUP")
    val stockGroups: List<StockGroup>? = null,

    @XmlElement(true)
    @XmlSerialName("STOCKCATEGORY")
    val stockCategories: List<StockCategory>? = null,

    @XmlElement(true)
    @XmlSerialName("STOCKITEM")
    val stockItems: List<StockItem>? = null,

    @XmlElement(true)
    @XmlSerialName("GSTCLASSIFICATION")
    val gstClassifications: List<GSTClassification>? = null,

    @XmlElement(true)
    @XmlSerialName("TDSRATE")
    val tdsRates: List<TDSRate>? = null,

    @XmlElement(true)
    @XmlSerialName("GROUP")
    val groups: List<Group>? = null,
)