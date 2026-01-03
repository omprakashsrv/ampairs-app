import com.ampairs.auth.authModule
import com.ampairs.business.businessModule
import com.ampairs.common.firebase.di.firebaseModule
import com.ampairs.common.localization.localizationModule
import com.ampairs.common.sentry.sentryModule
import com.ampairs.common.theme.themeModule
import com.ampairs.customer.di.customerModule
import com.ampairs.customer.ui.components.location.locationServiceModule
import com.ampairs.event.di.eventModule
import com.ampairs.form.di.formModule
import com.ampairs.product.productModule
import com.ampairs.subscription.di.subscriptionModule
import com.ampairs.tax.di.taxModule
import com.ampairs.unit.di.unitModule
import com.ampairs.update.di.updateModule
import com.ampairs.workspace.workspaceModule
import org.koin.core.KoinApplication
import org.koin.core.module.Module


fun initKoin(koinApplication: KoinApplication): KoinApplication {
    // Initialize module providers for dynamic navigation
    koinApplication.modules(
        listOf(
            themeModule,
            localizationModule,
            firebaseModule,
            sentryModule,
            platformModule,
            awsModule,
            // Platform-specific Room database modules
            authPlatformModule,
            workspacePlatformModule,
            customerPlatformModule,
            productPlatformModule,
            businessPlatformModule,
            taxPlatformModule,
            unitPlatformModule,
            subscriptionPlatformModule,
            authModule(),
            workspaceModule(),
            eventModule(),
            formModule,
            customerModule,
            locationServiceModule,
            productModule(),
            businessModule(),
            taxModule,
            unitModule,
            subscriptionModule,
            updateModule,
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
expect val unitPlatformModule: Module
expect val subscriptionPlatformModule: Module
