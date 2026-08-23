package com.ampairs.imagesearch.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import com.ampairs.imagesearch.scrape.ImageScraperJs
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.WebKit.WKScriptMessage
import platform.WebKit.WKScriptMessageHandlerProtocol
import platform.WebKit.WKUserContentController
import platform.WebKit.WKUserScript
import platform.WebKit.WKUserScriptInjectionTime
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.darwin.NSObject

/** Wires `window.__ampairsPost` to the WKScriptMessageHandler named "ampairsBridge". */
private const val IOS_SHIM =
    "window.__ampairsPost = function(j){ window.webkit.messageHandlers.ampairsBridge.postMessage(j); };"

private const val BROWSER_UA =
    "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 " +
        "(KHTML, like Gecko) Version/17.0 Mobile Safari/605.1.15"

private class ScrapeMessageHandler(
    private val sink: (String) -> Unit,
) : NSObject(), WKScriptMessageHandlerProtocol {
    override fun userContentController(
        userContentController: WKUserContentController,
        didReceiveScriptMessage: WKScriptMessage,
    ) {
        (didReceiveScriptMessage.body as? String)?.let(sink)
    }
}

/** iOS headless scraper — a [WKWebView] with a user script + message-handler bridge. */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalForeignApi::class)
@Composable
actual fun ImageSearchWebView(
    url: String,
    onResults: (String) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier,
) {
    val currentOnResults = rememberUpdatedState(onResults)
    val lastUrl = remember { arrayOfNulls<String>(1) }
    val handler = remember { ScrapeMessageHandler { currentOnResults.value(it) } }

    UIKitView(
        modifier = modifier,
        factory = {
            val controller = WKUserContentController().apply {
                addUserScript(
                    WKUserScript(
                        source = ImageScraperJs.bootstrap(IOS_SHIM),
                        injectionTime = WKUserScriptInjectionTime.WKUserScriptInjectionTimeAtDocumentEnd,
                        forMainFrameOnly = true,
                    )
                )
                addScriptMessageHandler(handler, name = "ampairsBridge")
            }
            val config = WKWebViewConfiguration().apply { userContentController = controller }
            WKWebView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0), configuration = config).apply {
                customUserAgent = BROWSER_UA
            }
        },
        update = { webView ->
            if (url.isNotBlank() && lastUrl[0] != url) {
                lastUrl[0] = url
                NSURL(string = url)?.let { webView.loadRequest(NSURLRequest.requestWithURL(it)) }
            }
        },
    )
}
