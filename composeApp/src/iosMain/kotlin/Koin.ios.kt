import coil3.ImageLoader
import coil3.PlatformContext
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import coil3.util.DebugLogger
import com.ampairs.auth.api.TokenRepository
import com.ampairs.aws.s3.IosS3Client
import com.ampairs.aws.s3.S3Client
import com.ampairs.common.DeviceService
import com.ampairs.common.ImageCacheKeyer
import com.ampairs.common.coroutines.DispatcherProvider
import com.ampairs.common.database.DatabasePathProvider
import com.ampairs.common.database.IosDatabasePathProvider
import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.httpClient
import com.ampairs.common.theme.iosAppConfigModule
import io.ktor.client.engine.darwin.Darwin
import kotlinx.cinterop.ExperimentalForeignApi
import okio.Path.Companion.toPath
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual val platformModule: Module = module {
    single<io.ktor.client.engine.HttpClientEngine> {
        Darwin.create()
    }
    single<DeviceService> { IosDeviceService() }

    // Database path provider and factory for workspace-aware databases
    single<DatabasePathProvider> { IosDatabasePathProvider() }
    single { WorkspaceAwareDatabaseFactory(get(), DispatcherProvider.io) }

    // Include theme DataStore module for iOS
    includes(iosAppConfigModule)
}

@OptIn(ExperimentalForeignApi::class)
fun generateImageLoader(): ImageLoader {
    val helper = object : KoinComponent {
        val engine: io.ktor.client.engine.HttpClientEngine by inject()
        val tokenRepository: TokenRepository by inject()
    }
    val client = httpClient(helper.engine, helper.tokenRepository)

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

actual val awsModule: Module = module {
    single { IosS3Client() } bind (S3Client::class)
}

actual val authPlatformModule: Module = com.ampairs.auth.authPlatformModule
actual val workspacePlatformModule: Module = com.ampairs.workspace.workspacePlatformModule
actual val customerPlatformModule: Module = com.ampairs.customer.di.customerPlatformModule
actual val productPlatformModule: Module = com.ampairs.product.productPlatformModule
actual val businessPlatformModule: Module = com.ampairs.business.businessPlatformModule
actual val taxPlatformModule: Module = com.ampairs.tax.taxPlatformModule
// Temporarily commented out pending customer integration updates
// actual val orderPlatformModule: Module = com.ampairs.order.orderPlatformModule
// actual val invoicePlatformModule: Module = com.ampairs.invoice.invoicePlatformModule
// actual val inventoryPlatformModule: Module = com.ampairs.inventory.inventoryPlatformModule
// actual val tallyPlatformModule: Module = com.ampairs.tally.tallyPlatformModule
