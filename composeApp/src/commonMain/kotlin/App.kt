import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.ampairs.common.localization.LocaleManager
import com.ampairs.common.localization.LocaleProvider
import com.ampairs.common.theme.ThemeManager
import com.ampairs.ui.theme.PlatformAmpairsTheme
import org.koin.compose.koinInject

/**
 * Feature flag for Navigation 3 migration.
 * Set to true to use Navigation 3, false to use Navigation 2.
 *
 * Navigation 3 introduces:
 * - User-owned back stack (SnapshotStateList)
 * - NavDisplay instead of NavHost
 * - Entry providers instead of NavGraphBuilder.composable
 * - Direct route access (no toRoute<T>() needed)
 */
private const val USE_NAVIGATION_3 = false

@Composable
fun App(
    onNavigationServiceReady: ((com.ampairs.workspace.navigation.DynamicModuleNavigationService?) -> Unit)? = null,
    onNavigationReady: (((String) -> Unit) -> Unit)? = null
) {
    val themeManager: ThemeManager = koinInject()
    val localeManager: LocaleManager = koinInject()
    val isDarkTheme = themeManager.isDarkTheme()

    // Feature flag can be toggled for testing
    val useNav3 = remember { USE_NAVIGATION_3 }

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
                if (useNav3) {
                    // Navigation 3 implementation
                    AppNavigationNav3(onNavigationServiceReady, onNavigationReady)
                } else {
                    // Navigation 2 implementation (current)
                    AppNavigation(onNavigationServiceReady, onNavigationReady)
                }
            }
        }
    }
}

expect fun getPlatformName(): String
