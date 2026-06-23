package com.ampairs.agent.di

import com.ampairs.agent.config.AssistantConfig
import com.ampairs.agent.llm.LlmBackend
import com.ampairs.common.di.WorkspaceScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Multibinds
import dev.zacsweers.metro.Provides

/**
 * Workspace-scope wiring for the on-device LLM tier (T023).
 *
 * - `Set<LlmBackend>` is declared `allowEmpty` so `ProviderRegistry` resolves before any engine
 *   adapter contributes — LiteRtLmEngine (T025) and LlamaCppEngine (T027) will each add a
 *   `@ContributesIntoSet(WorkspaceScope::class)` `LlmBackend`. Mirrors the SAFE_QUERY executor map.
 * - [AssistantConfig] is provided as [AssistantConfig.Default] for now; DataStore-backed persistence
 *   (engine/model picker, FR-011) lands with T032/T033.
 */
@ContributesTo(WorkspaceScope::class)
interface AgentLlmModule {

    @Multibinds(allowEmpty = true)
    fun llmBackends(): Set<LlmBackend>

    companion object {
        @Provides
        fun provideAssistantConfig(): AssistantConfig = AssistantConfig.Default
    }
}
