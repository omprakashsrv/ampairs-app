# Agent + LiteRT-LM — Reference Recipe (from Google AI Edge Gallery)

Concrete integration notes distilled from the open-source **Google AI Edge Gallery**
(`github.com/google-ai-edge/gallery`, **Apache-2.0** — copy-adaptable with attribution). Studied at
commit on `main` (2026-06). This is the implementation blueprint for our on-device LLM + agentic
actions; pair it with `spec.md` / `plan.md`.

## TL;DR — it validates our architecture
The Gallery's "Mobile Actions" custom task does exactly what our agent does, and our existing types
already line up 1:1:

| Gallery | Ampairs (ours) | Status |
|---|---|---|
| `Action` / `FunctionCallDetails(functionName, parameters)` | `AgentAction(actionType, moduleName, params)` | ✅ exists |
| `ToolSet` with `@Tool`/`@ToolParam` methods | per-module `ActionHandler` + `ActionDescriptor` metadata | ✅ exists |
| `onFunctionCalled(Action)` → `performAction(...)` | `ActionRegistry.dispatch(AgentAction)` → `ActionHandler.execute` | ✅ exists (Phase 0) |
| `LlmChatModelHelper` (Engine + Conversation) | `LlmEngine` port → `LiteRtLmEngine` adapter | ⛔ to build (native, needs device) |
| `ModelAllowlist` + `DownloadRepository` (HF) | `ModelCatalog` + `ModelManager` | ⛔ to build |

## LiteRT-LM Kotlin API (the exact surface to wrap)
Package `com.google.ai.edge.litertlm`. Reference: `ui/llmchat/LlmChatModelHelper.kt`.

```kotlin
// 1. Engine — one per loaded model
val engine = Engine(
  EngineConfig(
    modelPath = "/…/gemma.task",         // or .litertlm
    backend = Backend.CPU() | Backend.GPU() | Backend.NPU(nativeLibraryDir),
    visionBackend = Backend.GPU() | null, // Gemma 3n vision = GPU
    audioBackend = Backend.CPU() | null,  // Gemma 3n audio = CPU
    maxNumTokens = 4096,
    cacheDir = …,
  )
)
engine.initialize()

// 2. Conversation = a session, with system prompt + tools
val conversation = engine.createConversation(
  ConversationConfig(
    samplerConfig = SamplerConfig(topK, topP, temperature),
    systemInstruction = Contents.of(listOf(Content.Text("…"))),
    tools = listOf(tool(MyToolSet(onFunctionCalled = { … }))),  // List<ToolProvider>
  )
)

// 3. Send a turn; tool calls are dispatched by the runtime, text streams back
conversation.sendMessageAsync(
  Contents.of(listOf(Content.Text(userInput))),  // also Content.ImageBytes / Content.AudioBytes
  object : MessageCallback {
    override fun onMessage(m: Message) { /* m.toString(); m.channels["thought"] */ }
    override fun onDone() {}
    override fun onError(t: Throwable) {}
  },
  /* extraContext */ emptyMap(),
)

// Capability probe + flags
Capabilities(modelPath).use { it.hasSpeculativeDecodingSupport() }
ExperimentalFlags.enableSpeculativeDecoding = true
ExperimentalFlags.enableConversationConstrainedDecoding = true   // structured-output reliability
```

## Tool definition pattern (our `ActionHandler` ⇒ a `ToolSet`)
Reference: `customtasks/mobileactions/{MobileActionsTools,Actions,MobileActionsTask}.kt`.

```kotlin
class AmpairsAgentToolSet(val dispatch: suspend (AgentAction) -> ActionResult) : ToolSet {
  @Tool(description = "Create a draft invoice/bill for a customer")
  fun createInvoice(
    @ToolParam(description = "Customer name to bill") customer: String,
    @ToolParam(description = "Product name for a line item") product: String = "",
    @ToolParam(description = "Quantity") quantity: String = "",
  ): Map<String, String> {
    val res = runBlockingDispatch(AgentAction(ActionType.CREATE, "invoice",
      mapOf("customer" to customer, "product" to product, "quantity" to quantity)))
    return mapOf("result" to res)   // confirmation string fed back to the model
  }
  // … searchCustomers, countInvoices, lowStockItems, etc. — one @Tool per supported action
}
```

- `@Tool` methods are **statically annotated**; enumerate our known actions (CRUD per module) in one
  hand-authored `ToolSet`, each mapping to an `AgentAction` and delegating to `ActionRegistry.dispatch`.
  This is the LiteRT-LM equivalent of the GBNF grammar — **native structured tool-calling**, so on
  LiteRT-LM we do *not* need a hand-rolled grammar (only the llama.cpp adapter does).
- `getSystemPrompt()` injects dynamic context (current date/time) — we'll inject business
  locale/timezone + the active workspace's installed modules.

## Models (allowlist-driven)
Reference: `model_allowlist.json` + `data/ModelAllowlist.kt`, `data/DownloadRepository.kt`.
Each entry: `name, modelId (HF), modelFile (.task/.litertlm), sizeInBytes, estimatedPeakMemoryInBytes,
defaultConfig{topK,topP,temperature,maxTokens,accelerators}, taskTypes, llmSupportImage`.

- **Tool-calling / intent → action:** **FunctionGemma-270m** (Gallery's Mobile Actions model) — tiny,
  fast, device-control finetune. New default for our intent resolver (was 1–3B).
- **Conversational answers / queries:** **Gemma 3n E2B / E4B** (`.task`/`.litertlm`, ~3 GB E2B).
- **llama.cpp fallback (Desktop/parity):** Qwen2.5-3B GGUF.
- `estimatedPeakMemoryInBytes` drives our `DeviceCapability` RAM gating; `accelerators` ("cpu,gpu")
  drives backend selection. Gallery note: **CPU often beats GPU on cold start**.

## Voice
Reference: `ui/common/textandvoiceinput/{HoldToDictate,VoiceRecognizerOverlay}.kt`. Two viable paths:
1. Platform/recognizer dictation (HoldToDictate UX) → text → pipeline.
2. **LiteRT-LM audio input directly**: `Content.AudioBytes(...)` with a Gemma 3n audio model
   ("Audio Scribe") — the engine transcribes, no separate STT model. This is a *simpler* alternative
   to whisper.cpp for STT and is the recommended primary on LiteRT-LM.

## Concrete refinements applied to plan/tasks
1. `LlmEngine` port maps to `Engine`+`Conversation`; `LiteRtLmEngine` (Android/JVM Kotlin API; iOS
   Swift pkg) is the primary adapter.
2. Tool-calling = native `ToolSet`/`@Tool` (+ `enableConversationConstrainedDecoding`); GBNF only on
   llama.cpp. The "OutputSchema" port narrows to: build a `ToolSet` (LiteRT-LM) or a grammar (llama).
3. Default models updated: FunctionGemma-270m (actions) + Gemma 3n E2B/E4B (chat); Qwen2.5-3B fallback.
4. `ModelManager`/`ModelCatalog` mirror `ModelAllowlist` (allowlist JSON + HF download + `Capabilities`
   probe + peak-memory gating + accelerator pick).
5. STT primary = LiteRT-LM audio input (`Content.AudioBytes`); platform recognizer as a lighter option.
6. Speculative decoding (`Capabilities` + flag) as a perf option.
