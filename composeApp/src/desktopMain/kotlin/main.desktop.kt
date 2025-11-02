import androidx.compose.runtime.Composable

actual fun getPlatformName(): String = "Desktop"

@Composable
fun MainView(
    onLoggedIn: (Boolean) -> Unit,
    onNavigationServiceReady: ((com.ampairs.workspace.navigation.DynamicModuleNavigationService?) -> Unit)? = null,
    onNavigationReady: (((String) -> Unit) -> Unit)? = null
) = App(onLoggedIn, onNavigationServiceReady, onNavigationReady)