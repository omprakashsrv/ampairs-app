package com.ampairs.printing.data.db

import com.ampairs.printing.core.model.ConnectionType
import com.ampairs.printing.core.model.Orientation
import com.ampairs.printing.core.model.PageSize
import com.ampairs.printing.core.model.PaperSpec
import com.ampairs.printing.core.model.PrinterCapabilities
import com.ampairs.printing.core.model.PrinterClass
import com.ampairs.printing.core.model.PrinterProfile
import com.ampairs.printing.core.model.PrintJobState
import com.ampairs.printing.core.model.Template
import com.ampairs.printing.core.spool.PrintJob
import kotlinx.serialization.json.Json

// Entity <-> domain mappers. The @Entity classes live in :data:database (same package); these
// mappers stay in the feature module because they reference the printing core models.

/** Shared JSON for (de)serializing the [Template] aggregate stored in a template row. */
internal val printingJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

fun PrinterEntity.toProfile(): PrinterProfile = PrinterProfile(
    id = id,
    name = name,
    printerClass = enumOrDefault(printerClass, PrinterClass.THERMAL),
    connectionType = enumOrDefault(connectionType, ConnectionType.NETWORK),
    address = address,
    paper = when (paperType) {
        "PAGE" -> PaperSpec.Page(
            size = pageSize?.let { enumOrDefault(it, PageSize.A4) } ?: PageSize.A4,
            orientation = orientation?.let { enumOrDefault(it, Orientation.PORTRAIT) } ?: Orientation.PORTRAIT,
        )
        "LABEL" -> PaperSpec.Label(labelWidthMm ?: 50.0, labelHeightMm ?: 30.0)
        else -> PaperSpec.Thermal(widthChars, widthDots)
    },
    capabilities = PrinterCapabilities(
        charsPerLine = charsPerLine,
        supportsCut = supportsCut,
        supportsCashDrawer = supportsCashDrawer,
        supportsNativeQr = supportsNativeQr,
        supportsNativeBarcode = supportsNativeBarcode,
        codepage = codepage,
        supportsStatus = supportsStatus,
    ),
)

fun PrinterProfile.toEntity(active: Boolean = true): PrinterEntity {
    val paperType = when (paper) {
        is PaperSpec.Page -> "PAGE"
        is PaperSpec.Label -> "LABEL"
        is PaperSpec.Thermal -> "THERMAL"
    }
    val thermal = paper as? PaperSpec.Thermal
    val page = paper as? PaperSpec.Page
    val label = paper as? PaperSpec.Label
    return PrinterEntity(
        id = id,
        name = name,
        printerClass = printerClass.name,
        connectionType = connectionType.name,
        address = address,
        paperType = paperType,
        widthChars = thermal?.widthChars ?: 48,
        widthDots = thermal?.widthDots ?: 576,
        pageSize = page?.size?.name,
        orientation = page?.orientation?.name,
        labelWidthMm = label?.widthMm,
        labelHeightMm = label?.heightMm,
        charsPerLine = capabilities.charsPerLine,
        supportsCut = capabilities.supportsCut,
        supportsCashDrawer = capabilities.supportsCashDrawer,
        supportsNativeQr = capabilities.supportsNativeQr,
        supportsNativeBarcode = capabilities.supportsNativeBarcode,
        codepage = capabilities.codepage,
        supportsStatus = capabilities.supportsStatus,
        active = active,
    )
}

fun PrintJobEntity.toJob(): PrintJob = PrintJob(
    id = id, idempotencyKey = idempotencyKey, documentType = documentType, documentId = documentId,
    templateId = templateId, printerId = printerId, copies = copies,
    state = enumOrDefault(state, PrintJobState.QUEUED), attempts = attempts,
    createdAtMillis = createdAtMillis, updatedAtMillis = updatedAtMillis, lastError = lastError,
)

fun PrintJob.toEntity(): PrintJobEntity = PrintJobEntity(
    id = id, idempotencyKey = idempotencyKey, documentType = documentType, documentId = documentId,
    templateId = templateId, printerId = printerId, copies = copies, state = state.name,
    attempts = attempts, createdAtMillis = createdAtMillis, updatedAtMillis = updatedAtMillis,
    lastError = lastError,
)

fun PrintTemplateEntity.toTemplate(): Template = printingJson.decodeFromString(Template.serializer(), templateJson)

fun Template.toEntity(synced: Boolean = false, active: Boolean = true, updatedAt: String? = null): PrintTemplateEntity =
    PrintTemplateEntity(
        id = id,
        documentType = documentType.name,
        printerClass = printerClass.name,
        name = name,
        templateJson = printingJson.encodeToString(Template.serializer(), this),
        version = version,
        synced = synced,
        active = active,
        updatedAt = updatedAt,
    )

/** Read the embedded `isDefault` from a stored template JSON (false if it can't be parsed). */
fun templateJsonIsDefault(templateJson: String): Boolean =
    runCatching { printingJson.decodeFromString(Template.serializer(), templateJson).isDefault }.getOrDefault(false)

/** Return [templateJson] with its embedded `isDefault` forced to [isDefault] (server column wins on pull). */
fun templateJsonWithDefault(templateJson: String, isDefault: Boolean): String =
    runCatching {
        val t = printingJson.decodeFromString(Template.serializer(), templateJson)
        if (t.isDefault == isDefault) templateJson
        else printingJson.encodeToString(Template.serializer(), t.copy(isDefault = isDefault))
    }.getOrDefault(templateJson)

private inline fun <reified T : Enum<T>> enumOrDefault(name: String, default: T): T =
    enumValues<T>().firstOrNull { it.name == name } ?: default
