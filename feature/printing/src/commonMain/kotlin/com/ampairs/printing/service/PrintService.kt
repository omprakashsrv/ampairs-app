package com.ampairs.printing.service

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.printing.core.model.ConnectionType
import com.ampairs.printing.core.model.PlainValueFormatter
import com.ampairs.printing.core.model.PrintDocument
import com.ampairs.printing.core.model.PrinterClass
import com.ampairs.printing.core.model.PrinterProfile
import com.ampairs.printing.core.model.ValueFormatter
import com.ampairs.printing.core.render.Renderer
import com.ampairs.printing.core.spool.SendOutcome
import com.ampairs.printing.core.transport.PrinterTransportFactory
import com.ampairs.printing.render.escpos.EscPosRenderer
import com.ampairs.printing.render.html.HtmlRenderer
import com.ampairs.printing.render.label.LabelRenderer
import co.touchlab.kermit.Logger
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Orchestrates one print: pick the renderer for the printer class, render the IR, and send over the
 * matching transport — serialized per printer with a single-writer [Mutex] (§19). Returns a
 * [SendOutcome] the spool turns into a job-state transition. The transport is resolved per platform
 * via the injected [PrinterTransportFactory] (TCP everywhere; USB/Bluetooth on Android; OS-print on
 * desktop), so this orchestrator stays platform-agnostic.
 */
@Inject
@SingleIn(WorkspaceScope::class)
class PrintService(
    private val transportFactory: PrinterTransportFactory,
) {

    private val mapLock = Mutex()
    private val printerMutexes = mutableMapOf<String, Mutex>()
    private val log = Logger.withTag("PrintService")

    /**
     * Print a class-appropriate test page on [profile] to verify the full render→transport path
     * (thermal/page/label) on the current platform. Uses no business data.
     */
    suspend fun testPrint(profile: PrinterProfile): SendOutcome =
        print(TestPrintDocuments.forPrinter(profile.printerClass, profile.name), profile)

    suspend fun print(
        document: PrintDocument,
        profile: PrinterProfile,
        formatter: ValueFormatter = PlainValueFormatter,
    ): SendOutcome {
        val transport = transportFactory.transportFor(profile) ?: run {
            log.w { "no transport for conn=${profile.connectionType} on this platform" }
            return SendOutcome.PERMANENT_FAILURE
        }
        val renderer: Renderer = rendererFor(profile.printerClass)
        val output = renderer.render(document, profile, formatter)
        val outDesc = when (output) {
            is com.ampairs.printing.core.render.RenderedOutput.Bytes -> "Bytes(${output.data.size})"
            is com.ampairs.printing.core.render.RenderedOutput.Markup -> "Markup(${output.html.length})"
            is com.ampairs.printing.core.render.RenderedOutput.Pdf -> "Pdf(${output.data.size})"
        }
        log.i { "print: printer=${profile.name} conn=${profile.connectionType} class=${profile.printerClass} output=$outDesc" }

        return mutexFor(profile.id).withLock {
            log.i { "print: opening transport ${transport::class.simpleName}" }
            val opened = transport.open(profile)
            if (opened.isFailure) {
                log.e(opened.exceptionOrNull()) { "print: open failed on ${profile.name}: ${opened.exceptionOrNull()?.message}" }
                transport.close()
                // Surface the real reason (don't swallow it as a generic transient failure) so the
                // job's lastError and the UI message say WHY it didn't print.
                throw IllegalStateException(
                    "Could not connect to ${profile.name}: ${opened.exceptionOrNull()?.message ?: "open failed"}",
                    opened.exceptionOrNull(),
                )
            }
            log.i { "print: opened OK; sending to ${profile.name}" }
            val sent = transport.send(output)
            transport.close()
            if (sent.isFailure) {
                log.e(sent.exceptionOrNull()) { "print: send failed on ${profile.name}: ${sent.exceptionOrNull()?.message}" }
                throw IllegalStateException(
                    "Printing failed on ${profile.name}: ${sent.exceptionOrNull()?.message ?: "send failed"}",
                    sent.exceptionOrNull(),
                )
            }
            // OS print service accepted the job → confirmed. Raw thermal is fire-and-forget:
            // "sent" but not confirmed (no back-channel), so it stays for the user to verify (§19).
            val result = if (profile.connectionType == ConnectionType.OS_PRINT) SendOutcome.CONFIRMED else SendOutcome.SENT_UNCONFIRMED
            log.i { "print: send OK on ${profile.name} → $result" }
            result
        }
    }

    private fun rendererFor(printerClass: PrinterClass): Renderer = when (printerClass) {
        PrinterClass.THERMAL -> EscPosRenderer()
        PrinterClass.PAGE -> HtmlRenderer()
        PrinterClass.LABEL -> LabelRenderer()
    }

    private suspend fun mutexFor(printerId: String): Mutex =
        mapLock.withLock { printerMutexes.getOrPut(printerId) { Mutex() } }
}
