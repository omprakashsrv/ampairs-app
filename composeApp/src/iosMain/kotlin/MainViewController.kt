import androidx.compose.ui.window.ComposeUIViewController
import coil3.compose.setSingletonImageLoaderFactory
import cocoapods.FirebaseCore.FIRApp
import com.ampairs.common.sentry.SentryManager
import initKoin
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.context.startKoin

@OptIn(ExperimentalForeignApi::class)
fun MainViewController() = ComposeUIViewController {
    // Initialize Firebase for iOS
    if (FIRApp.defaultApp() == null) {
        FIRApp.configure()
    }

    // Initialize Sentry early for error tracking (before Koin to capture initialization errors)
    initializeSentry()

    // Initialize Koin for iOS
    if (org.koin.mp.KoinPlatform.getKoinOrNull() == null) {
        val koinApplication = startKoin { }
        initKoin(koinApplication)
    }

    // Initialize Coil ImageLoader
    setSingletonImageLoaderFactory { context ->
        generateImageLoader()
    }

    App({})
}

/**
 * Initialize Sentry error tracking for iOS.
 * Uses the shared SentryManager from commonMain.
 */
private fun initializeSentry() {
    SentryManager.initialize(
        dsn = "https://dfb15e7a55f3454ba18b1b4c5ab03a46@o4510332999106560.ingest.de.sentry.io/4510333325148240",
        environment = "production",
        enableDebug = false
    )
}