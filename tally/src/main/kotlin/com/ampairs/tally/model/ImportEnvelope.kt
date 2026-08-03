package com.ampairs.tally.model

/**
 * Builds a Tally `IMPORT` envelope for pushing vouchers *into* Tally (the inverse of the EXPORT
 * collection queries built by [Type.toTallyXML]). The shape Tally expects is:
 *
 * ```xml
 * <ENVELOPE>
 *   <HEADER><TALLYREQUEST>Import Data</TALLYREQUEST></HEADER>
 *   <BODY>
 *     <IMPORTDATA>
 *       <REQUESTDESC>
 *         <REPORTNAME>Vouchers</REPORTNAME>
 *         <STATICVARIABLES><SVCURRENTCOMPANY>Acme</SVCURRENTCOMPANY></STATICVARIABLES>
 *       </REQUESTDESC>
 *       <REQUESTDATA>
 *         <TALLYMESSAGE><VOUCHER .../></TALLYMESSAGE>
 *       </REQUESTDATA>
 *     </IMPORTDATA>
 *   </BODY>
 * </ENVELOPE>
 * ```
 *
 * [Body.desc] defaults to a non-null `DESC` element (used by EXPORT); it MUST be null here or Tally
 * rejects the request. [Header.type]/id are irrelevant to imports and are cleared. When
 * [companyName] is blank the `SVCURRENTCOMPANY` variable is omitted so Tally targets its active
 * company.
 */
fun buildVoucherImport(
    vouchers: List<com.ampairs.tally.model.voucher.Voucher>,
    companyName: String? = null,
): TallyXML {
    val messages: MutableList<TallyMessage?> =
        vouchers.map { TallyMessage(xmlsUdf = "TallyUDF", voucher = it) as TallyMessage? }.toMutableList()

    return TallyXML(
        header = Header(
            version = "1",
            tallyRequest = "Import Data",
            type = null,
            status = null,
            id = "",
        ),
        body = Body(
            importData = ImportData(
                requestDesc = RequestDesc(
                    reportName = "Vouchers",
                    staticVariables = companyName?.trim()?.takeIf { it.isNotBlank() }?.let {
                        StaticVariables(svCurrenCompany = it)
                    },
                ),
                requestData = RequestData(tallyMessage = messages),
            ),
            // EXPORT-only element — must be absent for an IMPORT request.
            desc = null,
        ),
    )
}
