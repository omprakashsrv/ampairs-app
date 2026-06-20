package com.ampairs.printing.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.input.ScrollEvent
import javafx.scene.input.ZoomEvent
import javafx.scene.web.WebView
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.util.concurrent.atomic.AtomicReference

/**
 * Desktop preview via a JavaFX [WebView] (WebKit) embedded in a [JFXPanel] — a real browser engine,
 * so the page template renders with full CSS/fonts/tables and supports zoom, unlike the legacy
 * `JEditorPane` (HTML 3.2). Matches the desktop printout, which also prints through a JavaFX WebView.
 *
 * Zoom: ⌘/Ctrl + scroll, or trackpad pinch. Vertical/horizontal scrolling is native to the WebView.
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
                webView.engine.isJavaScriptEnabled = false
                installZoom(webView)
                webViewRef.set(webView)
                sizeToPanel(webView, panel)
                webView.engine.loadContent(html, "text/html")
                panel.scene = Scene(webView)
            }
            // Pin the WebView to the panel's size: desktop WebKit lays the page out at the node's
            // width, so without this the page renders wider than the pane and right-hand content
            // (key-value values, the Rate/Amount columns) is pushed off-screen.
            panel.addComponentListener(object : ComponentAdapter() {
                override fun componentResized(e: ComponentEvent) {
                    val view = webViewRef.get() ?: return
                    Platform.runLater { sizeToPanel(view, panel) }
                }
            })
            panel
        },
        update = {
            webViewRef.get()?.let { webView ->
                Platform.runLater { webView.engine.loadContent(html, "text/html") }
            }
        },
    )
}

/** Constrain the WebView to exactly the host panel's size so the web page lays out at the pane width. */
private fun sizeToPanel(webView: WebView, panel: JFXPanel) {
    val w = panel.width.toDouble().coerceAtLeast(1.0)
    val h = panel.height.toDouble().coerceAtLeast(1.0)
    webView.minWidth = w
    webView.prefWidth = w
    webView.maxWidth = w
    webView.minHeight = h
    webView.prefHeight = h
    webView.maxHeight = h
}

/** Wire ⌘/Ctrl+scroll and trackpad-pinch to the WebView's zoom factor. */
private fun installZoom(webView: WebView) {
    webView.addEventFilter(ScrollEvent.SCROLL) { e ->
        if (e.isShortcutDown || e.isControlDown) {
            val factor = if (e.deltaY >= 0) 1.1 else 1.0 / 1.1
            webView.zoom = (webView.zoom * factor).coerceIn(0.3, 5.0)
            e.consume()
        }
    }
    webView.addEventFilter(ZoomEvent.ZOOM) { e ->
        webView.zoom = (webView.zoom * e.zoomFactor).coerceIn(0.3, 5.0)
        e.consume()
    }
}
