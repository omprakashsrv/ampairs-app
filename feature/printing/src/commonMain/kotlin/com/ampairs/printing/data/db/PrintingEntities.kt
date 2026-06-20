package com.ampairs.printing.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
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

/** Shared JSON for (de)serializing the [Template] aggregate stored in a template row. */
internal val printingJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

// --- Printer (device-local, per workspace; never synced) -----------------------------------------

@Entity(tableName = "printers")
data class PrinterEntity(
    @PrimaryKey @ColumnInfo("id") val id: String,
    @ColumnInfo("name") val name: String,
    @ColumnInfo("printer_class") val printerClass: String,
    @ColumnInfo("connection_type") val connectionType: String,
    @ColumnInfo("address") val address: String? = null,
    @ColumnInfo("paper_type") val paperType: String,
    @ColumnInfo("width_chars") val widthChars: Int = 48,
    @ColumnInfo("width_dots") val widthDots: Int = 576,
    @ColumnInfo("page_size") val pageSize: String? = null,
    @ColumnInfo("orientation") val orientation: String? = null,
    @ColumnInfo("label_width_mm") val labelWidthMm: Double? = null,
    @ColumnInfo("label_height_mm") val labelHeightMm: Double? = null,
    @ColumnInfo("chars_per_line") val charsPerLine: Int = 48,
    @ColumnInfo("supports_cut") val supportsCut: Boolean = true,
    @ColumnInfo("supports_cash_drawer") val supportsCashDrawer: Boolean = true,
    @ColumnInfo("supports_native_qr") val supportsNativeQr: Boolean = false,
    @ColumnInfo("supports_native_barcode") val supportsNativeBarcode: Boolean = false,
    @ColumnInfo("codepage") val codepage: String = "CP437",
    @ColumnInfo("supports_status") val supportsStatus: Boolean = false,
    @ColumnInfo("active") val active: Boolean = true,
)

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

// --- Routing (document type -> default printer; device-local) ------------------------------------

@Entity(tableName = "print_routing")
data class PrintRoutingEntity(
    @PrimaryKey @ColumnInfo("document_type") val documentType: String,
    @ColumnInfo("printer_id") val printerId: String,
)

// --- Print job (spool; device-local) -------------------------------------------------------------

@Entity(tableName = "print_jobs")
data class PrintJobEntity(
    @PrimaryKey @ColumnInfo("id") val id: String,
    @ColumnInfo("idempotency_key") val idempotencyKey: String,
    @ColumnInfo("document_type") val documentType: String,
    @ColumnInfo("document_id") val documentId: String,
    @ColumnInfo("template_id") val templateId: String,
    @ColumnInfo("printer_id") val printerId: String,
    @ColumnInfo("copies") val copies: Int,
    @ColumnInfo("state") val state: String,
    @ColumnInfo("attempts") val attempts: Int,
    @ColumnInfo("created_at_millis") val createdAtMillis: Long,
    @ColumnInfo("updated_at_millis") val updatedAtMillis: Long,
    @ColumnInfo("last_error") val lastError: String? = null,
)

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

// --- Template (workspace-synced) -----------------------------------------------------------------

@Entity(tableName = "print_templates")
data class PrintTemplateEntity(
    @PrimaryKey @ColumnInfo("id") val id: String,
    @ColumnInfo("document_type") val documentType: String,
    @ColumnInfo("printer_class") val printerClass: String,
    @ColumnInfo("name") val name: String,
    @ColumnInfo("template_json") val templateJson: String,
    @ColumnInfo("version") val version: Long = 1,
    @ColumnInfo("synced") val synced: Boolean = false,
    @ColumnInfo("active") val active: Boolean = true,
    @ColumnInfo("updated_at") val updatedAt: String? = null,
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

private inline fun <reified T : Enum<T>> enumOrDefault(name: String, default: T): T =
    enumValues<T>().firstOrNull { it.name == name } ?: default
