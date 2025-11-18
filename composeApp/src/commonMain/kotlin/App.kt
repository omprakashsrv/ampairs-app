
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.ampairs.ui.theme.PlatformAmpairsTheme
import com.ampairs.common.theme.ThemeManager
import com.ampairs.common.localization.LocaleManager
import com.ampairs.common.localization.LocaleProvider
import org.koin.compose.koinInject

@Composable
fun App(
    onNavigationServiceReady: ((com.ampairs.workspace.navigation.DynamicModuleNavigationService?) -> Unit)? = null,
    onNavigationReady: (((String) -> Unit) -> Unit)? = null
) {
    val themeManager: ThemeManager = koinInject()
    val localeManager: LocaleManager = koinInject()
    val isDarkTheme = themeManager.isDarkTheme()

    LocaleProvider(localeManager) {
        PlatformAmpairsTheme(
            darkTheme = isDarkTheme
        ) {
            AppNavigation(onNavigationServiceReady, onNavigationReady)
        }
    }
}

expect fun getPlatformName(): String
