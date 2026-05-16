package com.ampairs.agent.di

import com.ampairs.agent.core.ActionHandlerProvider
import com.ampairs.agent.core.ActionRegistry
import com.ampairs.agent.core.AgentOrchestrator
import com.ampairs.agent.core.IntentResolver
import com.ampairs.agent.offline.RuleBasedIntentResolver
import com.ampairs.agent.ui.ChatViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

val agentModule = module {
    // Collect all ActionHandlerProviders and build registry
    factory {
        ActionRegistry().apply {
            getAll<ActionHandlerProvider>().forEach { register(it) }
        }
    }

    // Offline intent resolver (rule-based)
    factory<IntentResolver>(named("offline")) { RuleBasedIntentResolver() }

    // TODO: Phase 2 - Add LlmIntentResolver(named("online"))
    // For now, use offline resolver for both slots
    factory<IntentResolver>(named("online")) { RuleBasedIntentResolver() }

    // Agent Orchestrator
    factory {
        AgentOrchestrator(
            actionRegistry = get(),
            onlineResolver = get(named("online")),
            offlineResolver = get(named("offline")),
        )
    }

    // Chat ViewModel
    viewModelOf(::ChatViewModel)
}
