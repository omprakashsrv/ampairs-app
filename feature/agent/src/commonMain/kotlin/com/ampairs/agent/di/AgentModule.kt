package com.ampairs.agent.di

import com.ampairs.agent.core.IntentResolver
import com.ampairs.agent.offline.RuleBasedIntentResolver
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.Qualifier

// Replaced Koin agentModule. Injectable classes are annotated with @Inject directly:
// - ActionRegistry: @Inject
// - RuleBasedIntentResolver: @Inject
// - AgentOrchestrator: @Inject
// - ChatViewModel: @Inject
//
// Note: The original Koin module used named("offline"/"online") qualifiers.
// IntentResolver bindings are provided here via @Provides with qualifier annotations.

@Qualifier
annotation class OfflineIntentResolver

@Qualifier
annotation class OnlineIntentResolver

@ContributesTo(AppScope::class)
interface AgentModule {
    companion object {
        // @OfflineIntentResolver is bound in WorkspaceScope (AgentLlmModule) — it composes the
        // on-device LlmIntentResolver (via ProviderRegistry) with the rule-based fallback (T030).
        @Provides
        @OnlineIntentResolver
        fun provideOnlineIntentResolver(): IntentResolver = RuleBasedIntentResolver()
    }
}

fun agentModule() = Unit
