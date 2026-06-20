package com.ampairs.printing.service

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.printing.core.model.PlainValueFormatter
import com.ampairs.printing.core.model.PrintDocument
import com.ampairs.printing.core.model.PrinterClass
import com.ampairs.printing.core.model.PrinterProfile
import com.ampairs.printing.core.model.ValueFormatter
import com.ampairs.printing.core.render.Renderer
import com.ampairs.printing.core.spool.SendOutcome
import com.ampairs.printing.core.transport.PrinterTransport
import com.ampairs.printing.render.escpos.EscPosRenderer
import com.ampairs.printing.render.html.HtmlRenderer
import com.ampairs.printing.render.label.LabelRenderer
import com.ampairs.printing.transport.network.NetworkTransport
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Orchestrates one print: pick the renderer for the printer class, render the IR, and send over the
 * matching transport — serialized per printer with a single-writer [Mutex] (§19). Returns a
 * [SendOutcome] the spool turns into a job-state transition. Currently wires the network transport;
 * Bluetooth/USB/OS-print transports are added in the connectivity phase.
 */
@Inject
@SingleIn(WorkspaceScope::class)
class PrintService {

    private val mapLock = Mutex()
    private val printerMutexes = mutableMapOf<String, Mutex>()

    suspend fun print(
        document: PrintDocument,
        profile: PrinterProfile,
        formatter: ValueFormatter = PlainValueFormatter,
    ): SendOutcome {
        val transport = transportFor(profile) ?: return SendOutcome.PERMANENT_FAILURE
        val renderer: Renderer = rendererFor(profile.printerClass)
        val output = renderer.render(document, profile, formatter)

        return mutexFor(profile.id).withLock {
            val opened = transport.open(profile)
            if (opened.isFailure) {
                transport.close()
                return@withLock SendOutcome.TRANSIENT_FAILURE
            }
            val sent = transport.send(output)
            transport.close()
            // Raw thermal is fire-and-forget: success means "sent", not "confirmed" (§19).
            if (sent.isSuccess) SendOutcome.SENT_UNCONFIRMED else SendOutcome.TRANSIENT_FAILURE
        }
    }

    private fun rendererFor(printerClass: PrinterClass): Renderer = when (printerClass) {
        PrinterClass.THERMAL -> EscPosRenderer()
        PrinterClass.PAGE -> HtmlRenderer()
        PrinterClass.LABEL -> LabelRenderer()
    }

    private fun transportFor(profile: PrinterProfile): PrinterTransport? = when (profile.connectionType) {
        com.ampairs.printing.core.model.ConnectionType.NETWORK -> NetworkTransport()
        else -> null // Bluetooth/USB/OS-print/Share land in the connectivity phase
    }

    private suspend fun mutexFor(printerId: String): Mutex =
        mapLock.withLock { printerMutexes.getOrPut(printerId) { Mutex() } }
}
