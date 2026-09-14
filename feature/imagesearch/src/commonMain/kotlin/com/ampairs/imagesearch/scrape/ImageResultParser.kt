package com.ampairs.imagesearch.scrape

import com.ampairs.imagesearch.domain.ImageResult
import com.ampairs.imagesearch.util.ImageSearchLogger
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Parses the JSON batches the scraper posts (`window.__ampairsPost`) into [ImageResult]s.
 * Pure + unit-testable — no platform APIs. The wire keys are terse (see [ScrapedImage]) to keep the
 * JS payload small.
 */
object ImageResultParser {

    private const val TAG = "ImageResultParser"
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Serializable
    private data class ScrapedImage(
        val t: String = "",   // thumbnail (url or data: URI)
        val f: String = "",   // full-res url
        val h: String = "",   // host
        val w: Int = 0,       // width
        val e: Int = 0,       // height
    )

    /** Parse one bridge payload. Returns empty list on any malformed input (never throws). */
    fun parse(payload: String): List<ImageResult> {
        if (payload.isBlank()) return emptyList()
        val scraped = try {
            json.decodeFromString<List<ScrapedImage>>(payload)
        } catch (e: Exception) {
            ImageSearchLogger.w(TAG, "Failed to parse scrape payload", e)
            return emptyList()
        }
        return scraped.mapNotNull { it.toImageResult() }
    }

    private fun ScrapedImage.toImageResult(): ImageResult? {
        val thumb = t.trim()
        if (thumb.isBlank() && f.isBlank()) return null
        return ImageResult(
            thumbnailUrl = thumb.ifBlank { f },
            thumbnailBytes = decodeDataUri(thumb),
            fullResUrl = f.trim(),
            sourceHost = h.trim(),
            width = w.takeIf { it > 0 },
            height = e.takeIf { it > 0 },
        )
    }

    /** Decode a `data:[<mime>];base64,<payload>` URI to bytes; null for non-data / undecodable. */
    @OptIn(ExperimentalEncodingApi::class)
    fun decodeDataUri(value: String): ByteArray? {
        if (!value.startsWith("data:")) return null
        val comma = value.indexOf(',')
        if (comma < 0) return null
        val meta = value.substring(5, comma)
        if (!meta.contains("base64", ignoreCase = true)) return null
        return try {
            Base64.decode(value.substring(comma + 1))
        } catch (e: Exception) {
            ImageSearchLogger.w(TAG, "Failed to decode data URI thumbnail", e)
            null
        }
    }

    /** Content-type sniffed from a `data:` URI's metadata, e.g. "image/png". Null if not a data URI. */
    fun dataUriContentType(value: String): String? {
        if (!value.startsWith("data:")) return null
        val comma = value.indexOf(',')
        if (comma < 0) return null
        val meta = value.substring(5, comma).substringBefore(';').trim()
        return meta.ifBlank { null }
    }
}
