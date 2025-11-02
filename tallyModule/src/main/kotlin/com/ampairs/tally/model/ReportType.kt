package com.ampairs.tally.model

enum class ReportType(var type: String) {
    STOCK_SUMMARY("Stock Summary"),
}

fun ReportType.toTallyXML(): TallyXML {
    val tallyXML = TallyXML()
    tallyXML.header?.tallyRequest = "Export Data"
    tallyXML.header?.type = null
    tallyXML.header?.version = null
    tallyXML.body?.desc = null
    tallyXML.body?.exportData = ExportData()
    tallyXML.body?.exportData!!.requestDesc = RequestDesc()
    tallyXML.body?.exportData!!.requestDesc!!.reportName = this.type
    tallyXML.body?.exportData!!.requestDesc!!.staticVariables = StaticVariables()
    tallyXML.body?.exportData!!.requestDesc!!.staticVariables!!.format = "\$\$SysName:XML"
    tallyXML.body?.exportData!!.requestDesc!!.staticVariables!!.isItemWise = "Yes"
    return tallyXML
}