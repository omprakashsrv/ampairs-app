package com.ampairs.tally.model

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
data class TallyMessage(
    @XmlElement(false)
    @XmlSerialName("xmlns:UDF")
    val xmlsUdf: String? = null,

    @XmlElement(true)
    @XmlSerialName("VOUCHER")
    val voucher: Voucher? = null,

    @XmlElement(true)
    @XmlSerialName("LEDGER")
    val ledger: Ledger? = null,

    @XmlElement(true)
    @XmlSerialName("UNIT")
    val unit: Unit? = null,

    @XmlElement(true)
    @XmlSerialName("STOCKGROUP")
    val stockGroup: StockGroup? = null,

    @XmlElement(true)
    @XmlSerialName("STOCKCATEGORY")
    val stockCategory: StockCategory? = null,

    @XmlElement(true)
    @XmlSerialName("STOCKITEM")
    val stockItem: StockItem? = null,

    @XmlElement(true)
    @XmlSerialName("GSTCLASSIFICATION")
    val gstClassification: GSTClassification? = null,

    @XmlElement(true)
    @XmlSerialName("TDSRATE")
    val tdsRate: TDSRate? = null,

    @XmlElement(true)
    @XmlSerialName("GROUP")
    val group: Group? = null,

    )
