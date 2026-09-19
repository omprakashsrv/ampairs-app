package com.ampairs.aiops.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.aiops.AiOpsAutonomyLevel
import com.ampairs.common.aiops.AiOpsSettings
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The autonomy levels a user may pick in the app UI. Only the app-tier levels are here — L3/L4
 * (`AUTO_EXECUTE`/`AUTONOMOUS`) are backend-managed and must never be user-selectable on device.
 */
val AiOpsSelectableLevels: List<AiOpsAutonomyLevel> = listOf(
    AiOpsAutonomyLevel.OBSERVE,
    AiOpsAutonomyLevel.RECOMMEND,
    AiOpsAutonomyLevel.AUTO_CORRECT,
)

/**
 * Backs the AI Ops autonomy-level setting screen. App-scoped because the autonomy level is a single
 * app-wide preference for now (see [AiOpsSettings] / `AiOpsSettingsImpl`). Reads the current level
 * reactively and persists the user's choice; the running engine observes the same [AiOpsSettings], so
 * a change takes effect on the next save with no restart.
 */
@ContributesIntoMap(AppScope::class)
@ViewModelKey
@Inject
class AiOpsSettingsViewModel(
    private val settings: AiOpsSettings,
) : ViewModel() {

    /** Only the app-tier levels are user-selectable; L3/L4 are backend-managed. */
    val selectableLevels: List<AiOpsAutonomyLevel> = AiOpsSelectableLevels

    val level: StateFlow<AiOpsAutonomyLevel> =
        settings.autonomyLevel()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AiOpsAutonomyLevel.Default)

    fun setLevel(level: AiOpsAutonomyLevel) {
        viewModelScope.launch { settings.setAutonomyLevel(level) }
    }
}
