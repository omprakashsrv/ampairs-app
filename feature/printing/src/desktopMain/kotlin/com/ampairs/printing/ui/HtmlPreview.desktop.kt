package com.ampairs.printing.ui

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.Image
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import javafx.application.Platform
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.concurrent.Worker
import javafx.embed.swing.SwingFXUtils
import javafx.scene.Group
import javafx.scene.Scene
import javafx.scene.SnapshotParameters
import javafx.scene.web.WebView
import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/** A4 portrait width in CSS px at 96 dpi (210 mm). The page renders to this fixed width. */
private const val A4_WIDTH_PX = 794.0

private val fxStarted = AtomicBoolean(false)

/**
 * Desktop preview. Renders the page off-screen with a JavaFX [WebView] (WebKit — full CSS/fonts/
 * tables) at a fixed A4 width, snapshots it to an image, and shows that image in a Compose scroll
 * area with real horizontal + vertical scrollbars. This avoids the fragile JFXPanel-in-SwingPanel
 * sizing entirely, so layout is deterministic. The desktop printout uses the same WebKit engine.
 */
@Composable
actual fun HtmlPreview(html: String, modifier: Modifier) {
    val webViewRef = remember { AtomicReference<WebView?>(null) }
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(html) {
        bitmap = renderToBitmap(webViewRef, html)
    }

    val vScroll = rememberScrollState()
    val hScroll = rememberScrollState()
    Box(modifier.background(Color(0xFFEEEEEE))) {
        bitmap?.let { bmp ->
            Box(
                Modifier.fillMaxSize().verticalScroll(vScroll).horizontalScroll(hScroll),
            ) {
                Image(bitmap = bmp, contentDescription = null)
            }
            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(vScroll),
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            )
            HorizontalScrollbar(
                adapter = rememberScrollbarAdapter(hScroll),
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            )
        }
    }
}

/** Render [html] off-screen at A4 width and snapshot the full page to a Compose [ImageBitmap]. */
private suspend fun renderToBitmap(ref: AtomicReference<WebView?>, html: String): ImageBitmap? {
    ensureFxStarted()
    val result = CompletableDeferred<ImageBitmap?>()
    Platform.runLater {
        try {
            val webView = ref.get() ?: WebView().also { wv ->
                wv.engine.isJavaScriptEnabled = true // only to measure content height
                wv.minWidth = A4_WIDTH_PX
                wv.prefWidth = A4_WIDTH_PX
                wv.maxWidth = A4_WIDTH_PX
                Scene(Group(wv)) // attach to a scene so layout + snapshot work off-screen
                ref.set(wv)
            }
            val engine = webView.engine
            val listener = object : ChangeListener<Worker.State> {
                override fun changed(
                    obs: ObservableValue<out Worker.State>,
                    old: Worker.State?,
                    state: Worker.State,
                ) {
                    when (state) {
                        Worker.State.SUCCEEDED -> {
                            engine.loadWorker.stateProperty().removeListener(this)
                            val h = (engine.executeScript("document.documentElement.scrollHeight") as? Number)
                                ?.toDouble()?.coerceAtLeast(1.0) ?: 1123.0
                            webView.minHeight = h
                            webView.prefHeight = h
                            webView.maxHeight = h
                            // Snapshot on the next pulse so the resize/layout is applied first.
                            Platform.runLater {
                                result.complete(
                                    runCatching {
                                        val img = webView.snapshot(SnapshotParameters(), null)
                                        SwingFXUtils.fromFXImage(img, null)?.toComposeImageBitmap()
                                    }.getOrNull(),
                                )
                            }
                        }
                        Worker.State.FAILED, Worker.State.CANCELLED -> {
                            engine.loadWorker.stateProperty().removeListener(this)
                            result.complete(null)
                        }
                        else -> Unit
                    }
                }
            }
            engine.loadWorker.stateProperty().addListener(listener)
            engine.loadContent(html, "text/html")
        } catch (t: Throwable) {
            result.complete(null)
        }
    }
    return result.await()
}

/** Boot the JavaFX runtime once per process. */
private fun ensureFxStarted() {
    if (fxStarted.compareAndSet(false, true)) {
        try {
            Platform.startup {}
        } catch (_: IllegalStateException) {
            // Already initialized — fine.
        }
    }
    Platform.setImplicitExit(false)
}
