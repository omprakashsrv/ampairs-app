import com.ampairs.auth.authModule
import com.ampairs.business.businessModule
import com.ampairs.common.firebase.di.firebaseModule
import com.ampairs.common.theme.themeModule
import com.ampairs.customer.di.customerModule
import com.ampairs.customer.ui.components.location.locationServiceModule
import com.ampairs.event.di.eventModule
import com.ampairs.form.di.formModule
import com.ampairs.product.productModule
import com.ampairs.tax.taxModule
import com.ampairs.workspace.workspaceModule
import org.koin.core.KoinApplication
import org.koin.core.module.Module


fun initKoin(koinApplication: KoinApplication): KoinApplication {
    // Initialize module providers for dynamic navigation
    koinApplication.modules(
        listOf(
            themeModule,
            firebaseModule,
            platformModule,
            awsModule,
            // Platform-specific Room database modules
            authPlatformModule,
            workspacePlatformModule,
            customerPlatformModule,
            productPlatformModule,
            businessPlatformModule,
            taxPlatformModule,
            authModule(),
            workspaceModule(),
            eventModule(),
            formModule,
            customerModule,
            locationServiceModule,
            productModule(),
            businessModule(),
            taxModule,
        )
    )
    return koinApplication
}

expect val platformModule: Module
expect val awsModule: Module
expect val authPlatformModule: Module
expect val workspacePlatformModule: Module
expect val customerPlatformModule: Module
expect val productPlatformModule: Module
expect val businessPlatformModule: Module
expect val taxPlatformModule: Module
