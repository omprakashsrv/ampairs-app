package com.ampairs.storefront.di

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.ImageCacheKeyer
import com.ampairs.common.httpClient
import io.ktor.client.engine.HttpClientEngine
import kotlinx.cinterop.ExperimentalForeignApi
import okio.Path.Companion.toPath
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

/**
 * iOS Coil [ImageLoader] for the storefront app. Mirrors `generateImageLoader` in :shared (not a
 * dependency here). Uses the shared authenticated [httpClient] so image requests carry the JWT.
 */
@OptIn(ExperimentalForeignApi::class)
fun generateStorefrontImageLoader(
    engine: HttpClientEngine,
    tokenRepository: TokenRepository,
): ImageLoader {
    val client = httpClient(engine, tokenRepository)
    return ImageLoader.Builder(PlatformContext.INSTANCE)
        .memoryCache {
            MemoryCache.Builder()
                .maxSizeBytes(32 * 1024 * 1024) // 32MB
                .build()
        }
        .diskCache {
            @Suppress("CAST_NEVER_SUCCEEDS")
            val cacheDir = (NSFileManager.defaultManager.URLsForDirectory(
                directory = NSCachesDirectory,
                inDomains = NSUserDomainMask,
            ).firstOrNull() as? NSURL)?.path ?: ""
            DiskCache.Builder()
                .directory("$cacheDir/ampairs/storefront_image_cache".toPath())
                .maxSizeBytes(256L * 1024 * 1024) // 256MB
                .build()
        }
        .components {
            add(KtorNetworkFetcherFactory(client))
            add(ImageCacheKeyer())
        }
        .crossfade(true)
        .build()
}
