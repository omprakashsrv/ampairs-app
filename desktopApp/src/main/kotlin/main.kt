import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.isTraySupported
import androidx.compose.ui.window.rememberTrayState
import androidx.compose.ui.window.rememberWindowState
import com.ampairs.auth.deeplink.DeepLinkHandler
import com.ampairs.common.desktop.DataDirectoryManager
import com.ampairs.common.desktop.DataDirectoryPickerDialog
import com.ampairs.workspace.navigation.DynamicModuleNavigationService
import com.ampairs.workspace.navigation.DynamicModulesMenu
import coil3.compose.setSingletonImageLoaderFactory
import com.ampairs.common.sentry.SentryManager
import com.ampairs.di.DesktopAppGraph
import com.ampairs.di.DesktopWorkspaceModule
import com.ampairs.tallysync.TallySettingsScreen
import com.ampairs.tallysync.TallySyncScheduler
import dev.zacsweers.metro.createGraphFactory
import java.awt.Frame

fun main() = application {
    // Check if data directory is set before initializing the graph
    var showDataDirectoryPicker by remember { mutableStateOf(!DataDirectoryManager.isDataDirectorySet()) }
    var dataDirectoryReady by remember { mutableStateOf(DataDirectoryManager.isDataDirectorySet()) }

    // Show data directory picker if not set
    if (showDataDirectoryPicker) {
        DataDirectoryPickerDialog(
            onDirectorySelected = { directory ->
                println("Main: Data directory selected: ${directory.absolutePath}")
                showDataDirectoryPicker = false
                dataDirectoryReady = true
            },
            onDismiss = {
                // User must select a directory - don't allow dismissing
                println("Main: Data directory selection is required")
            }
        )
    }

    // Only proceed with app initialization if data directory is ready
    if (!dataDirectoryReady) {
        return@application
    }

    val appGraph = remember {
        initializeSentry()
        createGraphFactory<DesktopAppGraph.Factory>().create()
    }

    // Setup deep link handler for desktop authentication
    LaunchedEffect(Unit) {
        println("Main: Initializing deep link handler...")
        DeepLinkHandler.setupDeepLinkHandler()
        println("Main: Deep link handler initialized")
    }

    val applicationState = remember { ApplicationState() }
    applicationState.windows
    setSingletonImageLoaderFactory { _ -> appGraph.imageLoader }
    var activeWorkspaceSlug by remember { mutableStateOf("") }

    // The main window is hidden (minimized to the system tray) on close instead of
    // terminating the process, so background work (sync service, deep links) keeps running.
    // The app is only fully exited from the tray menu's "Exit" item.
    var mainWindowVisible by remember { mutableStateOf(true) }

    // Reference to the underlying AWT window so the tray actions can raise it to the
    // foreground. Just flipping `visible = true` shows the window behind the currently
    // focused app — it must be explicitly de-iconified, brought to front and focused.
    var mainComposeWindow by remember { mutableStateOf<ComposeWindow?>(null) }

    val showMainWindow = {
        mainWindowVisible = true
        mainComposeWindow?.let { window ->
            // Make it visible synchronously (don't wait for recomposition) so the
            // toFront/requestFocus below act on an already-shown, de-iconified window.
            window.extendedState = window.extendedState and Frame.ICONIFIED.inv()
            window.isVisible = true
            window.toFront()
            window.requestFocus()
            // Force the window manager to raise it above the previously focused app
            // (notably required on Windows, which otherwise blocks focus stealing).
            window.isAlwaysOnTop = true
            window.isAlwaysOnTop = false
        }
    }

    if (isTraySupported) {
        val trayState = rememberTrayState()
        Tray(
            icon = painterResource("tray_icon.png"),
            state = trayState,
            tooltip = "Ampairs",
            onAction = showMainWindow, // double-click restores the window
            menu = {
                Item("Open Ampairs", onClick = showMainWindow)
                Item("Exit", onClick = ::exitApplication)
            }
        )
    }

    for (window in applicationState.windows) {
        key(window) {
            if (window.title == "Main") {
                MainWindow(
                    state = window,
                    appGraph = appGraph,
                    visible = mainWindowVisible,
                    onWindowReady = { mainComposeWindow = it },
                    // Closing the main window only hides it to the tray when the tray is
                    // available; otherwise fall back to exiting so the user is never stuck.
                    onCloseRequest = {
                        if (isTraySupported) mainWindowVisible = false else exitApplication()
                    },
                    onWorkspaceSlugChanged = { activeWorkspaceSlug = it }
                )
            } else if (window.title == "Tally") {
                TallyWindow(window, appGraph, activeWorkspaceSlug)
            }
        }
    }
}


@Composable
private fun ApplicationScope.TallyWindow(
    state: AppWindowState,
    appGraph: DesktopAppGraph,
    workspaceSlug: String,
) = Window(
    onCloseRequest = state::close, title = state.title,
    onKeyEvent = { false }
) {
    val session by appGraph.workspaceManager.session.collectAsState()
    val scheduler = (session?.graph as? DesktopWorkspaceModule)?.tallySyncScheduler
    if (scheduler != null) {
        TallySettingsScreen(
            workspaceSlug = workspaceSlug,
            scheduler = scheduler,
            dataStore = appGraph.appPreferences,
        )
    }
}

@Composable
private fun ApplicationScope.MainWindow(
    state: AppWindowState,
    appGraph: DesktopAppGraph,
    visible: Boolean = true,
    onCloseRequest: () -> Unit = { exitApplication() },
    onWindowReady: (ComposeWindow) -> Unit = {},
    onWorkspaceSlugChanged: (String) -> Unit = {},
    onOpenTallyWindow: () -> Unit = state.openNewWindow,
) =
    Window(
        onCloseRequest = onCloseRequest,
        visible = visible,
        state = rememberWindowState(placement = WindowPlacement.Maximized),
        title = "Ampairs"
    ) {

        // Expose the underlying AWT window so the tray "Open" action can raise it to front.
        LaunchedEffect(window) { onWindowReady(window) }

        var loggedIn by remember { mutableStateOf(false) }

        // Get navigationService from the app content to ensure consistency
        var navigationService by remember { mutableStateOf<DynamicModuleNavigationService?>(null) }

        // Navigation callback that will be set up by the app
        var navigationCallback by remember { mutableStateOf<((String) -> Unit)?>(null) }


        MainView(
            appGraph = appGraph,
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
            },
            onWorkspaceEntered = { workspaceSlug ->
                onWorkspaceSlugChanged(workspaceSlug)
                // centralSyncService is started by WorkspaceManager.activateWorkspace()
                val scheduler = (appGraph.workspaceManager.session.value?.graph as? DesktopWorkspaceModule)?.tallySyncScheduler
                scheduler?.start(workspaceSlug)
            },
            onWorkspaceLeft = {
                onWorkspaceSlugChanged("")
                // centralSyncService is stopped by WorkspaceManager.clearSession()
                val scheduler = (appGraph.workspaceManager.session.value?.graph as? DesktopWorkspaceModule)?.tallySyncScheduler
                scheduler?.stop()
            }
        )

        MenuBar {
            Menu("Tools") {
                Item("Tally Settings", onClick = onOpenTallyWindow)
            }
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

private fun initializeSentry() {
    SentryManager.initialize(
        dsn = "https://dfb15e7a55f3454ba18b1b4c5ab03a46@o4510332999106560.ingest.de.sentry.io/4510333325148240",
        environment = "production",
        enableDebug = false
    )
}
