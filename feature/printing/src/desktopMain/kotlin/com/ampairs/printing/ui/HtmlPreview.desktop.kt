package com.ampairs.printing.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import javax.swing.JEditorPane
import javax.swing.JScrollPane
import javax.swing.ScrollPaneConstants

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
            // Scrollbars appear as needed so a full A4 page is reachable (JEditorPane has no zoom).
            JScrollPane(
                pane,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED,
            )
        },
        update = { scroll ->
            (scroll.viewport.view as? JEditorPane)?.apply {
                text = html
                caretPosition = 0 // keep the view scrolled to the top after re-render
            }
        },
    )
}

