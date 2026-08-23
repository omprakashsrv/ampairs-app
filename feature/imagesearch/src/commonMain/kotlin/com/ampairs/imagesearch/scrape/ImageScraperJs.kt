package com.ampairs.imagesearch.scrape

import io.ktor.http.encodeURLParameter

/**
 * The single fragile point of the whole module: the Google Images URL + the JavaScript that scrapes
 * result nodes out of the (WebView-loaded) results page and posts them back to the host.
 *
 * Everything Google-DOM-specific lives HERE so a markup change is a one-file patch — and so the whole
 * scrape can later be swapped for a backend search proxy without touching the ViewModel/UI/product.
 *
 * ## Host bridge contract
 * The script calls `window.__ampairsPost(jsonString)` with a JSON array of results. Each platform's
 * WebView actual must define `window.__ampairsPost` BEFORE (or alongside) injecting [SCRIPT]:
 *  - Android:  `window.__ampairsPost = j => AmpairsBridge.onResults(j)`   (addJavascriptInterface)
 *  - iOS:      `window.__ampairsPost = j => window.webkit.messageHandlers.ampairsBridge.postMessage(j)`
 *  - Desktop:  `window.__ampairsPost = j => ampairsBridge.onResults(j)`   (JSObject.setMember)
 *
 * Use [bootstrap] to get `shim + SCRIPT` in one string for platforms that inject a single blob.
 */
object ImageScraperJs {

    private const val TAG = "ImageScraperJs"

    /** Build the Google Images results URL for [query]. `safe=active` keeps results SFW. */
    fun searchUrl(query: String): String {
        val q = query.trim().encodeURLParameter(spaceToPlus = true)
        return "https://www.google.com/search?tbm=isch&hl=en&safe=active&q=$q"
    }

    /**
     * The scraper. Runs on the results page: scans `/imgres?imgurl=…` anchors (full-res) + their
     * nested `<img>` (thumbnail), dedupes, auto-scrolls a few times to pull in lazy results, and
     * posts batches to `window.__ampairsPost`. Fully wrapped in try/catch — a DOM shift degrades to
     * "no results", never a thrown error.
     */
    val SCRIPT: String = """
        (function () {
          if (window.__ampairsScraping) return;
          window.__ampairsScraping = true;
          var seen = {};

          function param(url, key) {
            try {
              var m = new RegExp('[?&]' + key + '=([^&]+)').exec(url);
              return m ? decodeURIComponent(m[1]) : '';
            } catch (e) { return ''; }
          }
          function host(url) {
            try { return new URL(url).hostname; } catch (e) { return ''; }
          }

          function collect() {
            var out = [];
            try {
              var anchors = document.querySelectorAll('a[href*="/imgres?"]');
              for (var i = 0; i < anchors.length; i++) {
                var a = anchors[i];
                var full = param(a.href, 'imgurl');
                var img = a.querySelector('img');
                var thumb = img ? (img.src || img.getAttribute('data-src') || '') : '';
                if (!thumb && !full) continue;
                var key = full || thumb;
                if (seen[key]) continue;
                seen[key] = true;
                out.push({ t: thumb, f: full, h: host(full), w: img ? img.naturalWidth : 0, e: img ? img.naturalHeight : 0 });
              }
              if (out.length === 0) {
                // Fallback: bare <img> tags (markup with no imgres anchor).
                var imgs = document.querySelectorAll('img');
                for (var j = 0; j < imgs.length; j++) {
                  var s = imgs[j].src || '';
                  if (!s || s.length < 24) continue;
                  if (seen[s]) continue;
                  seen[s] = true;
                  out.push({ t: s, f: '', h: '', w: imgs[j].naturalWidth, e: imgs[j].naturalHeight });
                }
              }
            } catch (e) {}
            return out;
          }

          function flush() {
            var batch = collect();
            if (batch.length > 0 && window.__ampairsPost) {
              try { window.__ampairsPost(JSON.stringify(batch)); } catch (e) {}
            }
          }

          // Initial pass, then observe + auto-scroll to trigger lazy loading.
          flush();
          try {
            var obs = new MutationObserver(function () { flush(); });
            obs.observe(document.body, { childList: true, subtree: true });
          } catch (e) {}

          var scrolls = 0;
          var timer = setInterval(function () {
            try { window.scrollTo(0, document.body.scrollHeight); } catch (e) {}
            flush();
            if (++scrolls >= 8) { clearInterval(timer); }
          }, 700);
        })();
    """.trimIndent()

    /** `shim + SCRIPT` for platforms that inject one blob. [shim] wires `window.__ampairsPost`. */
    fun bootstrap(shim: String): String = shim + "\n" + SCRIPT
}
