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
        val transport = transportFactory.transportFor(profile) ?: return SendOutcome.PERMANENT_FAILURE
        val renderer: Renderer = rendererFor(profile.printerClass)
        val output = renderer.render(document, profile, formatter)

        return mutexFor(profile.id).withLock {
            val opened = transport.open(profile)
            if (opened.isFailure) {
                transport.close()
                // Surface the real reason (don't swallow it as a generic transient failure) so the
                // job's lastError and the UI message say WHY it didn't print.
                throw IllegalStateException(
                    "Could not connect to ${profile.name}: ${opened.exceptionOrNull()?.message ?: "open failed"}",
                    opened.exceptionOrNull(),
                )
            }
            val sent = transport.send(output)
            transport.close()
            if (sent.isFailure) {
                throw IllegalStateException(
                    "Printing failed on ${profile.name}: ${sent.exceptionOrNull()?.message ?: "send failed"}",
                    sent.exceptionOrNull(),
                )
            }
            // OS print service accepted the job → confirmed. Raw thermal is fire-and-forget:
            // "sent" but not confirmed (no back-channel), so it stays for the user to verify (§19).
            if (profile.connectionType == ConnectionType.OS_PRINT) SendOutcome.CONFIRMED else SendOutcome.SENT_UNCONFIRMED
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
