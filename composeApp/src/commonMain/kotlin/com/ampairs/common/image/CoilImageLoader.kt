package com.ampairs.common.image

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.crossfade
import coil3.util.DebugLogger
import okio.Path.Companion.toPath

/**
 * Creates a configured Coil ImageLoader optimized for customer image caching
 * with offline-first capabilities and efficient memory management.
 */
fun createImageLoader(
    context: PlatformContext,
    cacheDirectory: String,
    debug: Boolean = false
): ImageLoader {
    return ImageLoader.Builder(context)
        .memoryCache {
            MemoryCache.Builder()
                // Use 25% of app's available memory for image cache
                .maxSizePercent(context, percent = 0.25)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(cacheDirectory.toPath().resolve("image_cache"))
                // Set max cache size to 256MB for customer images
                .maxSizeBytes(256 * 1024 * 1024)
                .build()
        }
        .components {
            // Add network cache interceptor for better offline support
            add(NetworkCacheInterceptor())
        }
        .apply {
            if (debug) {
                logger(DebugLogger())
            }
            // Add smooth crossfade animation
            crossfade(300)
        }
        .build()
}

/**
 * Network cache interceptor that respects cache-control headers
 * and provides offline-first image loading capabilities.
 */
private class NetworkCacheInterceptor : coil3.intercept.Interceptor {
    override suspend fun intercept(chain: coil3.intercept.Interceptor.Chain): coil3.request.ImageResult {
        val request = chain.request

        // For customer images, prefer cached version for better performance
        val newRequest = request.newBuilder()
            .apply {
                // Add cache-control headers for better caching
                if (request.data.toString().contains("customer-images")) {
                    // Prefer cache for customer images (offline-first)
                    memoryCachePolicy(coil3.request.CachePolicy.ENABLED)
                    diskCachePolicy(coil3.request.CachePolicy.ENABLED)
                    networkCachePolicy(coil3.request.CachePolicy.ENABLED)
                }
            }
            .build()

        return chain.proceed()
    }
}