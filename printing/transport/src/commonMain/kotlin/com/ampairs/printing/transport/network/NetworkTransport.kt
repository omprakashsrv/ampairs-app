package com.ampairs.printing.transport.network

import com.ampairs.printing.core.model.ConnectionType
import com.ampairs.printing.core.model.PrinterProfile
import com.ampairs.printing.core.render.RenderedOutput
import com.ampairs.printing.core.transport.DiscoveredPrinter
import com.ampairs.printing.core.transport.PrinterStatus
import com.ampairs.printing.core.transport.PrinterTransport
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withTimeout

/**
 * Raw TCP transport to a network/WiFi/Ethernet printer on port 9100. Lives entirely in commonMain
 * via ktor-network, so one implementation serves Android, iOS, and Desktop (T018). Carries
 * [RenderedOutput.Bytes] (ESC/POS / TSPL).
 */
class NetworkTransport(
    private val connectTimeoutMs: Long = 8_000,
    private val sendTimeoutMs: Long = 30_000,
) : PrinterTransport {

    override val connectionType: ConnectionType = ConnectionType.NETWORK

    private var selector: SelectorManager? = null
    private var socket: Socket? = null
    private var channel: ByteWriteChannel? = null

    /** Network discovery (mDNS/Bonjour) is a follow-up; printers are added by IP for now. */
    override suspend fun discover(): List<DiscoveredPrinter> = emptyList()

    override suspend fun open(profile: PrinterProfile): Result<Unit> = runCatching {
        val (host, port) = parseAddress(profile.address)
        val sel = SelectorManager(Dispatchers.Default)
        val sock = withTimeout(connectTimeoutMs) {
            aSocket(sel).tcp().connect(host, port)
        }
        selector = sel
        socket = sock
        channel = sock.openWriteChannel(autoFlush = true)
    }

    override suspend fun send(output: RenderedOutput): Result<Unit> = runCatching {
        val ch = channel ?: error("NetworkTransport.send called before open()")
        val bytes = (output as? RenderedOutput.Bytes)?.data
            ?: error("NetworkTransport requires RenderedOutput.Bytes")
        withTimeout(sendTimeoutMs) {
            ch.writeFully(bytes, 0, bytes.size)
            ch.flush()
        }
    }

    /** Raw :9100 has no standard status back-channel here; status reads are a follow-up (FR-017). */
    override suspend fun queryStatus(profile: PrinterProfile): PrinterStatus =
        PrinterStatus.Unknown("network status not implemented")

    override suspend fun close() {
        runCatching { channel?.flushAndClose() }
        runCatching { socket?.close() }
        runCatching { selector?.close() }
        channel = null
        socket = null
        selector = null
    }

    private fun parseAddress(address: String?): Pair<String, Int> {
        val a = address?.trim().orEmpty()
        require(a.isNotEmpty()) { "NetworkTransport requires an address (host:port)" }
        val idx = a.lastIndexOf(':')
        return if (idx > 0) {
            a.substring(0, idx) to (a.substring(idx + 1).toIntOrNull() ?: DEFAULT_PORT)
        } else {
            a to DEFAULT_PORT
        }
    }

    companion object {
        const val DEFAULT_PORT = 9100
    }
}
