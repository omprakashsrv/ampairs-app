import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ampairs.common.localization.LocaleManager
import com.ampairs.common.localization.LocaleProvider
import com.ampairs.common.theme.ThemeManager
import com.ampairs.ui.theme.PlatformAmpairsTheme
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
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars)
            ) {
                AppNavigation(onNavigationServiceReady, onNavigationReady)
            }
        }
    }
}

expect fun getPlatformName(): String
