import com.ampairs.app.BuildConfig
import com.ampairs.aws.s3.AwsS3Client
import com.ampairs.aws.s3.S3Client
import com.ampairs.common.DeviceService
import com.ampairs.common.config.PlatformConfig
import com.ampairs.common.database.AndroidDatabasePathProvider
import com.ampairs.common.database.DatabasePathProvider
import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.theme.androidAppConfigModule
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

// Initialize PlatformConfig with BuildConfig values (BuildConfig is only available here in the app module)
@Suppress("ObjectPropertyName")
private val _platformConfigInit = PlatformConfig.configure(
    apiBaseUrl = BuildConfig.API_BASE_URL,
    environment = BuildConfig.ENVIRONMENT,
    isDebug = BuildConfig.DEBUG
)

actual val platformModule: Module = module {
    this.single {
        OkHttp.create()
    }
    single<DeviceService> { AndroidDeviceService(androidContext()) }

    // Database path provider and factory for workspace-aware databases
    single<DatabasePathProvider> { AndroidDatabasePathProvider(androidContext()) }
    single { WorkspaceAwareDatabaseFactory(get(), Dispatchers.IO) }

    // Include theme DataStore module for Android
    includes(androidAppConfigModule)
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
actual val unitPlatformModule: Module = com.ampairs.unit.unitPlatformModule
actual val subscriptionPlatformModule: Module = com.ampairs.subscription.di.subscriptionPlatformModule
actual val orderPlatformModule: Module = com.ampairs.order.orderPlatformModule
actual val invoicePlatformModule: Module = com.ampairs.invoice.invoicePlatformModule
actual val inventoryPlatformModule: Module = com.ampairs.inventory.inventoryPlatformModule
