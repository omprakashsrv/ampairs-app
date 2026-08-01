package com.ampairs.update.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.AppScope
import com.ampairs.update.domain.UpdateInfo
import com.ampairs.update.service.AppVersion
import com.ampairs.update.service.UpdateChecker
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Result of a user-initiated "Check for Updates" from the About screen.
 */
sealed interface UpdateCheckState {
    data object Idle : UpdateCheckState
    data object Checking : UpdateCheckState
    data object UpToDate : UpdateCheckState
    data class UpdateAvailable(val updateInfo: UpdateInfo) : UpdateCheckState
    data object Failed : UpdateCheckState
}

data class AboutUiState(
    val appVersion: String = AppVersion.VERSION_NAME,
    val versionCode: Int = AppVersion.VERSION_CODE,
    val checkState: UpdateCheckState = UpdateCheckState.Idle,
)

/**
 * Backs the About screen launched from the desktop system tray. Reuses [UpdateChecker]
 * (the same service that drives the on-launch update prompt) so the manual "Check for
 * Updates" flow and the automatic one share one code path.
 */
@ContributesIntoMap(AppScope::class)
@ViewModelKey
@Inject
class AboutViewModel(
    private val updateChecker: UpdateChecker,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AboutUiState())
    val uiState: StateFlow<AboutUiState> = _uiState.asStateFlow()

    fun checkForUpdates() {
        if (_uiState.value.checkState is UpdateCheckState.Checking) return
        viewModelScope.launch {
            _uiState.update { it.copy(checkState = UpdateCheckState.Checking) }
            // forceCheck = true bypasses the 4h rate limit so the user always gets a fresh answer.
            val result = updateChecker.checkForUpdates(forceCheck = true)
            val info = result?.updateInfo
            val next = when {
                result == null -> UpdateCheckState.Failed
                result.updateAvailable && info != null -> UpdateCheckState.UpdateAvailable(info)
                else -> UpdateCheckState.UpToDate
            }
            _uiState.update { it.copy(checkState = next) }
        }
    }

    /** Dismiss the update dialog / status and return the screen to idle. */
    fun dismissResult() {
        _uiState.update { it.copy(checkState = UpdateCheckState.Idle) }
    }
}
