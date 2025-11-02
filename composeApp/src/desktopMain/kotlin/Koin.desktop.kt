// Temporarily disabled tally imports
// import com.ampairs.tally.TallyApi
// import com.ampairs.tally.TallyApiImpl
// import com.ampairs.tally.TallyRepository
// import com.ampairs.tally.ui.TallyViewModel
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import coil3.util.DebugLogger
import com.ampairs.auth.api.TokenRepository
import com.ampairs.aws.s3.AwsS3Client
import com.ampairs.aws.s3.S3Client
import com.ampairs.common.DeviceService
import com.ampairs.common.ImageCacheKeyer
import com.ampairs.common.config.desktopAppConfigModule
import com.ampairs.common.database.DatabasePathProvider
import com.ampairs.common.database.DesktopDatabasePathProvider
import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.httpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.cio.endpoint
import kotlinx.coroutines.Dispatchers
import okio.Path.Companion.toOkioPath
import org.koin.core.context.GlobalContext
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module
import java.io.File

actual val platformModule: Module = module {
    // JVM/Android CIO engine
    single<HttpClientEngine> {
        CIO.create {
            requestTimeout = 15_000
            endpoint {
                connectTimeout = 10_000
                socketTimeout = 15_000
            }
        }
    }
    single { AwsS3Client() } bind (S3Client::class)
    single<DeviceService> { DesktopDeviceService() }
    // Temporarily disabled tally dependencies
    // single { TallyApiImpl(get()) } bind (TallyApi::class)
    // single { TallyRepository(get()) }
    // single { TallyViewModel(get(), get(), get(), get(), get()) }

    // Database path provider and factory for workspace-aware databases
    single<DatabasePathProvider> { DesktopDatabasePathProvider() }
    single { WorkspaceAwareDatabaseFactory(get(), Dispatchers.IO) }

    // Include theme DataStore module for Desktop
    includes(desktopAppConfigModule)
}

fun generateImageLoader(): ImageLoader {
    val engine = GlobalContext.get().get<HttpClientEngine>()
    val tokenRepository = GlobalContext.get().get<TokenRepository>()
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

// about currentOperatingSystem, see app
private fun getCacheDir(): File {
    val ApplicationName = "ampairs"
    return when (currentOperatingSystem) {
        OperatingSystem.Windows -> File(System.getenv("AppData"), "$ApplicationName/cache")
        OperatingSystem.Linux -> File(System.getProperty("user.home"), ".cache/$ApplicationName")
        OperatingSystem.MacOS -> File(
            System.getProperty("user.home"),
            "Library/Caches/$ApplicationName"
        )

        else -> throw IllegalStateException("Unsupported operating system")
    }
}

fun getDatabaseDir(): File {
    val ApplicationName = "ampairs"
    return when (currentOperatingSystem) {
        OperatingSystem.Windows -> File(System.getenv("AppData"), "$ApplicationName")
        OperatingSystem.Linux -> File(System.getProperty("user.home"), "$ApplicationName")
        OperatingSystem.MacOS -> File(System.getProperty("user.home"), "$ApplicationName")
        else -> throw IllegalStateException("Unsupported operating system")
    }
}

actual val awsModule: Module = module {
    single { AwsS3Client() } bind (S3Client::class)
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
