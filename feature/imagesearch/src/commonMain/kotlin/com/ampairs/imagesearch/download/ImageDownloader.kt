package com.ampairs.imagesearch.download

import com.ampairs.common.id_generator.UidGenerator
import com.ampairs.file.api.FilePickerResult
import com.ampairs.imagesearch.domain.ImageResult
import com.ampairs.imagesearch.scrape.ImageResultParser
import com.ampairs.imagesearch.util.ImageSearchLogger
import dev.zacsweers.metro.Inject
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess

/**
 * Downloads a chosen [ImageResult] and turns it into a [FilePickerResult] for the existing file
 * pipeline (`FileRepository.saveLocally`).
 *
 * SECURITY: uses a **bare** [HttpClient] built from the injected engine — NO `Auth` plugin and NO
 * `X-Workspace-ID`/JWT default headers. This client talks to arbitrary third-party image hosts, so it
 * must never carry Ampairs credentials. Do NOT swap this for `com.ampairs.common.httpClient(...)`.
 *
 * Robustness ladder (per /offline-sync Rule 3 — Ktor's blanket timeout is overridden per request):
 *  1. `data:` URI thumbnail already decoded → use those bytes (zero network).
 *  2. full-res URL.
 *  3. thumbnail URL (hotlink/403 fallback).
 */
@Inject
class ImageDownloader(engine: HttpClientEngine) {

    private val client = HttpClient(engine) {
        expectSuccess = false
        install(HttpTimeout) {
            requestTimeoutMillis = REQUEST_TIMEOUT_MS
            connectTimeoutMillis = 30_000L
            socketTimeoutMillis = REQUEST_TIMEOUT_MS
        }
    }

    /** Download [result] to a [FilePickerResult], or [Result.failure] if nothing usable came back. */
    suspend fun download(result: ImageResult): Result<FilePickerResult> {
        // 1. Inline data-URI thumbnail — no network needed.
        result.thumbnailBytes?.let { bytes ->
            val ct = ImageResultParser.dataUriContentType(result.thumbnailUrl) ?: "image/jpeg"
            return finalize(bytes, ct)
        }

        // 2/3. Try full-res, then the thumbnail URL.
        val candidates = listOf(result.fullResUrl, result.thumbnailUrl)
            .filter { it.isNotBlank() && it.startsWith("http") }
            .distinct()

        for (url in candidates) {
            val fetched = fetch(url)
            if (fetched != null) return finalize(fetched.first, fetched.second)
        }
        return Result.failure(IllegalStateException("Could not download the selected image"))
    }

    private suspend fun fetch(url: String): Pair<ByteArray, String?>? = try {
        val response: HttpResponse = client.get(url) {
            header(HttpHeaders.UserAgent, BROWSER_UA)
            timeout { requestTimeoutMillis = REQUEST_TIMEOUT_MS }
        }
        if (!response.status.isSuccess()) {
            ImageSearchLogger.w(TAG, "Download $url returned ${response.status}")
            null
        } else {
            val bytes = response.body<ByteArray>()
            if (bytes.isEmpty()) null else bytes to response.contentType()?.toString()
        }
    } catch (e: Exception) {
        ImageSearchLogger.w(TAG, "Download failed for $url", e)
        null
    }

    private fun finalize(bytes: ByteArray, rawContentType: String?): Result<FilePickerResult> {
        if (bytes.size > MAX_FILE_SIZE) {
            return Result.failure(IllegalStateException("Image is larger than 10 MB"))
        }
        val contentType = normalizeContentType(rawContentType)
            ?: return Result.failure(IllegalStateException("Unsupported image type"))
        val ext = extensionFor(contentType)
        val fileName = "web_${UidGenerator.generateUid(UID_PREFIX)}.$ext"
        return Result.success(
            FilePickerResult(
                fileName = fileName,
                contentType = contentType,
                fileSize = bytes.size.toLong(),
                imageData = bytes,
            )
        )
    }

    /** Keep only real image types (mirrors FileKitFilePicker's allow-list). */
    private fun normalizeContentType(raw: String?): String? {
        val ct = raw?.substringBefore(';')?.trim()?.lowercase()
        return when (ct) {
            "image/jpeg", "image/jpg" -> "image/jpeg"
            "image/png" -> "image/png"
            "image/webp" -> "image/webp"
            "image/gif" -> "image/gif"
            "image/bmp" -> "image/bmp"
            else -> null
        }
    }

    private fun extensionFor(contentType: String): String = when (contentType) {
        "image/jpeg" -> "jpg"
        "image/png" -> "png"
        "image/webp" -> "webp"
        "image/gif" -> "gif"
        "image/bmp" -> "bmp"
        else -> "jpg"
    }

    companion object {
        private const val TAG = "ImageDownloader"
        private const val UID_PREFIX = "IMG"
        private const val MAX_FILE_SIZE = 10 * 1024 * 1024L
        private const val REQUEST_TIMEOUT_MS = 120_000L
        private const val BROWSER_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/124.0 Safari/537.36"
    }
}
