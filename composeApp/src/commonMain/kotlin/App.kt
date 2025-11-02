
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.ampairs.ui.theme.PlatformAmpairsTheme
import com.ampairs.common.theme.ThemeManager
import org.koin.compose.koinInject

@Composable
fun App(
    onLoggedIn: (Boolean) -> Unit,
    onNavigationServiceReady: ((com.ampairs.workspace.navigation.DynamicModuleNavigationService?) -> Unit)? = null,
    onNavigationReady: (((String) -> Unit) -> Unit)? = null
) {
    val themeManager: ThemeManager = koinInject()
    val isDarkTheme = themeManager.isDarkTheme()

    PlatformAmpairsTheme(
        darkTheme = isDarkTheme
    ) {
        AppNavigation(onNavigationServiceReady, onNavigationReady)
    }
}

expect fun getPlatformName(): String
