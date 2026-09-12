package com.ampairs.aiops.di

import com.ampairs.common.aiops.AiOpsCapability
import com.ampairs.common.aiops.AiOpsExecutor
import com.ampairs.common.di.WorkspaceScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Multibinds

/**
 * Workspace-scope multibindings for the AI Ops engine. Both `allowEmpty` so the graph resolves before
 * any feature contributes a capability/executor (mirrors the SAFE_QUERY `SafeQueryExecutorModule`).
 *
 * Each owning feature contributes its capability (`@ContributesIntoMap(WorkspaceScope::class)` +
 * `@CapabilityKey`) and its executor (`+ @AiOpsExecutorKey`), keyed by the capability key.
 */
@ContributesTo(WorkspaceScope::class)
interface AiOpsMultibindingModule {
    @Multibinds(allowEmpty = true)
    fun capabilities(): Map<String, AiOpsCapability>

    @Multibinds(allowEmpty = true)
    fun executors(): Map<String, AiOpsExecutor>
}
