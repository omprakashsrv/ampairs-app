package com.ampairs.tally.model

enum class Type(var type: String, var id: String, var nativeMethods: List<String> = listOf("*")) {
    UNIT(
        "UNIT", "CUSTOMUNITCOL",
        listOf("GUID", "DECIMALPLACES", "ALTERID"),
    ),
    STOCK_GROUP(
        "STOCKGROUP", "CUSTOMSTOCKGROUPCOL",
        listOf("GUID", "PARENT", "ALTERID"),
    ),
    STOCK_CATEGORY(
        "STOCKCATEGORY", "CUSTOMSTOCKCATEGORYCOL",
        listOf("GUID", "PARENT", "ALTERID"),
    ),
    STOCK_ITEM(
        "STOCKITEM", "CUSTOMSTOCKITEMCOL",
        listOf(
            "GUID", "PARENT", "CATEGORY",
            "BASEUNITS", "ADDITIONALUNITS",
            "GSTAPPLICABLE", "GSTTYPEOFSUPPLY",
            "ALTERID",
            "STANDARDCOSTLIST.LIST",
            "STANDARDPRICELIST.LIST",
            "GSTDETAILS.LIST",
            "HSNDETAILS.LIST",
        ),
    ),
    LEDGER(
        "LEDGER", "CUSTOMLEDGERCOL",
        listOf(
            "GUID", "PARENT",
            "LEDGERMOBILE", "LEDGERPHONE", "PARTYGSTIN",
            "ADDRESS.LIST",
            "LEDMAILINGDETAILS.LIST",
            "LEDGSTREGDETAILS.LIST",
            "ALTERID",
        ),
    ),
    GST_CLASSIFICATION("GSTCLASSIFICATION", "CUSTOMGSTCLASSIFICATIONCOL"),
    TDS_RATE("TDSRATE", "CUSTOMTDSRATECOL"),
    GROUP(
        "GROUP", "CUSTOMGROUPCOL",
        listOf("GUID", "PARENT", "ALTERID"),
    ),
    STOCK_BALANCE("STOCKITEM", "STOCKBALANCECOL"),
}

fun Type.toTallyXML(): TallyXML {
    val tallyXML = TallyXML()
    tallyXML.header?.id = this.id
    tallyXML.body?.desc?.tdl?.tdlMessage?.collection?.name = this.id
    tallyXML.body?.desc?.tdl?.tdlMessage?.collection?.type = this.type
    tallyXML.body?.desc?.tdl?.tdlMessage?.collection?.nativeMethod = this.nativeMethods
    return tallyXML
}
