import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.ampairs.common.localization.LocaleProvider
import com.ampairs.common.localization.LocalLocaleManager
import com.ampairs.common.theme.LocalThemeManager
import com.ampairs.di.AppGraph
import com.ampairs.di.LocalAppGraph
import com.ampairs.ui.theme.PlatformAmpairsTheme
import com.ampairs.workspace.navigation.DynamicModuleNavigationService
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory

@Composable
fun App(
    appGraph: AppGraph,
    onNavigationServiceReady: ((DynamicModuleNavigationService?) -> Unit)? = null,
    onNavigationReady: (((String) -> Unit) -> Unit)? = null,
    onWorkspaceEntered: ((String) -> Unit)? = null,
    onWorkspaceLeft: (() -> Unit)? = null,
) {
    CompositionLocalProvider(
        LocalAppGraph provides appGraph,
        LocalMetroViewModelFactory provides appGraph.metroViewModelFactory,
        LocalThemeManager provides appGraph.themeManager,
        LocalLocaleManager provides appGraph.localeManager,
    ) {
        val isDarkTheme = LocalThemeManager.current.isDarkTheme()

        LocaleProvider(LocalLocaleManager.current) {
            PlatformAmpairsTheme(darkTheme = isDarkTheme) {
                // Surface (not Modifier.background) so LocalContentColor defaults to onSurface —
                // Text() without an explicit color would otherwise fall back to Compose's hardcoded
                // black, invisible against a dark surface in dark mode.
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.systemBars),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    AppNavigationNav3(onNavigationServiceReady, onNavigationReady, onWorkspaceEntered, onWorkspaceLeft)
                }
            }
        }
    }
}

expect fun getPlatformName(): String
