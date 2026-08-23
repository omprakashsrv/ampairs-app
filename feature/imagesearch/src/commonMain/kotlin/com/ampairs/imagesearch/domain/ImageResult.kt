package com.ampairs.imagesearch.domain

/**
 * A single scraped image search result.
 *
 * @property thumbnailUrl small preview shown in the grid — either an `http(s)` URL or a `data:` URI.
 * @property thumbnailBytes decoded bytes when [thumbnailUrl] is a `data:` URI (Coil can render these
 *   directly, and they double as an offline download fallback). Null for plain URL thumbnails.
 * @property fullResUrl best-effort original/full-resolution image URL (decoded from Google's
 *   `/imgres?imgurl=` link). May be blank when only a thumbnail was available.
 * @property sourceHost host of [fullResUrl], shown as a tiny caption for provenance.
 */
data class ImageResult(
    val thumbnailUrl: String,
    val thumbnailBytes: ByteArray? = null,
    val fullResUrl: String = "",
    val sourceHost: String = "",
    val width: Int? = null,
    val height: Int? = null,
) {
    /** Stable identity for LazyGrid keys / dedupe — prefer the full URL, else the thumbnail. */
    val id: String get() = fullResUrl.ifBlank { thumbnailUrl }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as ImageResult
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
