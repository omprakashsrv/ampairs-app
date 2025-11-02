import androidx.compose.ui.window.ComposeUIViewController
import coil3.compose.setSingletonImageLoaderFactory
import cocoapods.FirebaseCore.FIRApp
import initKoin
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.context.startKoin

@OptIn(ExperimentalForeignApi::class)
fun MainViewController() = ComposeUIViewController {
    // Initialize Firebase for iOS
    if (FIRApp.defaultApp() == null) {
        FIRApp.configure()
    }

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