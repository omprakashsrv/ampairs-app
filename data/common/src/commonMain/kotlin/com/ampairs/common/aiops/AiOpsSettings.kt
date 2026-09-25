package com.ampairs.common.aiops

import kotlinx.coroutines.flow.Flow

/**
 * Narrow read/write port for the AI Ops engine's configuration — currently just the autonomy level.
 * The engine (`feature/aiops`) and its settings UI depend on this small contract rather than the full
 * `AppPreferencesDataStore` (80+ members), which keeps them testable with a two-method fake and keeps
 * the storage choice swappable (DataStore now; workspace settings later). The runner only reads
 * [autonomyLevel]; the settings screen also writes via [setAutonomyLevel].
 */
interface AiOpsSettings {
    /** Current autonomy level, reactive. Defaults to [AiOpsAutonomyLevel.Default] when unset. */
    fun autonomyLevel(): Flow<AiOpsAutonomyLevel>

    /** Persist the chosen autonomy level. Reflected by [autonomyLevel] on the next emission. */
    suspend fun setAutonomyLevel(level: AiOpsAutonomyLevel)
}
