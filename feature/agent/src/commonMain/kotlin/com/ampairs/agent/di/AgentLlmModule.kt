package com.ampairs.agent.di

import com.ampairs.agent.config.AssistantConfig
import com.ampairs.agent.core.ActionRegistry
import com.ampairs.agent.core.IntentResolver
import com.ampairs.agent.llm.LlmBackend
import com.ampairs.agent.llm.ProviderRegistry
import com.ampairs.agent.offline.CompositeOfflineResolver
import com.ampairs.agent.offline.LlmIntentResolver
import com.ampairs.agent.offline.RuleBasedIntentResolver
import com.ampairs.common.agent.ModuleQuerySchema
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

        /**
         * The `@OfflineIntentResolver` (T030): on-device [LlmIntentResolver] when the engine is ready
         * (engine resolved lazily via [ProviderRegistry.engineOrNull]), otherwise the rule-based
         * fallback. Bound in WorkspaceScope because [ProviderRegistry] + [ActionRegistry] are
         * workspace-scoped. Until an engine adapter contributes (T025/T027) the registry yields no
         * engine, so this transparently behaves as rule-based.
         */
        @Provides
        @OfflineIntentResolver
        fun provideOfflineIntentResolver(
            providerRegistry: ProviderRegistry,
            actionRegistry: ActionRegistry,
            ruleBased: RuleBasedIntentResolver,
            querySchemas: Map<String, ModuleQuerySchema>,
        ): IntentResolver {
            val llm = LlmIntentResolver(
                actionRegistry = actionRegistry,
                engineProvider = { providerRegistry.engineOrNull() },
                querySchemas = querySchemas,
            )
            return CompositeOfflineResolver(
                llm = llm,
                rule = ruleBased,
                isLlmReady = { providerRegistry.isLlmReady() },
                isLlmSelected = { providerRegistry.selectedChatModel() != null },
            )
        }
    }
}
