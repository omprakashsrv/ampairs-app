package com.ampairs.printing.transport.mock

import com.ampairs.printing.core.model.ConnectionType
import com.ampairs.printing.core.model.PrinterProfile
import com.ampairs.printing.core.render.RenderedOutput
import com.ampairs.printing.core.transport.DiscoveredPrinter
import com.ampairs.printing.core.transport.PrinterStatus
import com.ampairs.printing.core.transport.PrinterTransport

/**
 * In-memory transport that captures sent output instead of touching hardware. Backs the on-device
 * print preview and the renderer golden tests (T011) — the whole pipeline is testable without a
 * printer.
 */
class MockTransport(
    private val status: PrinterStatus = PrinterStatus.Ready,
) : PrinterTransport {

    override val connectionType: ConnectionType = ConnectionType.NETWORK

    val sent: MutableList<RenderedOutput> = mutableListOf()
    var opened: Boolean = false
        private set

    override suspend fun discover(): List<DiscoveredPrinter> = emptyList()

    override suspend fun open(profile: PrinterProfile): Result<Unit> = runCatching { opened = true }

    override suspend fun send(output: RenderedOutput): Result<Unit> = runCatching {
        check(opened) { "MockTransport.send called before open()" }
        sent += output
    }

    override suspend fun queryStatus(profile: PrinterProfile): PrinterStatus = status

    override suspend fun close() {
        opened = false
    }

    /** Convenience for byte-output assertions in tests. */
    fun lastBytes(): ByteArray? = (sent.lastOrNull() as? RenderedOutput.Bytes)?.data

    fun lastText(): String = lastBytes()?.decodeToString() ?: ""
}
