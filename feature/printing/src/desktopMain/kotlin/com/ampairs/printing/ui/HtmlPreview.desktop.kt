package com.ampairs.printing.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.web.WebView
import java.util.concurrent.atomic.AtomicReference

/**
 * Desktop preview via a JavaFX [WebView] (WebKit) embedded in a [JFXPanel] — a real browser engine,
 * so the page renders with full CSS/fonts/tables (unlike the legacy `JEditorPane`). The WebView is the
 * Scene root, so it fills the pane; with `@page` scoped to print only and no viewport meta, WebKit
 * lays the page out at the WebView's (pane) width. Matches the desktop printout, which also prints
 * through a JavaFX WebView.
 *
 * Scrolling: a [JFXPanel] embedded through Compose's `SwingPanel` does not reliably forward AWT
 * mouse-wheel events to the JavaFX scene, so the WebView's native scroll appears dead and a tall page
 * only shows its top. We forward wheel events explicitly into the page via `window.scrollBy` (Shift =
 * horizontal), which makes the full document scrollable regardless of interop event delivery.
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
            panel.addMouseWheelListener { event ->
                val webView = webViewRef.get() ?: return@addMouseWheelListener
                // ~3 lines per notch; preciseWheelRotation gives smooth trackpad deltas too.
                val distance = event.preciseWheelRotation * 48.0
                val horizontal = event.isShiftDown
                Platform.runLater {
                    val script = if (horizontal) "window.scrollBy($distance, 0)" else "window.scrollBy(0, $distance)"
                    runCatching { webView.engine.executeScript(script) }
                }
            }
            Platform.runLater {
                val webView = WebView()
                webView.isContextMenuEnabled = false
                webViewRef.set(webView)
                panel.scene = Scene(webView)
                webView.engine.loadContent(html, "text/html")
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
