package com.ampairs.common.image

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.crossfade
import coil3.util.DebugLogger
import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.config.ConfigurationManager
import okio.Path.Companion.toPath
import org.koin.mp.KoinPlatform

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
            // Add network cache interceptor for better offline support and auth headers
            add(AuthenticatedNetworkInterceptor())
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
 * Network interceptor that adds authentication headers for API URLs
 * and provides offline-first image loading capabilities.
 */
private class AuthenticatedNetworkInterceptor : coil3.intercept.Interceptor {
    override suspend fun intercept(chain: coil3.intercept.Interceptor.Chain): coil3.request.ImageResult {
        val request = chain.request
        val url = request.data.toString()

        // Check if this is an API URL that needs authentication
        val apiBaseUrl = ConfigurationManager.apiBaseUrl
        val needsAuth = url.startsWith(apiBaseUrl)

        val newRequest = request.newBuilder()
            .apply {
                // Add auth header for API URLs
                if (needsAuth) {
                    try {
                        val tokenRepository = KoinPlatform.getKoin().get<TokenRepository>()
                        val accessToken = tokenRepository.getAccessToken()
                        if (!accessToken.isNullOrBlank()) {
                            httpHeaders(
                                NetworkHeaders.Builder()
                                    .set("Authorization", "Bearer $accessToken")
                                    .build()
                            )
                        }
                    } catch (e: Exception) {
                        // Token repository not available, proceed without auth
                        println("CoilImageLoader: Could not get auth token: ${e.message}")
                    }
                }

                // Cache policies for different image types
                if (url.contains("customer-images") || url.contains("/picture")) {
                    // Prefer cache for images (offline-first)
                    memoryCachePolicy(coil3.request.CachePolicy.ENABLED)
                    diskCachePolicy(coil3.request.CachePolicy.ENABLED)
                    networkCachePolicy(coil3.request.CachePolicy.ENABLED)
                }
            }
            .build()

        return chain.withRequest(newRequest).proceed()
    }
}