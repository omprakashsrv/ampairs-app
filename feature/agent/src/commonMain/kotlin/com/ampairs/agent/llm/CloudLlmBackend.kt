package com.ampairs.agent.llm

import com.ampairs.agent.llm.transport.LlmTransport
import com.ampairs.common.di.WorkspaceScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject

/**
 * Cloud [LlmBackend] (T-online) — contributed in **commonMain**, so unlike the native LiteRT-LM
 * backend it is available on every platform, including iOS/Desktop which have no on-device engine.
 * It joins the `Set<LlmBackend>` multibinding; [BackendSelector]/[ProviderRegistry] pick it for any
 * model whose [ModelDescriptor.backendId] is `"cloud"` and whose [ModelDescriptor.transportId] has a
 * registered [LlmTransport].
 *
 * [create] hands the transport map to a fresh [CloudLlmEngine]; [ProviderRegistry] owns its lifecycle
 * (one cached instance per workspace graph, closed on model/workspace switch or background unload).
 */
@Inject
@ContributesIntoSet(WorkspaceScope::class)
class CloudLlmBackend(
    private val transports: Map<String, LlmTransport>,
) : LlmBackend {

    override val id: String = ModelDescriptor.BACKEND_CLOUD

    override fun supports(model: ModelDescriptor): Boolean =
        model.isCloud && model.transportId != null && transports.containsKey(model.transportId)

    override fun create(): LlmEngine = CloudLlmEngine(transports)
}
