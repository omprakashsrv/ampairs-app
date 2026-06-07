package com.ampairs.invoice.print

import androidx.compose.runtime.Composable
import java.awt.Desktop
import java.io.File

@Composable
actual fun rememberInvoicePrinter(): (html: String) -> Unit = { html ->
    // Desktop has no unified print API for HTML — write a temp file and open it in the default
    // browser, where the user can print (Ctrl/Cmd+P) to paper or PDF.
    runCatching {
        val file = File.createTempFile("tax_invoice_", ".html")
        file.writeText(html)
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(file.toURI())
        }
    }
}
