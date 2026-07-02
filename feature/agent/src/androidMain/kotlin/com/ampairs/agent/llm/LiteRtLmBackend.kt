package com.ampairs.agent.llm

import android.content.Context
import android.os.Build
import com.ampairs.common.di.WorkspaceScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject

/**
 * LiteRT-LM engine registration (T025). Contributes a [LlmBackend] into the `Set<LlmBackend>`
 * multibinding (declared `@Multibinds(allowEmpty = true)` in `AgentLlmModule`) so
 * [BackendSelector]/[ProviderRegistry] can select it for any model whose
 * [ModelDescriptor.backendId] is `"litert-lm"`. Android-only — on iOS/Desktop no `litert-lm` backend
 * is contributed, so the selector falls through to llama.cpp (T027) or rule-based mode.
 *
 * **API gate.** LiteRT-LM requires Android 12 (API 31); the app's floor is API 24. [supports]
 * therefore additionally requires `SDK_INT >= 31`, so on Android < 12 no `litert-lm` backend matches
 * and `ProviderRegistry` stays on the rule-based resolver — the native engine is never instantiated
 * (the AAR ships unused there; the manifest merge is allowed via `tools:overrideLibrary`).
 *
 * [create] returns a fresh [LiteRtLmEngine]; [ProviderRegistry] owns its lifecycle (one cached
 * instance per workspace graph, closed via [com.ampairs.common.workspace.WorkspaceClosableRegistry]).
 */
@Inject
@ContributesIntoSet(WorkspaceScope::class)
class LiteRtLmBackend(private val context: Context) : LlmBackend {
    override val id: String = "litert-lm"

    override fun supports(model: ModelDescriptor): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && model.backendId == "litert-lm"

    override fun create(): LlmEngine = LiteRtLmEngine(context)
}
