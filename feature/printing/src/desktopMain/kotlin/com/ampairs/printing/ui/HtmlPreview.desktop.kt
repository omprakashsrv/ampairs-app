package com.ampairs.printing.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import javax.swing.JEditorPane
import javax.swing.JScrollPane

/**
 * Desktop preview via [JEditorPane] (`text/html`) — the **same component `JavaPrintServiceTransport`
 * prints with**, so the desktop preview matches the desktop printout exactly (HTML 3.2 / CSS1).
 */
@Composable
actual fun HtmlPreview(html: String, modifier: Modifier) {
    SwingPanel(
        modifier = modifier,
        factory = {
            val pane = JEditorPane().apply {
                contentType = "text/html"
                isEditable = false
                text = html
            }
            JScrollPane(pane)
        },
        update = { scroll ->
            (scroll.viewport.view as? JEditorPane)?.text = html
        },
    )
}
