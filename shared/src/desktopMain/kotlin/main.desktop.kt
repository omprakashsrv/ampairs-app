import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.ampairs.di.DesktopAppGraph
import com.ampairs.update.domain.UpdateCheckResult
import com.ampairs.update.ui.UpdateDialog
import kotlinx.coroutines.launch

actual fun getPlatformName(): String = "Desktop"

@Composable
fun MainView(
    appGraph: DesktopAppGraph,
    onNavigationServiceReady: ((com.ampairs.workspace.navigation.DynamicModuleNavigationService?) -> Unit)? = null,
    onNavigationReady: (((String) -> Unit) -> Unit)? = null,
    onWorkspaceEntered: ((String) -> Unit)? = null,
    onWorkspaceLeft: (() -> Unit)? = null,
) {
    val updateChecker = appGraph.updateChecker
    val scope = rememberCoroutineScope()

    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateCheckResult by remember { mutableStateOf<UpdateCheckResult?>(null) }

    LaunchedEffect(Unit) {
        try {
            val result = updateChecker.checkForUpdates(forceCheck = false)
            val updateInfo = result?.updateInfo
            if (result != null && result.updateAvailable && updateInfo != null) {
                val isDismissed = if (!updateInfo.isMandatory) {
                    updateChecker.isUpdateDismissed(updateInfo.version)
                } else {
                    false
                }
                if (!isDismissed) {
                    updateCheckResult = result
                    showUpdateDialog = true
                }
            }
        } catch (_: Exception) {}
    }

    if (showUpdateDialog && updateCheckResult?.updateInfo != null) {
        UpdateDialog(
            updateInfo = updateCheckResult!!.updateInfo!!,
            onDismiss = { showUpdateDialog = false },
            onDismissUpdate = {
                updateCheckResult?.updateInfo?.let { info ->
                    scope.launch { updateChecker.dismissUpdate(info.version) }
                }
            }
        )
    }

    App(appGraph, onNavigationServiceReady, onNavigationReady, onWorkspaceEntered, onWorkspaceLeft)
}
