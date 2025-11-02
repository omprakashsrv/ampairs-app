package com.ampairs.common

import coil3.key.Keyer
import coil3.request.Options
import io.ktor.http.Url

/**
 * Coil-compatible image cache keyer for URL-based images.
 * Generates cache keys based on host, path, and image options.
 */
class ImageCacheKeyer : Keyer<Url> {

    override fun key(data: Url, options: Options): String {
        // Create cache key based on URL components
        val baseKey = "${data.host}${data.encodedPath}"

        // Add query parameters that affect image rendering
        val queryParams = data.parameters.entries()
            .filter { (key, _) ->
                key in listOf("size", "width", "height", "quality", "format")
            }
            .joinToString("&") { (key, values) ->
                "$key=${values.firstOrNull()}"
            }

        return if (queryParams.isNotEmpty()) {
            "$baseKey?$queryParams"
        } else {
            baseKey
        }
    }
}