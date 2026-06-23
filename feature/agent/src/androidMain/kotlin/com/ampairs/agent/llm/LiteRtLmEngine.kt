package com.ampairs.agent.llm

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
 * so both run on [Dispatchers.IO].
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
    @Volatile private var conversation: Conversation? = null

    override suspend fun load(model: ModelDescriptor, params: LlmParams) {
        if (engine != null) return
        val modelFile = File(modelDir, model.fileName)
        check(modelFile.exists()) { "LiteRT-LM model not found: ${modelFile.absolutePath}" }
        withContext(Dispatchers.IO) {
            val eng = Engine(
                EngineConfig(
                    modelPath = modelFile.absolutePath,
                    backend = Backend.CPU(), // CPU is the safe cold-start default (Gallery note)
                    cacheDir = context.cacheDir.absolutePath,
                ),
            )
            eng.initialize()
            val convo = eng.createConversation(
                ConversationConfig(
                    samplerConfig = SamplerConfig(
                        topK = params.topK,
                        topP = params.topP.toDouble(),
                        temperature = params.temperature.toDouble(),
                    ),
                ),
            )
            engine = eng
            conversation = convo
        }
    }

    override fun isLoaded(): Boolean = engine != null && conversation != null

    override suspend fun generate(prompt: String, maxTokens: Int): String = runInference(prompt)

    override suspend fun generateConstrained(
        prompt: String,
        schema: OutputSchema,
        maxTokens: Int,
    ): String = runInference(prompt)

    private suspend fun runInference(prompt: String): String {
        val convo = conversation ?: error("LiteRtLmEngine.generate() called before load()")
        return withContext(Dispatchers.IO) { convo.sendMessage(prompt).toString() }
    }

    override suspend fun close() {
        withContext(Dispatchers.IO) {
            runCatching { conversation?.close() }
            runCatching { engine?.close() }
            conversation = null
            engine = null
        }
    }
}
