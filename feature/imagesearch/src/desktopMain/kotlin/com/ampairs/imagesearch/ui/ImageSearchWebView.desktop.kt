package com.ampairs.imagesearch.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import com.ampairs.imagesearch.scrape.ImageScraperJs
import javafx.application.Platform
import javafx.concurrent.Worker
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.web.WebView
import netscape.javascript.JSObject
import java.util.concurrent.atomic.AtomicReference

/** Wires `window.__ampairsPost` to the JSObject member "ampairsBridge". */
private const val DESKTOP_SHIM =
    "window.__ampairsPost = function(j){ ampairsBridge.onResults(j); };"

/**
 * Public named bridge so JavaFX (WebKit) can reflectively invoke [onResults] from JS.
 * (An anonymous object's methods aren't reliably accessible to the JavaFX script engine.)
 */
class DesktopScrapeBridge(private val sink: (String) -> Unit) {
    @Suppress("unused") // called from injected JavaScript via JSObject.setMember
    fun onResults(json: String) {
        sink(json)
    }
}

/**
 * Desktop headless scraper — a JavaFX [WebView] in a [JFXPanel]. JavaFX WebView is older WebKit, so
 * Google may serve a degraded/consent page here; validate on-device (documented risk). The engine
 * must be attached to a shown Scene to run, so it is embedded (drawn behind the opaque grid).
 */
@Composable
actual fun ImageSearchWebView(
    url: String,
    onResults: (String) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier,
) {
    val currentOnResults = rememberUpdatedState(onResults)
    val currentOnError = rememberUpdatedState(onError)
    val webViewRef = remember { AtomicReference<WebView?>(null) }
    val lastUrl = remember { AtomicReference<String?>(null) }

    SwingPanel(
        modifier = modifier,
        factory = {
            val panel = JFXPanel()
            Platform.setImplicitExit(false)
            Platform.runLater {
                val webView = WebView()
                webView.isContextMenuEnabled = false
                val bridge = DesktopScrapeBridge { currentOnResults.value(it) }
                webView.engine.loadWorker.stateProperty().addListener { _, _, newState ->
                    when (newState) {
                        Worker.State.SUCCEEDED -> runCatching {
                            val window = webView.engine.executeScript("window") as JSObject
                            window.setMember("ampairsBridge", bridge)
                            webView.engine.executeScript(ImageScraperJs.bootstrap(DESKTOP_SHIM))
                        }
                        Worker.State.FAILED -> currentOnError.value("Failed to load results")
                        else -> Unit
                    }
                }
                webViewRef.set(webView)
                panel.scene = Scene(webView)
                if (url.isNotBlank()) {
                    lastUrl.set(url)
                    webView.engine.load(url)
                }
            }
            panel
        },
        update = {
            if (url.isNotBlank() && lastUrl.get() != url) {
                lastUrl.set(url)
                webViewRef.get()?.let { webView ->
                    Platform.runLater { webView.engine.load(url) }
                }
            }
        },
    )
}
