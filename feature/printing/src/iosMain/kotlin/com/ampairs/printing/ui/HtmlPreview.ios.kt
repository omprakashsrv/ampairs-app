package com.ampairs.printing.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration

/** iOS preview via [WKWebView] — the same engine AirPrint renders for OS print. */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalForeignApi::class)
@Composable
actual fun HtmlPreview(html: String, modifier: Modifier) {
    UIKitView(
        factory = {
            WKWebView(
                frame = CGRectMake(0.0, 0.0, 0.0, 0.0),
                configuration = WKWebViewConfiguration(),
            )
        },
        modifier = modifier,
        update = { webView ->
            // WKWebView defaults to a ~980px layout and shrinks the page; the viewport meta makes it
            // lay out at the device width. (Other platforms fit at the view width without it.)
            val withViewport = html.replaceFirst(
                "<head>",
                "<head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">",
            )
            webView.loadHTMLString(withViewport, baseURL = null)
        },
    )
}
