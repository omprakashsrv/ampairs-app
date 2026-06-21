package com.ampairs.printing.core.model

import kotlinx.serialization.Serializable

/**
 * A configured physical printer on a device. Device-local + per-workspace (FR-008) — never synced.
 * [capabilities] drives renderer decisions (chars-per-line, native vs raster codes, status protocol).
 */
@Serializable
data class PrinterProfile(
    val id: String,
    val name: String,
    val printerClass: PrinterClass,
    val connectionType: ConnectionType,
    val paper: PaperSpec,
    /** Transport address: IP:port for NETWORK, MAC for BLUETOOTH, device id for USB, null for OS_PRINT/SHARE. */
    val address: String? = null,
    val capabilities: PrinterCapabilities = PrinterCapabilities(),
)

/** Vendor/model capability flags. Defaults target a common 80mm ESC/POS thermal printer. */
@Serializable
data class PrinterCapabilities(
    val charsPerLine: Int = 48,
    val supportsCut: Boolean = true,
    val supportsCashDrawer: Boolean = true,
    val supportsNativeQr: Boolean = false,
    val supportsNativeBarcode: Boolean = false,
    /** Code page for native text; lines with glyphs outside it are raster-rendered (FR-028). */
    val codepage: String = "CP437",
    /** Whether the transport can read back DLE EOT / ASB status (FR-017). */
    val supportsStatus: Boolean = false,
)
