package com.ampairs.agent.llm

import android.content.Context
import co.touchlab.kermit.Logger
import com.ampairs.agent.core.AgentStreamEvent
import com.ampairs.common.agent.ActionResult
import com.ampairs.common.agent.AgentAction
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.ExperimentalFlags
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.MessageCallback
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.tool
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
 * so both run on [Dispatchers.IO]. The acceleration backend is **GPU only** ([initEngine]) — there is
 * deliberately no CPU fallback (CPU latency is unusably slow); a GPU init failure surfaces in logs and
 * the assistant stays rule-based rather than silently running on CPU.
 *
 * **Streaming + stateless per call.** [generateStream] streams text deltas from `sendMessageAsync`;
 * [generate]/[generateConstrained] collect that stream into a full string (bounded by
 * [INFERENCE_TIMEOUT_MS]). Each call uses a *fresh* single-turn [Conversation] (closed after), so the
 * native side never accumulates turn history (the resolver rebuilds the full prompt each time) — a
 * long-lived shared conversation grew context every message and bogged a 1B CPU model to an apparent
 * hang. On timeout/cancellation `awaitClose` calls `cancelProcess()` to actually stop native
 * generation, so a slow/hung call can't pin the chat on "Thinking…" and the composite resolver falls
 * back to rule-based.
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
    @Volatile private var samplerConfig: SamplerConfig? = null

    // Persistent tool-calling chat session. The conversation is created once and reused across turns
    // so LiteRT-LM keeps its KV cache (system prompt + tool schemas + prior turns) warm instead of
    // re-prefilling that whole block every message — the dominant first-token cost. Reset on
    // clearChat()/close() or after a cancelled turn (whose KV is left mid-decode).
    @Volatile private var conversation: Conversation? = null
    // Per-turn routing for the *persistent* tool set: each turn points these at its own callbackFlow
    // before sending, so a tool invoked during that turn reaches the right collector.
    @Volatile private var currentEmit: ((AgentStreamEvent) -> Unit)? = null
    @Volatile private var currentDispatch: (suspend (AgentAction) -> ActionResult)? = null

    // One tool set, bound to the persistent conversation, routing through the per-turn indirection
    // above (a fresh tool set per turn would mean rebuilding the conversation — defeating KV reuse).
    private val toolSet: AmpairsAgentToolSet by lazy {
        AmpairsAgentToolSet(
            dispatch = { action -> currentDispatch?.invoke(action) ?: ActionResult.Error("No active chat turn") },
            emit = { event -> currentEmit?.invoke(event) },
        )
    }

    override suspend fun load(model: ModelDescriptor, params: LlmParams) {
        if (engine != null) return
        val modelFile = File(modelDir, model.fileName)
        check(modelFile.exists()) { "LiteRT-LM model not found: ${modelFile.absolutePath}" }
        withContext(Dispatchers.IO) {
            engine = initEngine(modelFile)
            samplerConfig = SamplerConfig(
                topK = params.topK,
                topP = params.topP.toDouble(),
                temperature = params.temperature.toDouble(),
            )
        }
    }

    /**
     * Initialize the engine on the **GPU only** — no CPU fallback (product decision: CPU is far too
     * slow for usable on-device latency). `visionBackend`/`audioBackend` are explicitly null for our
     * text model (matching the Gallery recipe); leaving them at EngineConfig defaults made GPU init
     * fail on a text-only model, which is what previously forced the slow CPU path. If GPU init throws
     * it propagates to [load] → [ProviderRegistry] logs it and stays rule-based — never silent CPU.
     */
    private fun initEngine(modelFile: File): Engine =
        Engine(
            EngineConfig(
                modelPath = modelFile.absolutePath,
                cacheDir = context.cacheDir.absolutePath,
                backend = Backend.GPU(),
                visionBackend = null, // text model — must be null or GPU init fails
                audioBackend = null,
                // Bound total context (prompt + generation). litertlm 0.13.1 has no per-response
                // output-token cap; this caps the KV-cache / total tokens — keep it above the built
                // prompt size so the intent JSON is never truncated.
                maxNumTokens = MAX_NUM_TOKENS,
            ),
        ).also {
            it.initialize()
            Logger.i(tag = LOG_TAG) { "LiteRT-LM engine initialized on GPU (maxNumTokens=$MAX_NUM_TOKENS)" }
        }

    override fun isLoaded(): Boolean = engine != null && samplerConfig != null

    override val supportsToolCalling: Boolean = true

    override suspend fun generate(prompt: String, maxTokens: Int): String = collectFull(prompt)

    override suspend fun generateConstrained(
        prompt: String,
        schema: OutputSchema,
        maxTokens: Int,
    ): String = collectFull(prompt)

    /**
     * Stream a turn token-by-token (Gallery-style): emits assistant text **deltas** from LiteRT-LM's
     * `sendMessageAsync` as they're generated. Each call uses a **fresh, single-turn** [Conversation]
     * (stateless — the resolver rebuilds the full prompt each time). On cancellation (e.g. the
     * collector times out) [awaitClose] calls `cancelProcess()` to actually stop native generation
     * and closes the conversation, instead of leaking a stuck native call.
     */
    fun generateStream(prompt: String): Flow<String> = callbackFlow {
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

    /**
     * Gallery-style native tool-calling chat over a **persistent** [Conversation]: the conversation is
     * created once (system instruction + [AmpairsAgentToolSet] + constrained decoding, seeding context
     * from [recentHistory]) and reused across turns, so LiteRT-LM keeps its KV cache warm instead of
     * re-prefilling the system prompt + tool schemas + history every message. Each turn streams the
     * model's prose as [AgentStreamEvent.TextDelta] and lets the runtime invoke tools — which dispatch
     * via [dispatch] and push [AgentStreamEvent.ActionProposed]/[ActionExecuted].
     */
    override fun chatStream(
        userMessage: String,
        recentHistory: List<String>,
        dispatch: suspend (AgentAction) -> ActionResult,
    ): Flow<AgentStreamEvent> = callbackFlow {
        val convo = conversationFor(recentHistory) // created once, reused → KV cache persists across turns
        val started = System.currentTimeMillis()
        var completed = false
        // Point the persistent tool set at THIS turn's collector. Capture our own emit so awaitClose
        // only clears the binding if a later turn hasn't already replaced it (turns are serialized,
        // but cancellation teardown can interleave).
        val myEmit: (AgentStreamEvent) -> Unit = { trySend(it) }
        currentEmit = myEmit
        currentDispatch = dispatch
        convo.sendMessageAsync(
            Contents.of(listOf(Content.Text(userMessage))),
            object : MessageCallback {
                override fun onMessage(message: Message) {
                    val thought = message.channels["thought"]
                    if (!thought.isNullOrBlank()) trySend(AgentStreamEvent.ThoughtDelta(thought))
                    val text = message.toString()
                    if (text.isNotEmpty()) trySend(AgentStreamEvent.TextDelta(text))
                }
                override fun onDone() {
                    completed = true
                    Logger.i(tag = LOG_TAG) { "Chat stream done in ${System.currentTimeMillis() - started}ms (KV reused: ${conversation === convo})" }
                    trySend(AgentStreamEvent.Done)
                    close()
                }
                override fun onError(throwable: Throwable) {
                    trySend(AgentStreamEvent.Failed(throwable.message ?: "inference error"))
                    close()
                }
            },
            emptyMap(),
        )
        awaitClose {
            if (currentEmit === myEmit) {
                currentEmit = null
                currentDispatch = null
            }
            // Keep the conversation alive on a clean finish so its warm KV cache is reused next turn.
            // A cancelled/errored turn leaves the KV mid-decode, so drop the conversation — the next
            // turn rebuilds it (conversationFor also guards on isAlive).
            if (!completed) {
                runCatching { convo.cancelProcess() }
                if (conversation === convo) {
                    runCatching { convo.close() }
                    conversation = null
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * The retained tool-calling [Conversation], created on first use (and after a reset). Built with
     * constrained decoding (better tool-call reliability) and the persistent [toolSet]; [recentHistory]
     * seeds context only at creation — subsequent turns rely on the live KV cache.
     */
    @OptIn(ExperimentalApi::class)
    private fun conversationFor(recentHistory: List<String>): Conversation {
        conversation?.let { if (it.isAlive) return it else runCatching { it.close() } }
        val eng = engine ?: throw IllegalStateException("LiteRtLmEngine.chatStream() called before load()")
        val sampler = samplerConfig ?: throw IllegalStateException("LiteRtLmEngine.chatStream() called before load()")
        // Constrained decoding improves tool-call reliability; flip it only around conversation setup.
        ExperimentalFlags.enableConversationConstrainedDecoding = true
        val convo = eng.createConversation(
            ConversationConfig(
                samplerConfig = sampler,
                systemInstruction = Contents.of(listOf(Content.Text(systemInstruction(recentHistory)))),
                tools = listOf(tool(toolSet)),
            ),
        )
        ExperimentalFlags.enableConversationConstrainedDecoding = false
        conversation = convo
        return convo
    }

    /** Drop the retained chat conversation so the next turn starts a fresh session (KV cleared). */
    override fun resetChat() {
        val convo = conversation ?: return
        conversation = null
        currentEmit = null
        currentDispatch = null
        runCatching { convo.cancelProcess() }
        runCatching { convo.close() }
    }

    /** System prompt for the tool-calling chat: role, tool guidance, and short recent context. */
    private fun systemInstruction(recentHistory: List<String>): String = buildString {
        append("You are the assistant for the Ampairs business management app. ")
        append("Use the provided tools to perform actions (add cart items, set the customer, create an invoice or order, search customers/products, count). ")
        append("Build a bill/order by adding items to the cart, then create the invoice or order. ")
        append("For anything else, answer helpfully and concisely in plain language. Do not output JSON.")
        if (recentHistory.isNotEmpty()) {
            append("\n\nRecent conversation:\n")
            recentHistory.forEach { append(it).append('\n') }
        }
    }

    /** Collect the whole streamed reply into one string, bounded by [INFERENCE_TIMEOUT_MS]. */
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
            conversation?.let { convo ->
                runCatching { convo.cancelProcess() }
                runCatching { convo.close() }
            }
            conversation = null
            currentEmit = null
            currentDispatch = null
            runCatching { engine?.close() }
            samplerConfig = null
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
