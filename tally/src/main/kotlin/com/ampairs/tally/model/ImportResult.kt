package com.ampairs.tally.model

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

/**
 * Tally's `IMPORTDATA` response summary (`<BODY><DATA><IMPORTRESULT>`). Non-zero [errors] /
 * [exceptions] (or a non-blank [lineError]) mean the import was rejected — a bad ledger/stock-item
 * name, a duplicate voucher number, or a malformed line. [created] + [altered] is how many vouchers
 * actually landed. [lastVchId] is the internal id of the last voucher written (diagnostic only).
 */
@Serializable
data class ImportResult(
    @XmlElement(true) @XmlSerialName("CREATED") var created: Int? = null,
    @XmlElement(true) @XmlSerialName("ALTERED") var altered: Int? = null,
    @XmlElement(true) @XmlSerialName("DELETED") var deleted: Int? = null,
    @XmlElement(true) @XmlSerialName("COMBINED") var combined: Int? = null,
    @XmlElement(true) @XmlSerialName("IGNORED") var ignored: Int? = null,
    @XmlElement(true) @XmlSerialName("ERRORS") var errors: Int? = null,
    @XmlElement(true) @XmlSerialName("EXCEPTIONS") var exceptions: Int? = null,
    @XmlElement(true) @XmlSerialName("LASTVCHID") var lastVchId: String? = null,
    @XmlElement(true) @XmlSerialName("LASTMID") var lastMId: String? = null,
    @XmlElement(true) @XmlSerialName("LINEERROR") var lineError: String? = null,
) {
    /** True when Tally reported no errors/exceptions and at least one voucher was created or altered. */
    val isSuccess: Boolean
        get() = (errors ?: 0) == 0 && (exceptions ?: 0) == 0 &&
            lineError.isNullOrBlank() && ((created ?: 0) + (altered ?: 0)) > 0
}

/**
 * Tally's `IMPORTDATA` reply for a voucher push arrives as a bare `<RESPONSE>` root (NOT wrapped in
 * `<ENVELOPE><BODY><DATA><IMPORTRESULT>`), carrying the same counters. Deserializing it as [TallyXML]
 * throws `root tag "RESPONSE" does not match expected "ENVELOPE"`, so this model parses that shape
 * directly. Field set mirrors [ImportResult]; [toImportResult] normalizes the two shapes into one.
 */
@Serializable
@XmlSerialName("RESPONSE")
data class ImportResponse(
    @XmlElement(true) @XmlSerialName("CREATED") var created: Int? = null,
    @XmlElement(true) @XmlSerialName("ALTERED") var altered: Int? = null,
    @XmlElement(true) @XmlSerialName("DELETED") var deleted: Int? = null,
    @XmlElement(true) @XmlSerialName("COMBINED") var combined: Int? = null,
    @XmlElement(true) @XmlSerialName("IGNORED") var ignored: Int? = null,
    @XmlElement(true) @XmlSerialName("ERRORS") var errors: Int? = null,
    @XmlElement(true) @XmlSerialName("EXCEPTIONS") var exceptions: Int? = null,
    @XmlElement(true) @XmlSerialName("LASTVCHID") var lastVchId: String? = null,
    @XmlElement(true) @XmlSerialName("LASTMID") var lastMId: String? = null,
    @XmlElement(true) @XmlSerialName("LINEERROR") var lineError: String? = null,
) {
    fun toImportResult(): ImportResult = ImportResult(
        created = created, altered = altered, deleted = deleted, combined = combined,
        ignored = ignored, errors = errors, exceptions = exceptions,
        lastVchId = lastVchId, lastMId = lastMId, lineError = lineError,
    )
}
