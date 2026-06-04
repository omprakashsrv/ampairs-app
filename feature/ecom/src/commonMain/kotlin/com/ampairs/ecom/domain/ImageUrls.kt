package com.ampairs.ecom.domain

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

private val imageJson = Json { ignoreUnknownKeys = true }

/** Decode the JSON-array `image_urls` column into a list. */
fun String.imageUrls(): List<String> =
    runCatching { imageJson.decodeFromString(ListSerializer(String.serializer()), this) }.getOrDefault(emptyList())

/** First image URL (or null) from the JSON-array `image_urls` column. */
fun String.firstImageUrl(): String? = imageUrls().firstOrNull()
