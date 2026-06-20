package com.ampairs.printing.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import javafx.application.Platform
import javafx.concurrent.Worker
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.control.ScrollPane
import javafx.scene.web.WebView
import java.util.concurrent.atomic.AtomicReference

/**
 * Desktop preview via a JavaFX [WebView] (WebKit) inside a [ScrollPane] embedded in a [JFXPanel].
 * WebKit renders the page with full CSS/fonts/tables (unlike the legacy `JEditorPane`); the WebView is
 * sized to its rendered content so the surrounding [ScrollPane] provides real horizontal + vertical
 * scrollbars over the full-size page. Matches the desktop printout (also printed via a JavaFX WebView).
 */
@Composable
actual fun HtmlPreview(html: String, modifier: Modifier) {
    val webViewRef = remember { AtomicReference<WebView?>(null) }
    SwingPanel(
        modifier = modifier,
        factory = {
            // JFXPanel() boots the JavaFX runtime; keep it alive when panels come and go.
            val panel = JFXPanel()
            Platform.setImplicitExit(false)
            Platform.runLater {
                val webView = WebView()
                // JS enabled only so we can measure the laid-out content size for the ScrollPane.
                webView.engine.isJavaScriptEnabled = true
                webView.engine.loadWorker.stateProperty().addListener { _, _, state ->
                    if (state == Worker.State.SUCCEEDED) sizeToContent(webView)
                }
                webViewRef.set(webView)
                val scroll = ScrollPane(webView).apply {
                    hbarPolicy = ScrollPane.ScrollBarPolicy.AS_NEEDED
                    vbarPolicy = ScrollPane.ScrollBarPolicy.AS_NEEDED
                    isPannable = true // drag-to-pan in addition to the scrollbars
                    isFitToWidth = false
                    isFitToHeight = false
                }
                webView.engine.loadContent(html, "text/html")
                panel.scene = Scene(scroll)
            }
            panel
        },
        update = {
            webViewRef.get()?.let { webView ->
                Platform.runLater { webView.engine.loadContent(html, "text/html") }
            }
        },
    )
}

/**
 * Size the WebView to its full rendered content (not the viewport) so the WebView itself never scrolls
 * internally — the surrounding [ScrollPane] does, giving both horizontal and vertical scrollbars.
 */
private fun sizeToContent(webView: WebView) {
    try {
        val width = (webView.engine.executeScript("document.documentElement.scrollWidth") as? Number)?.toDouble()
        val height = (webView.engine.executeScript("document.documentElement.scrollHeight") as? Number)?.toDouble()
        if (width != null && width > 1.0) {
            webView.minWidth = width
            webView.prefWidth = width
            webView.maxWidth = width
        }
        if (height != null && height > 1.0) {
            webView.minHeight = height
            webView.prefHeight = height
            webView.maxHeight = height
        }
    } catch (_: Throwable) {
        // Page not ready / measurement failed — leave the WebView's default sizing.
    }
}
