package com.ampairs.di

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import coil3.util.DebugLogger
import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.ImageCacheKeyer
import com.ampairs.common.httpClient
import io.ktor.client.engine.HttpClientEngine
import kotlinx.cinterop.ExperimentalForeignApi
import okio.Path.Companion.toPath
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
fun generateImageLoader(engine: HttpClientEngine, tokenRepository: TokenRepository): ImageLoader {
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
                inDomains = NSUserDomainMask
            ).firstOrNull() as? platform.Foundation.NSURL)?.path ?: ""

            DiskCache.Builder()
                .directory("$cacheDir/ampairs/image_cache".toPath())
                .maxSizeBytes(512L * 1024 * 1024) // 512MB
                .build()
        }
        .components {
            add(KtorNetworkFetcherFactory(client))
            add(ImageCacheKeyer())
        }
        .crossfade(true)
        .logger(DebugLogger())
        .build()
}
