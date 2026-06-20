package com.ampairs.invoice.print

import androidx.compose.runtime.Composable
import java.awt.Desktop
import java.awt.print.PrinterJob
import java.io.File
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JEditorPane
import javax.swing.SwingUtilities

@Composable
actual fun rememberInvoicePrinter(): (html: String) -> Unit = { html ->
    // Print directly through the OS print service: lay the invoice HTML out in a JEditorPane and show
    // the system print dialog (printer / "Save as PDF"). Runs off the UI thread; Swing widgets are
    // built on the EDT. Falls back to opening the HTML in the browser only if direct printing fails.
    Thread {
        runCatching {
            val paneRef = AtomicReference<JEditorPane>()
            SwingUtilities.invokeAndWait {
                paneRef.set(
                    JEditorPane().apply {
                        contentType = "text/html"
                        text = html
                    },
                )
            }
            val job = PrinterJob.getPrinterJob()
            job.setPrintable(paneRef.get().getPrintable(null, null))
            if (job.printDialog()) job.print()
        }.onFailure {
            runCatching {
                val file = File.createTempFile("tax_invoice_", ".html")
                file.writeText(html)
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(file.toURI())
                }
            }
        }
    }.apply { isDaemon = true }.start()
}
