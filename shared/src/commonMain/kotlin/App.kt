import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.ampairs.common.localization.LocaleProvider
import com.ampairs.di.LocalAppGraph
import com.ampairs.ui.theme.PlatformAmpairsTheme
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory

@Composable
fun App(
    appGraph: com.ampairs.di.AppGraph,
    onNavigationServiceReady: ((com.ampairs.workspace.navigation.DynamicModuleNavigationService?) -> Unit)? = null,
    onNavigationReady: (((String) -> Unit) -> Unit)? = null
) {
    CompositionLocalProvider(
        LocalAppGraph provides appGraph,
        LocalMetroViewModelFactory provides appGraph.metroViewModelFactory,
    ) {
        val themeManager = appGraph.themeManager
        val localeManager = appGraph.localeManager
        val isDarkTheme = themeManager.isDarkTheme()

        LocaleProvider(localeManager) {
            PlatformAmpairsTheme(darkTheme = isDarkTheme) {
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface)
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.systemBars)
                ) {
                    AppNavigationNav3(onNavigationServiceReady, onNavigationReady)
                }
            }
        }
    }
}

expect fun getPlatformName(): String
