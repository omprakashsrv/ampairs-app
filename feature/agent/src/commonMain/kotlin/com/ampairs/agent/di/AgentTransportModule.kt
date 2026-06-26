package com.ampairs.agent.di

import com.ampairs.agent.llm.transport.LlmTransport
import com.ampairs.common.di.WorkspaceScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Multibinds

/**
 * Declares the cloud-transport multibinding `Map<String, LlmTransport>` (keyed by transport id) so the
 * `CloudLlmBackend` resolves it even when no transport has contributed yet. `AmpairsProxyTransport`
 * adds the `"ampairs"` entry via `@ContributesIntoMap(WorkspaceScope) + @TransportKey`. WorkspaceScope
 * matches the module's other multibindings (`Set<LlmBackend>`, `Map<SyncEntity, SyncDelegate>`,
 * `Map<String, ModuleQuerySchema>`) and the `CloudLlmBackend` consumer.
 */
@ContributesTo(WorkspaceScope::class)
interface AgentTransportModule {
    @Multibinds(allowEmpty = true)
    fun llmTransports(): Map<String, LlmTransport>
}
