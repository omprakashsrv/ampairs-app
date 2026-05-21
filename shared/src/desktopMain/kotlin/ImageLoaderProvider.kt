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
import okio.Path.Companion.toOkioPath
import java.io.File

fun generateImageLoader(engine: HttpClientEngine, tokenRepository: TokenRepository): ImageLoader {
    val client = httpClient(engine, tokenRepository)
    return ImageLoader.Builder(PlatformContext.INSTANCE)
        .memoryCache {
            MemoryCache.Builder()
                .maxSizeBytes(32 * 1024 * 1024) // 32MB
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(getCacheDir().toOkioPath().resolve("image_cache"))
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

private fun getCacheDir(): File {
    return try {
        com.ampairs.common.desktop.DataDirectoryManager.getCacheDir()
    } catch (e: IllegalStateException) {
        val fallbackDir = File(System.getProperty("user.home"), ".ampairs/cache")
        fallbackDir.mkdirs()
        fallbackDir
    }
}

fun getDatabaseDir(): File {
    return try {
        com.ampairs.common.desktop.DataDirectoryManager.getDatabaseDir()
    } catch (e: IllegalStateException) {
        throw IllegalStateException(
            "Data directory not set. This indicates an initialization order problem.",
            e
        )
    }
}
