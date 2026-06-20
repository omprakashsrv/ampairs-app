package com.ampairs.printing.ui

import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/** Android preview via [WebView] — the same engine `PrintManager` renders for OS print. */
@Composable
actual fun HtmlPreview(html: String, modifier: Modifier) {
    AndroidView(
        factory = { context -> WebView(context) },
        modifier = modifier,
        update = { webView ->
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        },
    )
}
