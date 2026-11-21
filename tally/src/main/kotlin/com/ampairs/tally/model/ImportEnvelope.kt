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
        // Bare <ENVELOPE> (no Action attribute) to match Tally's documented import envelope exactly.
        action = null,
        // Minimal import header — ONLY <TALLYREQUEST>Import Data</TALLYREQUEST>. Tally rejects the
        // request as "Unknown Request, cannot be processed" when the header also carries the
        // export-style <VERSION> and an empty <ID/> (which the shared Header defaults leaked in).
        header = Header(
            version = null,
            tallyRequest = "Import Data",
            type = null,
            status = null,
            id = null,
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

/**
 * Builds a Tally `IMPORT` envelope for pushing masters (ledgers, stock items, groups, stock
 * groups/categories, units) *into* Tally — the master-data counterpart of [buildVoucherImport].
 * Identical envelope shape; only [reportName] and the message payload differ. [reportName] defaults
 * to Tally's generic "All Masters" report, which accepts a mixed list of master `TALLYMESSAGE`s
 * (e.g. `TallyMessage(ledger = x)` alongside `TallyMessage(stockItem = y)`) in one request.
 */
fun buildMastersImport(
    messages: List<TallyMessage>,
    reportName: String = "All Masters",
    companyName: String? = null,
): TallyXML {
    return TallyXML(
        action = null,
        header = Header(
            version = null,
            tallyRequest = "Import Data",
            type = null,
            status = null,
            id = null,
        ),
        body = Body(
            importData = ImportData(
                requestDesc = RequestDesc(
                    reportName = reportName,
                    staticVariables = companyName?.trim()?.takeIf { it.isNotBlank() }?.let {
                        StaticVariables(svCurrenCompany = it)
                    },
                ),
                requestData = RequestData(tallyMessage = messages.toMutableList<TallyMessage?>()),
            ),
            desc = null,
        ),
    )
}
