package com.ampairs.agent.llm

import com.ampairs.common.di.WorkspaceScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject

/**
 * Desktop LiteRT-LM engine registration — contributes a [LlmBackend] into the `Set<LlmBackend>`
 * multibinding so [BackendSelector]/[ProviderRegistry] can select it for any model whose
 * [ModelDescriptor.backendId] is `"litert-lm"` (e.g. the catalog's Gemma 3n entries). Unlike Android
 * there is no SDK gate; if GPU init fails at [LiteRtLmEngine.load] the registry stays rule-based.
 *
 * [create] builds a fresh [LiteRtLmEngine] from [ModelStorage] (the shared `agent_models` dir);
 * [ProviderRegistry] owns its lifecycle (one cached instance per workspace graph).
 */
@Inject
@ContributesIntoSet(WorkspaceScope::class)
class LiteRtLmBackend(private val storage: ModelStorage) : LlmBackend {
    override val id: String = "litert-lm"

    override fun supports(model: ModelDescriptor): Boolean = model.backendId == "litert-lm"

    override fun create(): LlmEngine = LiteRtLmEngine(storage)
}
