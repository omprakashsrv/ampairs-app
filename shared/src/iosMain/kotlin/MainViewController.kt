import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import coil3.compose.setSingletonImageLoaderFactory
import cocoapods.FirebaseCore.FIRApp
import com.ampairs.FcmBridge
import com.ampairs.common.sentry.SentryManager
import com.ampairs.di.IosAppGraph
import com.ampairs.logging.initAppLogging
import dev.zacsweers.metro.createGraphFactory
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
fun MainViewController() = ComposeUIViewController {
    if (FIRApp.defaultApp() == null) {
        FIRApp.configure()
    }

    initAppLogging()
    initializeSentry()

    val appGraph = remember { createGraphFactory<IosAppGraph.Factory>().create() }

    // Publish the shared FCM singleton so AppDelegate (Swift) can forward token/notification events.
    remember(appGraph) { FcmBridge.register(appGraph.firebaseMessaging); appGraph }

    setSingletonImageLoaderFactory { _ -> appGraph.imageLoader }

    App(appGraph, {})
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
