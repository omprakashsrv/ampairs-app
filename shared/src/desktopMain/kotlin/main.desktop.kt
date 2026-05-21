import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.ampairs.di.DesktopAppGraph
import com.ampairs.update.ui.UpdateDialog
import kotlinx.coroutines.launch

actual fun getPlatformName(): String = "Desktop"

@Composable
fun MainView(
    appGraph: DesktopAppGraph,
    onLoggedIn: (Boolean) -> Unit,
    onNavigationServiceReady: ((com.ampairs.workspace.navigation.DynamicModuleNavigationService?) -> Unit)? = null,
    onNavigationReady: (((String) -> Unit) -> Unit)? = null
) {
    val updateChecker = appGraph.updateChecker
    val updateDownloader = appGraph.updateDownloader
    val updateInstaller = appGraph.updateInstaller
    val scope = rememberCoroutineScope()

    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateCheckResult by remember { mutableStateOf<com.ampairs.update.domain.UpdateCheckResult?>(null) }

    // Check for updates on app launch
    LaunchedEffect(Unit) {
        println("🚀 Desktop app launched - checking for updates...")
        scope.launch {
            try {
                val result = updateChecker.checkForUpdates(forceCheck = false)
                if (result != null && result.updateAvailable) {
                    val updateInfo = result.updateInfo
                    if (updateInfo != null) {
                        // Check if user has dismissed this version (only for optional updates)
                        val isDismissed = if (!updateInfo.isMandatory) {
                            updateChecker.isUpdateDismissed(updateInfo.version)
                        } else {
                            false
                        }

                        if (!isDismissed) {
                            println("📢 Update available: ${updateInfo.version} (Mandatory: ${updateInfo.isMandatory})")
                            updateCheckResult = result
                            showUpdateDialog = true
                        } else {
                            println("👤 User has dismissed update version: ${updateInfo.version}")
                        }
                    }
                }
            } catch (e: Exception) {
                println("❌ Update check failed: ${e.message}")
            }
        }
    }

    // Show update dialog if update is available
    if (showUpdateDialog && updateCheckResult?.updateInfo != null) {
        UpdateDialog(
            updateInfo = updateCheckResult!!.updateInfo!!,
            downloader = updateDownloader,
            installer = updateInstaller,
            onDismiss = {
                showUpdateDialog = false
                // Reset states
                updateDownloader.resetState()
                updateInstaller.resetState()
            },
            onDismissUpdate = {
                // User dismissed the update - mark it as dismissed
                scope.launch {
                    updateCheckResult?.updateInfo?.let { updateInfo ->
                        updateChecker.dismissUpdate(updateInfo.version)
                    }
                }
            }
        )
    }

    // Show main app
    App(appGraph, onNavigationServiceReady, onNavigationReady)
}
