package com.ampairs.agent.llm

import co.touchlab.kermit.Logger
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.MessageCallback
import com.google.ai.edge.litertlm.SamplerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import kotlin.concurrent.Volatile

/**
 * Desktop (JVM) [LlmEngine] adapter over Google AI Edge **LiteRT-LM** — the unified Kotlin API's JVM
 * build (`com.google.ai.edge.litertlm:litertlm-jvm`, same 0.13.1 release as the Android AAR). Mirrors
 * the Android [com.ampairs.agent.llm.LiteRtLmEngine] but with no `Context`: the model file is resolved
 * from [ModelStorage] (the same `agent_models` directory the model manager downloads into), and the
 * engine cache lives under `~/.ampairs/litertlm_cache`.
 *
 * **GPU only.** [initEngine] forces `Backend.GPU()` with `vision`/`audioBackend = null` (text model),
 * matching Android — no CPU fallback (CPU latency is unusable). A GPU init failure propagates to
 * [load] → [ProviderRegistry] logs it and stays rule-based, never silently CPU. Desktop GPU runs on
 * WebGPU/Dawn (Linux/Windows) or Metal (macOS); those accelerator plugins ship as separate prebuilt
 * native libs that must be discoverable on the JVM native path at runtime (the bundled `libLiteRt`
 * is loaded by the artifact itself).
 *
 * **No native tool-calling.** Unlike Android, this adapter leaves [supportsToolCalling] = false and
 * does not implement `chatStream` — the chat layer uses the resolver pipeline ([generateConstrained]).
 * The LiteRT-LM `ToolSet`/`@Tool` path can be added later once validated on desktop.
 */
class LiteRtLmEngine(
    private val storage: ModelStorage,
) : LlmEngine {

    @Volatile private var engine: Engine? = null
    @Volatile private var samplerConfig: SamplerConfig? = null

    private val cacheDir: File =
        File(System.getProperty("user.home"), ".ampairs/litertlm_cache").apply { mkdirs() }

    override suspend fun load(model: ModelDescriptor, params: LlmParams) {
        if (engine != null) return
        val modelFile = File(storage.modelsDirectoryPath(), model.fileName)
        check(modelFile.exists()) { "LiteRT-LM model not found: ${modelFile.absolutePath}" }
        val contextTokens = params.contextTokens.coerceAtLeast(MIN_CONTEXT_TOKENS)
        withContext(Dispatchers.IO) {
            engine = initEngine(modelFile, contextTokens)
            samplerConfig = SamplerConfig(
                topK = params.topK,
                topP = params.topP.toDouble(),
                temperature = params.temperature.toDouble(),
            )
        }
    }

    /** GPU-only init (no CPU fallback); `vision`/`audioBackend` null for the text model — see Android. */
    private fun initEngine(modelFile: File, contextTokens: Int): Engine =
        Engine(
            EngineConfig(
                modelPath = modelFile.absolutePath,
                cacheDir = cacheDir.absolutePath,
                backend = Backend.GPU(),
                visionBackend = null,
                audioBackend = null,
                // Per-model context budget (LlmParams.contextTokens), floored — see Android.
                maxNumTokens = contextTokens,
            ),
        ).also {
            it.initialize()
            Logger.i(tag = LOG_TAG) { "LiteRT-LM (desktop) engine initialized on GPU (maxNumTokens=$contextTokens)" }
        }

    override fun isLoaded(): Boolean = engine != null && samplerConfig != null

    override val supportsToolCalling: Boolean = false

    override suspend fun generate(prompt: String, maxTokens: Int): String = collectFull(prompt)

    override suspend fun generateConstrained(
        prompt: String,
        schema: OutputSchema,
        maxTokens: Int,
    ): String = collectFull(prompt)

    /** Stream text deltas from a fresh single-turn conversation (stateless per call), like Android. */
    private fun generateStream(prompt: String): Flow<String> = callbackFlow {
        val eng = engine ?: throw IllegalStateException("LiteRtLmEngine.generate() called before load()")
        val sampler = samplerConfig ?: throw IllegalStateException("LiteRtLmEngine.generate() called before load()")
        val started = System.currentTimeMillis()
        val convo = eng.createConversation(ConversationConfig(samplerConfig = sampler))
        convo.sendMessageAsync(
            Contents.of(listOf(Content.Text(prompt))),
            object : MessageCallback {
                override fun onMessage(message: Message) { trySend(message.toString()) }
                override fun onDone() {
                    Logger.i(tag = LOG_TAG) { "Inference stream done in ${System.currentTimeMillis() - started}ms" }
                    close()
                }
                override fun onError(throwable: Throwable) { close(throwable) }
            },
            emptyMap(),
        )
        awaitClose {
            runCatching { convo.cancelProcess() }
            runCatching { convo.close() }
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun collectFull(prompt: String): String {
        val sb = StringBuilder()
        val finished = withTimeoutOrNull(INFERENCE_TIMEOUT_MS) {
            generateStream(prompt).collect { sb.append(it) }
            true
        }
        if (finished == null) {
            Logger.w(tag = LOG_TAG) { "Inference timed out after ${INFERENCE_TIMEOUT_MS / 1000}s — falling back" }
            throw IllegalStateException("On-device inference timed out after ${INFERENCE_TIMEOUT_MS / 1000}s")
        }
        return sb.toString()
    }

    override suspend fun close() {
        withContext(Dispatchers.IO) {
            runCatching { engine?.close() }
            samplerConfig = null
            engine = null
        }
    }

    private companion object {
        const val LOG_TAG = "AgentLlm"
        const val INFERENCE_TIMEOUT_MS = 60_000L
        // Floor for the per-model context budget (LlmParams.contextTokens) — see Android.
        const val MIN_CONTEXT_TOKENS = 2048
    }
}
