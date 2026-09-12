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

    /**
     * Build the Google Images results URL for [query]. `safe=active` keeps results SFW; `tbs=isz:l`
     * biases toward large/high-resolution source images (better quality for product photos).
     */
    fun searchUrl(query: String): String {
        val q = query.trim().encodeURLParameter(spaceToPlus = true)
        return "https://www.google.com/search?tbm=isch&hl=en&safe=active&tbs=isz:l&q=$q"
    }

    /**
     * The scraper. Runs on the results page and harvests full-resolution source URLs two ways,
     * because Google's current markup rarely exposes the classic `/imgres?imgurl=` anchors anymore:
     *
     *  1. **`/imgres?imgurl=` anchors** — the classic path (still seen on the older WebKit that
     *     JavaFX uses on Desktop): `imgurl` = full-res, nested `<img>` = thumbnail.
     *  2. **inline page/script data** — the modern path. Google embeds the real source images as
     *     `["https://host/photo.jpg",height,width]` triples inside `<script>`/AF_initDataCallback
     *     blobs. We regex those out (unescaping `\/`, `\u0026`, `\u003d`) and skip Google's own
     *     thumbnail/asset hosts (gstatic/googleusercontent proxy/ggpht). **This is what makes saved
     *     images full quality** — the visible `<img>` tags are only ~90px gstatic/data-URI previews.
     *
     * Both feed the same `{t,f,h,w,e}` shape; the host de-dupes. Auto-scrolls to pull in lazy
     * results, posts batches to `window.__ampairsPost`. Fully wrapped in try/catch — a DOM/markup
     * shift degrades to "no results", never a thrown error. This is the single fragile point: when
     * Google changes markup, patch here (or swap for a backend proxy) without touching UI/VM.
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
          // A Google-owned thumbnail/asset host — never a real full-res source.
          function isGoogleHost(h) {
            return !h || /(^|\.)(gstatic|google|googleusercontent|ggpht|googleapis)\.com${'$'}/.test(h)
              || h.indexOf('google') === 0;
          }
          function unescapeUrl(u) {
            return u.replace(/\\u003d/gi, '=').replace(/\\u0026/gi, '&')
                    .replace(/\\u003c/gi, '<').replace(/\\u003e/gi, '>')
                    .replace(/\\\//g, '/').replace(/\\"/g, '"');
          }

          // 1. Classic `/imgres?imgurl=` anchors (full-res in the query param).
          function collectAnchors() {
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
            } catch (e) {}
            return out;
          }

          // 2. Modern path: mine `["url",height,width]` triples embedded in inline scripts. These are
          //    the actual source images (full quality). Heavier — run only in the scroll timer.
          function collectScripts() {
            var out = [];
            try {
              var scripts = document.getElementsByTagName('script');
              // Match ["<image url>",height,width] — allow escaped slashes (\/) inside the url; the
              // extension is anchored so ".pngfoo" won't match. unescapeUrl() normalizes it after.
              var re = /\["(https?:[^"]*?\.(?:jpe?g|png|webp|gif|bmp)(?![a-z0-9])[^"]*?)",(\d+),(\d+)\]/gi;
              for (var s = 0; s < scripts.length; s++) {
                var text = scripts[s].textContent || '';
                if (text.indexOf('http') < 0) continue;
                var m;
                while ((m = re.exec(text)) !== null) {
                  var url = unescapeUrl(m[1]);
                  if (url.indexOf('http') !== 0) continue;
                  var h = host(url);
                  if (isGoogleHost(h)) continue;          // skip Google's own thumbnails/proxies
                  if (seen[url]) continue;
                  seen[url] = true;
                  // Google emits [url, height, width]. Use the URL as its own grid thumbnail.
                  out.push({ t: url, f: url, h: h, w: parseInt(m[3], 10) || 0, e: parseInt(m[2], 10) || 0 });
                }
              }
            } catch (e) {}
            return out;
          }

          // Last resort: bare <img> tags (only when nothing else matched). Skip tiny icons/sprites.
          function collectImgs() {
            var out = [];
            try {
              var imgs = document.querySelectorAll('img');
              for (var j = 0; j < imgs.length; j++) {
                var src = imgs[j].src || '';
                if (!src || src.length < 24) continue;
                var w = imgs[j].naturalWidth, hgt = imgs[j].naturalHeight;
                if (w && hgt && (w < 100 || hgt < 100)) continue;
                if (seen[src]) continue;
                seen[src] = true;
                out.push({ t: src, f: '', h: '', w: w, e: hgt });
              }
            } catch (e) {}
            return out;
          }

          function post(batch) {
            if (batch.length > 0 && window.__ampairsPost) {
              try { window.__ampairsPost(JSON.stringify(batch)); } catch (e) {}
            }
          }
          // Cheap pass (observer): anchors only.
          function flush() { post(collectAnchors()); }
          // Deep pass (timer): anchors + inline-script full-res, with a bare-<img> fallback.
          function flushDeep() {
            var batch = collectAnchors().concat(collectScripts());
            if (batch.length === 0) batch = collectImgs();
            post(batch);
          }

          flushDeep();
          try {
            var obs = new MutationObserver(function () { flush(); });
            obs.observe(document.body, { childList: true, subtree: true });
          } catch (e) {}

          var scrolls = 0;
          var timer = setInterval(function () {
            try { window.scrollTo(0, document.body.scrollHeight); } catch (e) {}
            flushDeep();
            if (++scrolls >= 8) { clearInterval(timer); }
          }, 700);
        })();
    """.trimIndent()

    /** `shim + SCRIPT` for platforms that inject one blob. [shim] wires `window.__ampairsPost`. */
    fun bootstrap(shim: String): String = shim + "\n" + SCRIPT
}
