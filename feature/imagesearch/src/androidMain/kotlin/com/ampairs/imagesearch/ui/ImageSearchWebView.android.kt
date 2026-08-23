package com.ampairs.imagesearch.ui

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.ampairs.imagesearch.scrape.ImageScraperJs

/** Wires `window.__ampairsPost` to the injected `AmpairsBridge` (addJavascriptInterface). */
private const val ANDROID_SHIM =
    "window.__ampairsPost = function(j){ AmpairsBridge.onResults(j); };"

private const val BROWSER_UA =
    "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/124.0 Mobile Safari/537.36"

/** Android headless scraper — a [WebView] running [ImageScraperJs.SCRIPT] after each page load. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun ImageSearchWebView(
    url: String,
    onResults: (String) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier,
) {
    val currentOnResults = rememberUpdatedState(onResults)
    val lastUrl = remember { arrayOfNulls<String>(1) }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.userAgentString = BROWSER_UA
                addJavascriptInterface(
                    object {
                        @JavascriptInterface
                        fun onResults(json: String) {
                            currentOnResults.value(json)
                        }
                    },
                    "AmpairsBridge",
                )
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, finishedUrl: String?) {
                        evaluateJavascript(ImageScraperJs.bootstrap(ANDROID_SHIM), null)
                    }
                }
            }
        },
        update = { webView ->
            if (url.isNotBlank() && lastUrl[0] != url) {
                lastUrl[0] = url
                webView.loadUrl(url)
            }
        },
    )
}
