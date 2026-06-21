package com.ampairs.printing.core.transport

import com.ampairs.printing.core.model.ConnectionType
import com.ampairs.printing.core.model.PrinterClass
import com.ampairs.printing.core.model.PrinterProfile
import com.ampairs.printing.core.render.RenderedOutput

/**
 * Sends rendered output to a printer over one channel. Raw-byte transports (NETWORK/BLUETOOTH/USB)
 * carry [RenderedOutput.Bytes]; OS_PRINT/SHARE carry [RenderedOutput.Markup]/[RenderedOutput.Pdf].
 * Implementations live in `printing/transport` (expect/actual); core only owns the contract.
 */
interface PrinterTransport {
    val connectionType: ConnectionType

    /** Channel discovery (mDNS/Bonjour, BT scan, USB enumerate). Cached by the caller. */
    suspend fun discover(): List<DiscoveredPrinter>

    /** Open (and reuse/keep-alive) a connection to the target. */
    suspend fun open(profile: PrinterProfile): Result<Unit>

    /** Send one rendered job. Success means "sent" — confirmation is a separate concern (FR-016). */
    suspend fun send(output: RenderedOutput): Result<Unit>

    /** Read printer health if the channel supports a back-channel; else [PrinterStatus.Unknown]. */
    suspend fun queryStatus(profile: PrinterProfile): PrinterStatus

    suspend fun close()
}

data class DiscoveredPrinter(
    val id: String,
    val name: String,
    val connectionType: ConnectionType,
    /** IP:port / MAC / USB id. */
    val address: String?,
    val suggestedClass: PrinterClass? = null,
)

/** Printer health derived from ESC/POS DLE EOT / ASB where available (FR-017). */
sealed interface PrinterStatus {
    data object Ready : PrinterStatus
    data object Offline : PrinterStatus
    data object PaperOut : PrinterStatus
    data object PaperLow : PrinterStatus
    data object CoverOpen : PrinterStatus
    data object CutterError : PrinterStatus
    data class Unknown(val reason: String? = null) : PrinterStatus
}
