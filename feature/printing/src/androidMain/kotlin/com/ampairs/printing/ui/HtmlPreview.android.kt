package com.ampairs.printing.ui

import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/** Android preview via [WebView] — the same engine `PrintManager` renders for OS print. */
@Composable
actual fun HtmlPreview(html: String, modifier: Modifier) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                with(settings) {
                    // Lay the page out at the WebView's own width (no <meta viewport> is emitted) so
                    // it fits the pane, then allow pinch-zoom so overflowing detail is reachable.
                    useWideViewPort = false
                    loadWithOverviewMode = false
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false
                }
                // Native scrolling for content taller/wider than the pane.
                isVerticalScrollBarEnabled = true
                isHorizontalScrollBarEnabled = true
            }
        },
        modifier = modifier,
        update = { webView ->
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        },
    )
}
