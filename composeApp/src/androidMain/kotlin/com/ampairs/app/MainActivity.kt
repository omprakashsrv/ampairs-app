package com.ampairs.app

import MainView
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import coil3.util.DebugLogger
import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.ActivityProvider
import com.ampairs.common.ImageCacheKeyer
import com.ampairs.common.httpClient
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init
import io.github.vinceglb.filekit.manualFileKitCoreInitialization
import io.ktor.client.engine.HttpClientEngine
import okio.Path.Companion.toOkioPath
import org.koin.android.ext.android.get

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Register activity for Firebase Phone Auth
        ActivityProvider.setActivity(this)

        // Enable modern edge-to-edge (Android 15+ compatible)
        enableEdgeToEdge()

        actionBar?.hide()

        // Initialize FileKit for Android platform
        FileKit.init(this)

        setContent {
            setSingletonImageLoaderFactory { context ->
                generateImageLoader()
            }
            MainView()
        }
    }

    // Your shared Ktor client with global auth headers
    private fun generateImageLoader(): ImageLoader {
        val engine = get<HttpClientEngine>()
        val tokenRepository = get<TokenRepository>()
        val client = httpClient(engine, tokenRepository)

        return ImageLoader.Builder(this@MainActivity)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(this@MainActivity, 0.25) // Increased for better on-demand performance
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(this@MainActivity.cacheDir.resolve("customer_images_cache").toOkioPath())
                    .maxSizeBytes(100L * 1024 * 1024) // Increased to 100MB for better offline experience
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

    override fun onDestroy() {
        super.onDestroy()
        // Clear activity reference to avoid memory leaks
        ActivityProvider.clearActivity()
    }
}

