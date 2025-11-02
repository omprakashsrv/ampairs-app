import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.input.key.key
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
// import com.ampairs.tally.TallyApp
import com.ampairs.auth.deeplink.DeepLinkHandler
import com.ampairs.workspace.navigation.DynamicModuleNavigationService
import com.ampairs.workspace.navigation.DynamicModulesMenu
import coil3.compose.LocalPlatformContext
import coil3.compose.setSingletonImageLoaderFactory
import coil3.PlatformContext
import org.koin.compose.koinInject

import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.java.KoinJavaComponent.get

fun main() = application {
    if (GlobalContext.getOrNull() == null) {
        val koinApplication = startKoin {}
        initKoin(koinApplication)
    }

    // Setup deep link handler for desktop authentication
    LaunchedEffect(Unit) {
        println("Main: Initializing deep link handler...")
        DeepLinkHandler.setupDeepLinkHandler()
        println("Main: Deep link handler initialized")
    }

    val applicationState = remember { ApplicationState() }
    applicationState.windows
    setSingletonImageLoaderFactory { context ->
        generateImageLoader()
    }
    for (window in applicationState.windows) {
        key(window) {
            if (window.title == "Main") {
                MainWindow(window)
            } else if (window.title == "Tally") {
                TallyWindow(window)
            }
        }
    }
}


@Composable
private fun ApplicationScope.TallyWindow(
    state: AppWindowState,
) = Window(
    onCloseRequest = state::close, title = state.title,
    onKeyEvent = {
        false
    }) {
    // TallyApp()
}

@Composable
private fun ApplicationScope.MainWindow(state: AppWindowState) =
    Window(
        onCloseRequest = ::exitApplication,
        state = WindowState(placement = WindowPlacement.Maximized),
        title = "Ampairs"
    ) {

        var loggedIn by remember { mutableStateOf(false) }

        // Get navigationService from the app content to ensure consistency
        var navigationService by remember { mutableStateOf<DynamicModuleNavigationService?>(null) }

        // Navigation callback that will be set up by the app
        var navigationCallback by remember { mutableStateOf<((String) -> Unit)?>(null) }


        MainView(
            onLoggedIn = {
                println("MainWindow: onLoggedIn callback - changing from $loggedIn to $it")
                loggedIn = it
            },
            onNavigationServiceReady = {
                println("MainWindow: Callback received navigationService: ${if (it != null) "NOT NULL" else "NULL"}")
                navigationService = it
                if (it == null) {
                    println("MainWindow: NavigationService cleared - menus will be hidden")
                }
            },
            onNavigationReady = { callback ->
                println("MainWindow: Navigation callback ready")
                navigationCallback = callback
            }
        )

        MenuBar {
            // Dynamic Workspace Module Menus - only show if navigationService is available
            navigationService?.let { navService ->
                println("MenuBar: About to render DynamicModulesMenu")
                DynamicModulesMenu(
                    navigationService = navService,
                    onNavigate = { route ->
                        println("MenuBar: onNavigate to $route")
                        navigationCallback?.invoke(route)
                    }
                )
            } ?: println("MenuBar: navigationService is null, not rendering DynamicModulesMenu")
        }
    }

private class ApplicationState {
    val windows = mutableStateListOf<AppWindowState>()

    init {
        windows += AppWindowState(title = "Main")
    }

    fun openNewWindow() {
        windows += AppWindowState(title = "Tally")
    }

    fun exit() {
        windows.clear()
    }

    private fun AppWindowState(
        title: String,
    ) = AppWindowState(
        title,
        openNewWindow = ::openNewWindow,
        exit = ::exit,
        windows::remove
    )
}

private class AppWindowState(
    val title: String,
    val openNewWindow: () -> Unit,
    val exit: () -> Unit,
    private val close: (AppWindowState) -> Unit,
) {
    fun close() = close(this)
}