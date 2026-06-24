package com.ampairs.agent.llm

import com.ampairs.agent.config.AssistantConfig
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.workspace.WorkspaceClosableRegistry
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.concurrent.Volatile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The single seam the pipeline asks for an [LlmEngine] (T023, FR-011/FR-012). It selects a backend
 * from the Metro multibinding `Set<LlmBackend>` — currently empty (`@Multibinds(allowEmpty = true)`,
 * see `AgentLlmModule`) until the LiteRT-LM (T025) / llama.cpp (T027) adapters contribute — and
 * lazily creates + loads the engine on first use, behind a [Mutex] so concurrent resolvers share one
 * instance.
 *
 * Model choice is **RAM-gated for safety**: [DeviceCapability.totalRamBytes] feeds [RamTiers], so a
 * sub-3 GB device (or `llmEnabled = false`) yields a null engine and the [CompositeOfflineResolver]
 * stays on the rule-based path. Engine order of preference is the [AssistantConfig.engineId] override,
 * then [PlatformDefaults] primary, then fallback.
 *
 * Lifecycle: the lazily-created engine is registered with [WorkspaceClosableRegistry] so it is closed
 * when the workspace graph is torn down. Because `WorkspaceClosable.close()` is non-suspend but
 * [LlmEngine.close] is suspend, cleanup is launched fire-and-forget on an internal scope — enough to
 * release native model memory on a workspace switch.
 *
 * [engineOrNull] / [isLlmReady] are exposed as suspend functions so they can be passed as the
 * `engineProvider` / `isLlmReady` lambdas to [com.ampairs.agent.offline.LlmIntentResolver] (T029) and
 * [CompositeOfflineResolver] (T030) without leaking DI into those pure resolvers.
 */
@Inject
@SingleIn(WorkspaceScope::class)
class ProviderRegistry(
    private val backends: Set<LlmBackend>,
    private val config: AssistantConfig,
    private val closableRegistry: WorkspaceClosableRegistry,
    private val modelCatalog: ModelCatalogProvider,
) {
    private val mutex = Mutex()
    private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var engine: LlmEngine? = null

    @Volatile
    private var closableRegistered = false

    /**
     * The chat model the device may run, gated by RAM; null → rule-based-only. Resolved from the
     * server-seeded catalog ([ModelCatalogProvider]) — the RAM tier picks a preferred id, and we fall
     * back to the largest CHAT model whose memory footprint fits if that exact id isn't seeded.
     */
    suspend fun selectedChatModel(): ModelDescriptor? {
        if (!config.llmEnabled) return null
        val ram = DeviceCapability.totalRamBytes()
        val recommendedId = RamTiers.recommendedChatModelId(ram) ?: return null
        val catalog = modelCatalog.all()
        return catalog.firstOrNull { it.id == recommendedId }
            ?: catalog.filter { it.role == ModelRole.CHAT && it.estimatedPeakMemoryBytes <= ram }
                .maxByOrNull { it.estimatedPeakMemoryBytes }
    }

    /** Engine ids in preference order: config override → platform primary → platform fallback. */
    private fun enginePreference(): List<String> =
        listOfNotNull(config.engineId, PlatformDefaults.primaryEngineId, PlatformDefaults.fallbackEngineId)
            .distinct()

    /**
     * Returns the loaded engine, creating + loading it on first use. Null when the LLM tier is off,
     * the device can't run a model, no backend is registered/supports the model, or load fails.
     */
    suspend fun engineOrNull(): LlmEngine? {
        engine?.let { return it }
        val model = selectedChatModel() ?: return null
        return mutex.withLock {
            engine?.let { return it }
            val backend = BackendSelector.select(backends, model, enginePreference()) ?: return null
            val created = backend.create()
            val loaded = runCatching { created.load(model) }.isSuccess
            if (!loaded) {
                cleanupScope.launch { runCatching { created.close() } }
                return null
            }
            registerCloseableOnce()
            engine = created
            created
        }
    }

    /** True only when an engine is created and reports loaded — drives `CompositeOfflineResolver`. */
    suspend fun isLlmReady(): Boolean = engineOrNull()?.isLoaded() == true

    private fun registerCloseableOnce() {
        if (closableRegistered) return
        closableRegistered = true
        closableRegistry.register {
            val toClose = engine
            engine = null
            if (toClose != null) cleanupScope.launch { runCatching { toClose.close() } }
        }
    }
}
