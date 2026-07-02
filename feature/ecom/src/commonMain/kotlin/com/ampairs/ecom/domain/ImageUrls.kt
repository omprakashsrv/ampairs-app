package com.ampairs.ecom.domain

import com.ampairs.common.config.ConfigurationManager
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

private val imageJson = Json { ignoreUnknownKeys = true }

/**
 * Turn a server image reference into an absolute HTTP URL. The backend returns root-relative paths
 * (e.g. `/api/file/v1/images/{id}/download`) with no scheme/host; Coil treats a leading-`/` string
 * as a local FILE path and fails (on Desktop: `FileNotFoundException`). Prepending the API host makes
 * it a real HTTP URL that loads on every platform.
 *
 * Idempotent: a value that is already absolute (doesn't start with `/`) is returned unchanged, so it
 * is safe to apply at both the write (mapper) and read (accessor) boundaries.
 */
fun String.toAbsoluteImageUrl(): String =
    if (startsWith("/")) "${ConfigurationManager.apiBaseUrl}$this" else this

/** Decode the JSON-array `image_urls` column into a list of absolute image URLs. */
fun String.imageUrls(): List<String> =
    runCatching { imageJson.decodeFromString(ListSerializer(String.serializer()), this) }
        .getOrDefault(emptyList())
        .map { it.toAbsoluteImageUrl() }

/** First image URL (or null) from the JSON-array `image_urls` column. */
fun String.firstImageUrl(): String? = imageUrls().firstOrNull()

