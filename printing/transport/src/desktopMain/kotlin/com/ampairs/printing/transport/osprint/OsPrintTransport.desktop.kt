package com.ampairs.printing.transport.osprint

import com.ampairs.printing.core.model.ConnectionType
import com.ampairs.printing.core.model.PrinterProfile
import com.ampairs.printing.core.render.RenderedOutput
import com.ampairs.printing.core.transport.DiscoveredPrinter
import com.ampairs.printing.core.transport.PrinterStatus
import com.ampairs.printing.core.transport.PrinterTransport
import co.touchlab.kermit.Logger
import javafx.application.Platform
import javafx.concurrent.Worker
import javafx.print.Printer
import javafx.print.PrinterJob as FxPrinterJob
import javafx.scene.web.WebView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.print.PrinterJob
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
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

    /**
     * Inkjet/laser path — render the HTML with a JavaFX [WebView] (WebKit) so the printout matches
     * the high-fidelity preview, then print to [svc] with no dialog. Falls back to the legacy
     * [JEditorPane] path if JavaFX can't render/print (so printing never regresses).
     */
    private fun printHtmlPage(svc: PrintService, html: String) {
        val printed = runCatching { printViaJavaFx(svc, html) }.getOrElse {
            log.w(it) { "JavaFX print failed; falling back to JEditorPane: ${it.message}" }
            false
        }
        if (!printed) printHtmlPageSwing(svc, html)
    }

    /** Render + print through a JavaFX WebView. Returns true if a page was submitted to the printer. */
    private fun printViaJavaFx(svc: PrintService, html: String): Boolean {
        ensureFxStarted()
        val latch = CountDownLatch(1)
        val error = AtomicReference<Throwable?>(null)
        val printed = AtomicBoolean(false)
        Platform.runLater {
            try {
                val webView = WebView()
                val engine = webView.engine
                engine.loadWorker.stateProperty().addListener { _, _, state ->
                    when (state) {
                        Worker.State.SUCCEEDED -> {
                            try {
                                val printer = Printer.getAllPrinters()
                                    .firstOrNull { it.name.equals(svc.name, ignoreCase = true) }
                                val job = if (printer != null) {
                                    FxPrinterJob.createPrinterJob(printer)
                                } else {
                                    FxPrinterJob.createPrinterJob()
                                }
                                if (job != null) {
                                    engine.print(job)
                                    job.endJob()
                                    printed.set(true)
                                    log.i { "printViaJavaFx: printed to '${svc.name}'" }
                                } else {
                                    log.w { "printViaJavaFx: no JavaFX PrinterJob for '${svc.name}'" }
                                }
                            } catch (t: Throwable) {
                                error.set(t)
                            } finally {
                                latch.countDown()
                            }
                        }
                        Worker.State.FAILED, Worker.State.CANCELLED -> {
                            error.set(engine.loadWorker.exception ?: RuntimeException("WebView load $state"))
                            latch.countDown()
                        }
                        else -> Unit // RUNNING / SCHEDULED / READY — keep waiting
                    }
                }
                engine.loadContent(html, "text/html")
            } catch (t: Throwable) {
                error.set(t)
                latch.countDown()
            }
        }
        if (!latch.await(30, TimeUnit.SECONDS)) error("JavaFX print timed out")
        error.get()?.let { throw it }
        return printed.get()
    }

    /** Legacy fallback — Swing JEditorPane (HTML 3.2). Lower fidelity but always available. */
    private fun printHtmlPageSwing(svc: PrintService, html: String) {
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
        log.i { "printHtmlPageSwing: submitting job to '${svc.name}' (html=${html.length} chars)" }
        job.print()
        log.i { "printHtmlPageSwing: job.print() returned for '${svc.name}'" }
    }

    /** Boot the JavaFX runtime once per process; safe if the preview already started it. */
    private fun ensureFxStarted() {
        if (fxStarted.compareAndSet(false, true)) {
            try {
                Platform.startup { Platform.setImplicitExit(false) }
            } catch (e: IllegalStateException) {
                // Toolkit already initialized (e.g. by the preview's JFXPanel) — fine.
                Platform.setImplicitExit(false)
            }
        }
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

    private companion object {
        /** JavaFX toolkit is a process singleton — start it at most once. */
        private val fxStarted = AtomicBoolean(false)
    }
}
