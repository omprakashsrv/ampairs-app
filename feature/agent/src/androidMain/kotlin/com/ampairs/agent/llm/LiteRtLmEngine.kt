package com.ampairs.agent.llm

import android.content.Context
import co.touchlab.kermit.Logger
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import kotlin.concurrent.Volatile

/**
 * Android [LlmEngine] adapter (T025) over Google AI Edge **LiteRT-LM**
 * (`com.google.ai.edge.litertlm`). It wraps one [Engine] (a loaded model) plus a single stateless
 * [Conversation], exposing the pipeline's engine-agnostic [LlmEngine] port so the resolvers never see
 * native types. Picked by [ProviderRegistry] through [LiteRtLmBackend] whenever the device RAM tier
 * resolves to a `litert-lm` Gemma model (FunctionGemma-270m / Gemma 3n E2B/E4B).
 *
 * **Model file.** Resolved at load from [modelDir]`/{model.fileName}`. Until the model manager (T032)
 * downloads it on demand, sideload the `.litertlm` file there. A missing file makes [load] throw, so
 * [ProviderRegistry.engineOrNull] catches it and the [com.ampairs.agent.offline.CompositeOfflineResolver]
 * stays on the rule-based path — the app never crashes for a missing model.
 *
 * **Threading.** `Engine.initialize()` (up to ~10 s) and `sendMessage()` are blocking native calls,
 * so both run on [Dispatchers.IO]. The acceleration backend is **GPU with an automatic CPU
 * fallback** ([initEngine]) for lower latency, dropping to CPU when GPU init fails on a device.
 *
 * **Stateless per call.** Each [runInference] creates a *fresh* [Conversation] and closes it after,
 * so the native side never accumulates turn history (the resolver already rebuilds the full prompt,
 * incl. recent turns, each call). A long-lived shared conversation grew context every message and
 * progressively bogged a 1B CPU model down to an apparent hang; a fresh one bounds the work to the
 * built prompt. A blocking [runInference] is also bounded by [INFERENCE_TIMEOUT_MS]: it runs in a
 * detached job we abandon on timeout so a slow/hung native call can't pin the chat on "Thinking…"
 * forever — the composite resolver then falls back to rule-based.
 *
 * **Constrained decoding.** Currently *prompt-guided*: [com.ampairs.agent.offline.LlmIntentResolver]
 * builds a JSON-instructed prompt (from [OutputSchema]) and validates the parsed action against the
 * registry (SC-003), so [generateConstrained] just generates and returns the raw text. Native
 * structured tool-calling (`ToolSet`/`@Tool`) / `ExperimentalFlags.enableConversationConstrainedDecoding`
 * for hard guarantees is a follow-up.
 */
class LiteRtLmEngine(
    private val context: Context,
    private val modelDir: File = File(context.filesDir, "agent_models"),
) : LlmEngine {

    @Volatile private var engine: Engine? = null
    @Volatile private var conversationConfig: ConversationConfig? = null

    // Detached scope for the blocking native inference so a timeout can abandon a stuck call.
    private val inferenceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override suspend fun load(model: ModelDescriptor, params: LlmParams) {
        if (engine != null) return
        val modelFile = File(modelDir, model.fileName)
        check(modelFile.exists()) { "LiteRT-LM model not found: ${modelFile.absolutePath}" }
        withContext(Dispatchers.IO) {
            engine = initEngine(modelFile)
            conversationConfig = ConversationConfig(
                samplerConfig = SamplerConfig(
                    topK = params.topK,
                    topP = params.topP.toDouble(),
                    temperature = params.temperature.toDouble(),
                ),
            )
        }
    }

    /**
     * Initialize on the **GPU** for lower latency, transparently falling back to **CPU** if GPU
     * init fails (driver/device incompatibility) — so the assistant still runs everywhere. A failed
     * GPU attempt surfaces in Logcat (tag [LOG_TAG]); switch the device and compare timings there.
     */
    private fun initEngine(modelFile: File): Engine {
        runCatching { buildEngine(modelFile, Backend.GPU()) }
            .onSuccess {
                Logger.i(tag = LOG_TAG) { "LiteRT-LM engine initialized on GPU (maxNumTokens=$MAX_NUM_TOKENS)" }
                return it
            }
            .onFailure { Logger.w(throwable = it, tag = LOG_TAG) { "GPU backend unavailable — falling back to CPU" } }
        return buildEngine(modelFile, Backend.CPU())
            .also { Logger.i(tag = LOG_TAG) { "LiteRT-LM engine initialized on CPU (maxNumTokens=$MAX_NUM_TOKENS)" } }
    }

    private fun buildEngine(modelFile: File, backend: Backend): Engine =
        Engine(
            EngineConfig(
                modelPath = modelFile.absolutePath,
                cacheDir = context.cacheDir.absolutePath,
                backend = backend,
                // Bound total context (prompt + generation). litertlm 0.13.1 has no per-response
                // output-token cap; this caps the KV-cache / total tokens — keep it above the built
                // prompt size so the intent JSON is never truncated.
                maxNumTokens = MAX_NUM_TOKENS,
            ),
        ).also { it.initialize() }

    override fun isLoaded(): Boolean = engine != null && conversationConfig != null

    override suspend fun generate(prompt: String, maxTokens: Int): String = runInference(prompt)

    override suspend fun generateConstrained(
        prompt: String,
        schema: OutputSchema,
        maxTokens: Int,
    ): String = runInference(prompt)

    private suspend fun runInference(prompt: String): String {
        val eng = engine ?: error("LiteRtLmEngine.generate() called before load()")
        val cfg = conversationConfig ?: error("LiteRtLmEngine.generate() called before load()")
        // Run the blocking native call in a detached job so withTimeoutOrNull can return control even
        // if sendMessage never does — a fresh, single-turn conversation keeps each call stateless.
        val deferred = inferenceScope.async {
            val started = System.currentTimeMillis()
            val convo = eng.createConversation(cfg)
            try {
                convo.sendMessage(prompt).toString().also {
                    Logger.i(tag = LOG_TAG) {
                        "Inference ok in ${System.currentTimeMillis() - started}ms (prompt=${prompt.length} chars, out=${it.length})"
                    }
                }
            } finally {
                runCatching { convo.close() }
            }
        }
        val result = withTimeoutOrNull(INFERENCE_TIMEOUT_MS) { deferred.await() }
        if (result == null) {
            deferred.cancel() // native thread finishes on its own; we stop waiting on it
            Logger.w(tag = LOG_TAG) { "Inference timed out after ${INFERENCE_TIMEOUT_MS / 1000}s — falling back" }
            throw IllegalStateException("On-device inference timed out after ${INFERENCE_TIMEOUT_MS / 1000}s")
        }
        return result
    }

    override suspend fun close() {
        runCatching { inferenceScope.cancel() }
        withContext(Dispatchers.IO) {
            runCatching { engine?.close() }
            conversationConfig = null
            engine = null
        }
    }

    private companion object {
        const val LOG_TAG = "AgentLlm"
        const val INFERENCE_TIMEOUT_MS = 60_000L
        // Total-token bound (prompt + generation). Above our built prompt so intent JSON isn't cut.
        const val MAX_NUM_TOKENS = 2048
    }
}
