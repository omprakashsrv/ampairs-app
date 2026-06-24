package com.ampairs.agent.llm

import co.touchlab.kermit.Logger
import com.ampairs.agent.config.AssistantConfig
import com.ampairs.common.config.AppPreferencesDataStore
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.workspace.WorkspaceClosableRegistry
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.concurrent.Volatile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
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
    private val appPreferences: AppPreferencesDataStore,
) {
    private val mutex = Mutex()
    private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var engine: LlmEngine? = null

    @Volatile
    private var closableRegistered = false

    /**
     * The chat model the device should run; null → rule-based-only. Resolution order:
     * 1. The user's explicit choice from the model manager ([AppPreferencesDataStore.getSelectedLlmModelId]),
     *    if it's still in the catalog — an explicit pick overrides the RAM gate (the user opted in).
     * 2. Otherwise, the RAM-gated auto-pick: among CHAT models whose footprint fits this device's RAM,
     *    the server-flagged `recommended` one, else the largest that fits.
     */
    suspend fun selectedChatModel(): ModelDescriptor? {
        if (!config.llmEnabled) return null
        val all = modelCatalog.all()
        appPreferences.getSelectedLlmModelId().first()?.let { chosenId ->
            all.firstOrNull { it.id == chosenId }?.let { return it }
        }
        val ram = DeviceCapability.totalRamBytes()
        if (!RamTiers.canRunLlm(ram)) return null
        val candidates = all.filter { it.role == ModelRole.CHAT && it.estimatedPeakMemoryBytes <= ram }
        return candidates.filter { it.recommended }.maxByOrNull { it.estimatedPeakMemoryBytes }
            ?: candidates.maxByOrNull { it.estimatedPeakMemoryBytes }
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
            val backend = BackendSelector.select(backends, model, enginePreference()) ?: run {
                Logger.w(tag = LOG_TAG) { "No backend supports model '${model.id}' (${model.backendId})" }
                return null
            }
            val created = backend.create()
            val loadResult = runCatching { created.load(model) }
            if (loadResult.isFailure) {
                // Surface why the engine didn't come up (missing/incompatible model file, native init
                // failure, OOM) — otherwise the assistant silently stays rule-based (actions only).
                Logger.e(throwable = loadResult.exceptionOrNull(), tag = LOG_TAG) {
                    "Failed to load on-device model '${model.id}' (${model.fileName}, ${model.backendId})"
                }
                cleanupScope.launch { runCatching { created.close() } }
                return null
            }
            Logger.i(tag = LOG_TAG) { "On-device LLM ready: ${model.id} (${model.fileName})" }
            registerCloseableOnce()
            engine = created
            created
        }
    }

    /** True only when an engine is created and reports loaded — drives `CompositeOfflineResolver`. */
    suspend fun isLlmReady(): Boolean = engineOrNull()?.isLoaded() == true

    /**
     * True when this platform has at least one LLM engine backend registered. Today only Android
     * contributes one (LiteRT-LM); Desktop/iOS have none until a llama.cpp backend lands, so the
     * model manager uses this to show "Android only" instead of offering un-loadable downloads.
     */
    fun hasLlmBackend(): Boolean = backends.isNotEmpty()

    /**
     * Drop the cached engine (closing it) so the next [engineOrNull] re-resolves [selectedChatModel]
     * and loads afresh. Called when the user switches/deletes the active model in the model manager.
     */
    suspend fun reload() {
        val toClose = mutex.withLock {
            val current = engine
            engine = null
            current
        }
        toClose?.let { runCatching { it.close() } }
    }

    private fun registerCloseableOnce() {
        if (closableRegistered) return
        closableRegistered = true
        closableRegistry.register {
            val toClose = engine
            engine = null
            if (toClose != null) cleanupScope.launch { runCatching { toClose.close() } }
        }
    }

    private companion object {
        const val LOG_TAG = "AgentLlm"
    }
}
