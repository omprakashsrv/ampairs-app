package com.ampairs.printing.data.db

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

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
