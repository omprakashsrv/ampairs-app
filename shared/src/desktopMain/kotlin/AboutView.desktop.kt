import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.ampairs.common.localization.LocaleProvider
import com.ampairs.common.localization.LocalLocaleManager
import com.ampairs.common.theme.LocalThemeManager
import com.ampairs.di.DesktopAppGraph
import com.ampairs.update.ui.AboutScreen
import com.ampairs.ui.theme.PlatformAmpairsTheme
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory

/**
 * Standalone About window content for the desktop app, launched from the system tray.
 * Wires the same theme, locale and Metro ViewModel factory that [App] provides, plus a
 * dedicated [ViewModelStoreOwner] (the main app gets one from the Nav3 store decorators;
 * a standalone window has none) so [AboutScreen]'s ViewModel and the reused UpdateDialog
 * resolve correctly.
 */
@Composable
fun AboutView(appGraph: DesktopAppGraph) {
    val viewModelStoreOwner = remember {
        object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
    }
    DisposableEffect(viewModelStoreOwner) {
        onDispose { viewModelStoreOwner.viewModelStore.clear() }
    }

    CompositionLocalProvider(
        LocalMetroViewModelFactory provides appGraph.metroViewModelFactory,
        LocalViewModelStoreOwner provides viewModelStoreOwner,
        LocalThemeManager provides appGraph.themeManager,
        LocalLocaleManager provides appGraph.localeManager,
    ) {
        val isDarkTheme = LocalThemeManager.current.isDarkTheme()
        LocaleProvider(LocalLocaleManager.current) {
            PlatformAmpairsTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    AboutScreen(appName = "Ampairs")
                }
            }
        }
    }
}
