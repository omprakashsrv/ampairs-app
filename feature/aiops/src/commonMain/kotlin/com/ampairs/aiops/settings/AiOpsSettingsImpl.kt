package com.ampairs.aiops.settings

import com.ampairs.common.aiops.AiOpsAutonomyLevel
import com.ampairs.common.aiops.AiOpsSettings
import com.ampairs.common.config.AppPreferencesDataStore
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow

/**
 * Backs [AiOpsSettings] with the shared [AppPreferencesDataStore] (the app's single DataStore — never a
 * new instance). App-scoped: the autonomy level is one setting shared across workspaces for now, and an
 * `AppScope` binding is still visible to the `WorkspaceScope` child graph where the runner lives.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class AiOpsSettingsImpl(
    private val preferences: AppPreferencesDataStore,
) : AiOpsSettings {
    override fun autonomyLevel(): Flow<AiOpsAutonomyLevel> = preferences.getAiOpsAutonomyLevel()
}
