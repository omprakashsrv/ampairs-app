package com.ampairs.printing.transport.osprint

import com.ampairs.printing.core.model.ConnectionType
import com.ampairs.printing.core.model.PrinterProfile
import com.ampairs.printing.core.render.RenderedOutput
import com.ampairs.printing.core.transport.DiscoveredPrinter
import com.ampairs.printing.core.transport.PrinterStatus
import com.ampairs.printing.core.transport.PrinterTransport
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.print.PrinterJob
import java.util.concurrent.atomic.AtomicReference
import javax.print.DocFlavor
import javax.print.PrintService
import javax.print.PrintServiceLookup
import javax.print.SimpleDoc
import javax.print.attribute.HashPrintRequestAttributeSet
import javax.print.attribute.standard.Copies
import javax.swing.JEditorPane
import javax.swing.SwingUtilities

actual fun createOsPrintTransport(): PrinterTransport? = JavaPrintServiceTransport()

/**
 * Desktop OS-print channel backed by the Java Print Service (`javax.print` / `java.awt.print`).
 *
 * This is the **inkjet / laser (PAGE) desktop channel** for Windows, macOS and Linux: a PAGE
 * template renders to [RenderedOutput.Markup] (HTML), which we lay out and print through a
 * [JEditorPane] + [PrinterJob] to the chosen [PrintService] — no print dialog, no extra dependency.
 * It also accepts [RenderedOutput.Pdf] (PDF DocFlavor).
 *
 * It deliberately does **not** print raw ESC/POS [RenderedOutput.Bytes]: pushing control codes
 * through the OS spooler is unreliable (drivers reinterpret/reformat the stream and corrupt the
 * commands). Thermal printers must use the raw network (:9100) / USB transports, which write the
 * bytes to the device untouched.
 *
 * The target printer is selected by its system name in [PrinterProfile.address]; a blank address
 * uses the OS default printer. JVM-only: this file lives in `desktopMain`, never `commonMain`.
 */
internal class JavaPrintServiceTransport : PrinterTransport {

    override val connectionType: ConnectionType = ConnectionType.OS_PRINT

    private var service: PrintService? = null
    private val log = Logger.withTag("JavaPrintService")

    override suspend fun discover(): List<DiscoveredPrinter> = withContext(Dispatchers.IO) {
        PrintServiceLookup.lookupPrintServices(null, null).map { svc ->
            DiscoveredPrinter(
                id = svc.name,
                name = svc.name,
                connectionType = ConnectionType.OS_PRINT,
                address = svc.name,
            )
        }
    }

    override suspend fun open(profile: PrinterProfile): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val target = profile.address?.trim().orEmpty()
            val available = PrintServiceLookup.lookupPrintServices(null, null).map { it.name }
            log.i { "open: target='$target' available=$available" }
            service = if (target.isEmpty()) {
                PrintServiceLookup.lookupDefaultPrintService()
                    ?: error("No default OS print service available")
            } else {
                PrintServiceLookup.lookupPrintServices(null, null)
                    .firstOrNull { it.name.equals(target, ignoreCase = true) }
                    ?: error("OS printer not found: '$target' (available: $available)")
            }
            log.i { "open: bound to service='${service?.name}'" }
        }.onFailure { log.e(it) { "open failed: ${it.message}" } }
    }

    override suspend fun send(output: RenderedOutput): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val svc = service ?: error("JavaPrintServiceTransport.send called before open()")
            log.i { "send: output=${output::class.simpleName} to '${svc.name}'" }
            when (output) {
                is RenderedOutput.Markup -> printHtmlPage(svc, output.html)
                is RenderedOutput.Pdf -> printPdf(svc, output.data)
                // Raw ESC/POS through the OS spooler is unreliable — use the network/USB transport.
                is RenderedOutput.Bytes ->
                    error("Java Print Service is for inkjet/laser pages; print raw ESC/POS via the network/USB transport")
            }
            log.i { "send: completed to '${svc.name}'" }
        }.onFailure { log.e(it) { "send failed: ${it.message}" } }
    }

    /** Inkjet/laser path — render the HTML page and print it to [svc] with no dialog. */
    private fun printHtmlPage(svc: PrintService, html: String) {
        val paneRef = AtomicReference<JEditorPane>()
        // Swing components must be built on the EDT; the blocking job.print() then runs off-EDT.
        SwingUtilities.invokeAndWait {
            paneRef.set(
                JEditorPane().apply {
                    contentType = "text/html"
                    text = html
                },
            )
        }
        val job = PrinterJob.getPrinterJob()
        job.printService = svc
        // JTextComponent.getPrintable wraps content to the printer page width and paginates.
        job.setPrintable(paneRef.get().getPrintable(null, null))
        log.i { "printHtmlPage: submitting job to '${svc.name}' (html=${html.length} chars)" }
        job.print()
        log.i { "printHtmlPage: job.print() returned for '${svc.name}'" }
    }

    /** Print a pre-rendered PDF straight to the queue (inkjet/laser). */
    private fun printPdf(svc: PrintService, bytes: ByteArray) {
        val flavor = DocFlavor.BYTE_ARRAY.PDF
        require(svc.isDocFlavorSupported(flavor)) { "Printer ${svc.name} does not support PDF printing" }
        val attrs = HashPrintRequestAttributeSet().apply { add(Copies(1)) }
        svc.createPrintJob().print(SimpleDoc(bytes, flavor, null), attrs)
    }

    /** The Java Print Service exposes attributes, but reliable thermal status reads are a follow-up. */
    override suspend fun queryStatus(profile: PrinterProfile): PrinterStatus =
        if (service != null) PrinterStatus.Ready else PrinterStatus.Unknown("not opened")

    override suspend fun close() {
        service = null
    }
}
