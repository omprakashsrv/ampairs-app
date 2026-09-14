package com.ampairs.common.aiops

import kotlinx.coroutines.flow.Flow

/**
 * Narrow read port for the AI Ops engine's per-workspace configuration — currently just the autonomy
 * level. The engine (`feature/aiops`) depends on this small contract rather than the full
 * `AppPreferencesDataStore` (80+ members), which keeps the runner testable with a one-method fake and
 * keeps the storage choice swappable (DataStore now; workspace settings later).
 */
interface AiOpsSettings {
    /** Current autonomy level, reactive. Defaults to [AiOpsAutonomyLevel.Default] when unset. */
    fun autonomyLevel(): Flow<AiOpsAutonomyLevel>
}
