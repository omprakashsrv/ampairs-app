# Implementation Plan: Offline AI Assistant (Text & Voice Agentic Chat)

**Branch**: `claude/nice-gauss-0ndtbn` · **Spec**: `./spec.md` · **Tasks**: `./tasks.md`

This plan captures the HOW. It builds on the existing scaffold in `feature/agent/` and
`data/common/.../agent/`, follows the project's Metro DI (`/metro-di`), offline-sync (`/offline-sync`),
and KMP/CMP rules (`/cmp-practices`), and keeps all feature code in feature modules (never `shared/`).

---

## 1. Architecture Overview

```
                          ┌──────────────────────────── feature/agent ───────────────────────────┐
 mic ─▶ SpeechToText ─▶   │ ChatViewModel ─▶ AgentOrchestrator                                     │
 (expect/actual)          │                    ├─ OnlineIntentResolver  (cloud LLM, optional)      │
 text ────────────────▶   │                    ├─ OfflineIntentResolver ── LlmIntentResolver ──┐   │
                          │                    │                          └─ RuleBasedResolver │   │
 spk ◀─ TextToSpeech ◀─   │                    └─ ActionRegistry.dispatch(AgentAction)         │   │
 (expect/actual)          └─────────────────────────────────┬──────────────────────────────┘ │   │
                                                             ▼                                  │
                       ActionHandler per module (customer/product/order/invoice/inventory) ◀──┘
                                                             │
                                   Repository (local-only) ─▶ Room  (synced=false) ─▶ CentralSyncService push
                                                             ▲
                       LlmEngine (pluggable: MediaPipe / llama.cpp)  │  ModelManager (download + capability gating)
```

Everything swappable sits behind a **stable `commonMain` port** (interface). Each platform contributes
one or more **adapters**, and a **`ProviderRegistry`** picks the active adapter from `PlatformDefaults`
(best-per-platform) overridable by a persisted **`AssistantConfig`**. New ports: **LlmEngine**,
**SpeechToText**, **TextToSpeech**. New services: **ProviderRegistry**, **ModelManager** (catalog-driven
download + selection). Plus the registry wiring and the invoice CREATE handler. This is what makes the
pipeline adaptable — see §4.0.

---

## 2. Technology Choices — best-per-platform, behind a stable abstraction

Guiding principle: **ports & adapters**. The pipeline depends only on small `commonMain` interfaces
(ports); each platform plugs in the best available engine (adapter). "Best" is **configuration, not
hardcoding** — a `ProviderRegistry` + persisted `AssistantConfig` choose the active adapter and model at
runtime, so swapping a model, engine, or flow later is a config/registry change, not a rewrite (§4.0).

### LLM engine — best per platform (all behind one `LlmEngine` port)

| Platform | Primary engine | Why |
|---|---|---|
| **Android** | **LiteRT-LM** (Google AI Edge) | First-class Google runtime; OpenCL/GPU accel (~52 tok/s); **native Gemma 4 function calling**; `.litertlm` models. |
| **iOS** | **LiteRT-LM Swift API** | Google's **new native iOS Swift API** with Metal GPU (~56 tok/s); runs Gemma 4 offline with the same native function calling. |
| **Desktop (JVM)** | **LiteRT-LM Kotlin/JVM API** (CPU/GPU); **llama.cpp** (GGUF) fallback | LiteRT-LM's Kotlin API officially targets **Android *and* JVM**, so Desktop can share the same engine + models; llama.cpp stays as a portable fallback. |

All engines implement the **same `LlmEngine` port** (load / constrained-generate / generate / close), so
the rest of the pipeline is platform-identical. A device can override its engine via config (e.g. force
llama.cpp on Android for parity testing).

> **LiteRT-LM is effectively a single engine across our targets.** Google ships a **Kotlin API for
> Android + JVM**, a **Swift package for iOS + macOS**, plus Web/Flutter/Python/CLI — so one runtime can
> back Android, iOS, *and* Desktop. Its runtime-level **function calling** (pause → structured tool-call
> → resume, from FunctionGemma/Gemma 4) maps almost 1:1 onto our `AgentAction` dispatch. The ports
> abstraction means we treat LiteRT-LM as the **primary adapter on all three platforms** and keep
> **llama.cpp** as an alternate/fallback adapter — both swappable by config, never hardcoded.
>
> Verify before locking in: the LiteRT-LM **Kotlin/JVM** native libs cover Linux/macOS/Windows desktop,
> and the **iOS Swift package** is reachable from the Kotlin/Native iosMain layer (bridge through the
> `iosApp` Swift target if needed). Until confirmed on Desktop, llama.cpp is the Desktop default.

### Tool-calling — native `ToolSet` on LiteRT-LM, grammar on llama.cpp

Validated against the **Google AI Edge Gallery** "Mobile Actions" task (Apache-2.0). Concrete recipe +
file citations: **`docs/features/AGENT_LITERTLM_REFERENCE.md`**.

- **LiteRT-LM (Android/iOS/Desktop):** use the native function-calling API — `com.google.ai.edge.litertlm`
  `ToolSet` with `@Tool`/`@ToolParam` methods, registered via `ConversationConfig(tools = listOf(tool(...)))`.
  Each `@Tool` maps to an `AgentAction` and delegates to `ActionRegistry.dispatch` (their
  `onFunctionCalled` → `performAction` is our `dispatch` → `ActionHandler.execute`). Optionally set
  `ExperimentalFlags.enableConversationConstrainedDecoding` for stricter structured output.
- **llama.cpp (fallback):** GBNF grammar to the `AgentAction` JSON schema.
- Both are produced from the same `ActionRegistry` metadata; the resolver validates and re-asks on
  failure → 100% structurally-valid actions (SC-003).

### Models — adaptable catalog, not a hardcoded choice

Split by **role** (the Gallery uses a tiny tool-caller + a larger chat model — we mirror that):

| Role | Model | ~size | Notes |
|---|---|---|---|
| **Intent → action (tool-calling)** | **FunctionGemma-270m** | ~few hundred MB | The Gallery's "Mobile Actions" model — device-control / function-call finetune. Tiny + fast, runs on low-end phones. Default for the intent resolver. |
| **Conversational answers / queries** | **Gemma 3n E2B / E4B** (`.task`/`.litertlm`) | ~2–3 GB | For phrasing query results & chat (Gallery ships these in `model_allowlist.json`; E4B when RAM allows). Gemma 4 E-series when available on LiteRT-LM. |
| **llama.cpp fallback (Desktop/parity)** | **Qwen2.5-3B-Instruct** (GGUF) | ~2 GB | Robust llama.cpp support; default for the llama.cpp adapter / when LiteRT-LM is unavailable. |

Models live in a **`ModelCatalog`** mirroring the Gallery's `model_allowlist.json` (name, `modelId`
[Hugging Face], `modelFile`, `sizeInBytes`, `estimatedPeakMemoryInBytes`,
`defaultConfig{topK,topP,temperature,maxTokens,accelerators}`, capabilities). `ModelManager` downloads
from HF and probes `Capabilities(modelPath)`; `estimatedPeakMemoryInBytes` drives `DeviceCapability`
RAM gating and `accelerators` drives backend selection (`Backend.CPU/GPU/NPU`; Gallery note: **CPU
often beats GPU on cold start**). Adding/changing a model = a catalog entry — no resolver/handler/UI change.

### Speech & delivery (same port pattern)

| Concern | Android | iOS | Desktop | Port |
|---|---|---|---|---|
| STT (primary) | **LiteRT-LM audio input** (`Content.AudioBytes`, Gemma 3n audio) — engine transcribes, no separate STT model | same | same | (via `LlmEngine`) |
| STT (lighter alt) | `SpeechRecognizer` (offline) | `SFSpeechRecognizer` (on-device) | whisper.cpp | `SpeechToText` |
| TTS | `TextToSpeech` | `AVSpeechSynthesizer` | system / Piper | `TextToSpeech` |

> STT refinement from the Gallery: it transcribes via the model's **audio input** ("Audio Scribe",
> `Content.AudioBytes`) and a hold-to-dictate UX (`ui/common/textandvoiceinput/HoldToDictate`). On
> LiteRT-LM we can feed audio straight to the engine — simpler than a separate whisper.cpp model —
> with the platform recognizer as a lighter fallback.

Model delivery: on-demand download to app-private dir, Wi-Fi gated, RAM-gated selection (FR-011/012).
Online path: `OnlineIntentResolver` slot for a cloud LLM, tried first when connected (FR-014).

`expect`/`actual` note: per project KMP rules, the iOS actual for IO dispatch uses `Dispatchers.Default`,
not `Dispatchers.IO`.

---

## 3. Module & File Layout

Reuse `feature/agent`; add platform source sets and a small set of new files. New cross-cutting
contracts that other modules need go in `data/common`.

```
feature/agent/src/
├── commonMain/.../agent/
│   ├── core/AgentOrchestrator.kt                 (exists)
│   ├── core/ActionRegistry.kt                    (exists; now actually populated)
│   ├── offline/RuleBasedIntentResolver.kt        (exists; stays as fallback)
│   ├── offline/LlmIntentResolver.kt              ★ new — uses ProviderRegistry.llmEngine + OutputSchema
│   ├── offline/AgentSchemaBuilder.kt             ★ new — ActionDescriptors → OutputSchema (GBNF + JSON-schema)
│   ├── llm/LlmEngine.kt                          ★ new — port interface (load/generateConstrained/generate/close)
│   ├── llm/LlmBackend.kt                         ★ new — adapter descriptor (id/supports/create)
│   ├── llm/ProviderRegistry.kt                   ★ new — picks adapter via PlatformDefaults + AssistantConfig
│   ├── llm/PlatformDefaults.kt                   ★ new — expect/actual: best engine id per platform
│   ├── config/AssistantConfig.kt                 ★ new — runtime overrides (DataStore-backed)
│   ├── speech/SpeechToText.kt + TextToSpeech.kt  ★ new — ports (+ STT/TTS backend registry, same pattern)
│   ├── model/ModelCatalog.kt + ModelManager.kt   ★ new — catalog + download/capability gating
│   └── ui/ (ChatViewModel/ChatScreen/components)  (exists; wire voice + confirm UI)
├── androidMain/.../agent/   ★ LiteRtLmEngine (Kotlin API) + LlamaCppEngine + STT/TTS android actuals
├── iosMain/.../agent/       ★ LiteRtLmEngine (Swift pkg bridge) + LlamaCppEngine + actuals (Dispatchers.Default)
└── desktopMain/.../agent/   ★ LiteRtLmEngine (Kotlin/JVM) and/or LlamaCppEngine + actuals
   (iosApp Swift target: thin bridge exposing the LiteRT-LM Swift package to iosMain)

data/common/src/commonMain/.../agent/
├── (existing contracts)
└── DeviceCapability.kt                           ★ new — expect: totalRamBytes() etc. for model gating

feature/invoice/.../agent/InvoiceActionHandler.kt  (add CREATE; add InvoiceDraft build via saveInvoice)
feature/order/.../agent/OrderActionHandler.kt       (optional: add CREATE later)
```

Registry wiring: contribute each handler into a Metro map and populate `ActionRegistry` from it.

---

## 4. Detailed Design

### 4.0 Provider abstraction (ports & adapters) — the adaptability backbone

Everything swappable sits behind a `commonMain` **port**; each platform ships one or more **adapters**;
a `ProviderRegistry` selects the active one at runtime from `PlatformDefaults` (overridable by
`AssistantConfig`). This is what makes engines, models, speech, and flow steps interchangeable later.

```kotlin
// Ports — the pipeline only ever sees these
interface LlmEngine { suspend fun load(model: ModelDescriptor, params: LlmParams)
    suspend fun generateConstrained(prompt: String, schema: OutputSchema, maxTokens: Int): String
    suspend fun generate(prompt: String, maxTokens: Int): String; fun isLoaded(): Boolean; suspend fun close() }
interface SpeechToText { /* start/stop, Flow<partial>, final */ }
interface TextToSpeech { /* speak/stop/mute */ }
interface IntentResolver { /* exists */ }
interface ActionHandler { /* exists */ }

// Self-describing adapters, contributed to a Metro multibinding
interface LlmBackend { val id: String; fun supports(m: ModelDescriptor): Boolean; fun create(): LlmEngine }

@Inject @SingleIn(WorkspaceScope::class)
class ProviderRegistry(
    private val llmBackends: Set<LlmBackend>,        // LiteRtLmBackend, LlamaCppBackend, …
    private val platformDefaults: PlatformDefaults,  // expect/actual: best ids per platform
    private val config: AssistantConfig,             // persisted in DataStore — runtime override
    private val catalog: ModelCatalog,
) {
    fun llmEngine(): LlmEngine = pick(llmBackends, config, platformDefaults).create()
}
```

Adaptability guarantees this buys:

- **Swap the model** → add/point to a `ModelCatalog` entry; `ModelManager` downloads, `ProviderRegistry`
  selects. No code change to resolver/handlers/UI.
- **Swap/add an engine** (e.g. a future MLC or ONNX backend) → add one `LlmBackend` adapter + register
  it. `PlatformDefaults`/`AssistantConfig` decide when it's used.
- **Change the pipeline/flow** → the orchestrator is a thin ordered chain of injected steps
  (transcribe → resolve → confirm? → dispatch → render/speak); steps can be reordered, inserted
  (e.g. a guardrail/PII or analytics stage), or replaced independently.
- **Per-platform "best"** lives only in `PlatformDefaults` (Android/iOS/Desktop → litert-lm; llama.cpp
  fallback). Everything else reads it; nothing else hardcodes a platform choice.
- **STT/TTS** follow the identical backend-registry pattern, so speech engines are swappable too.

### 4.1 Registry wiring (closes Gap #1)

Make each `ActionHandler` a Metro multibinding member and have `ActionRegistry` consume the map. Because
handlers depend on workspace-scoped repositories/DAOs, they are contributed to **`WorkspaceScope`**:

```kotlin
// each handler, e.g. InvoiceActionHandler
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@ActionHandlerKey("invoice")
class InvoiceActionHandler(...) : ActionHandler { ... }
```

`ActionRegistry` (workspace-scoped) receives `Map<String, ActionHandler>` (or a set of providers) and
registers them on creation, so `getAllActions()` / `dispatch()` are populated. `AgentOrchestrator`,
`LlmIntentResolver`, and `ChatViewModel` move to `WorkspaceScope` (ChatViewModel already is).

> Decision point for review: handlers wrap workspace DBs, so registry + orchestrator must be
> `WorkspaceScope`. Confirm there are no `AppScope` consumers of `AgentOrchestrator`.

### 4.2 On-device intent resolution (closes Gap #2, FR-003/004)

`LlmIntentResolver : IntentResolver`:

1. Build a system prompt from `ActionRegistry.generateCapabilitiesPrompt()` (already exists).
2. Build an engine-agnostic **`OutputSchema`** (`AgentSchemaBuilder`) from `ActionRegistry` metadata,
   constraining output to a valid `AgentAction` — `actionType` limited to the enum, `moduleName` to
   registered modules, `params` keys hinted from the relevant `ActionDescriptor`, plus an
   `"intent": conversation | clarify | action` discriminator. The schema renders to **GBNF** (llama.cpp)
   or a **JSON-schema / function-call definition** (LiteRT-LM) — the resolver doesn't care which.
3. Call `providerRegistry.llmEngine().generateConstrained(prompt, schema)` → parse → `ResolvedIntent`.
   On LiteRT-LM, prefer its native function-calling (the engine returns a structured tool call directly).
4. Validate the parsed action against the schema; on parse/confidence failure → `ResolvedIntent.Clarification`
   (never execute a wrong action).

`LlmEngine` is a **commonMain interface** (not an `expect class`) so a platform can offer more than one
backend and config can pick between them (see §4.0). Adapters: **`LiteRtLmEngine`** (Android/JVM Kotlin
API; iOS via the Swift package bridged from iosMain) and **`LlamaCppEngine`** (JNI on Android/Desktop,
cinterop on iOS) — both registered as `LlmBackend`s. `ProviderRegistry` picks per `PlatformDefaults` +
`AssistantConfig`. The engine is created lazily on first use and closed on workspace switch
(`WorkspaceClosableRegistry`).

DI: bind `@OfflineIntentResolver` to a composite that uses `LlmIntentResolver` when a model is loaded,
else `RuleBasedIntentResolver` (FR-004/FR-012). `@OnlineIntentResolver` stays the cloud slot.

### 4.3 Voice (closes Gap #3, FR-008/009)

`SpeechToText` (expect): start/stop, exposes partial transcripts (`Flow<String>`) and a final result.
`ChatViewModel` collects the final result → calls the (currently dead) `onVoiceResult()` → submits to
the orchestrator. Mic permission via Moko Permissions (already a dependency).

`TextToSpeech` (expect): `speak(text)`, `stop()`, mute flag in `ChatUiState`. Wire response read-back
in `ChatViewModel` after a response is produced.

Platform actuals:
- Android: `SpeechRecognizer` with `EXTRA_PREFER_OFFLINE`; `android.speech.tts.TextToSpeech`.
- iOS: `SFSpeechRecognizer` (`requiresOnDeviceRecognition = true`); `AVSpeechSynthesizer`.
- Desktop: phase-1 stub / text-only; phase-2 whisper.cpp + Piper.

### 4.4 Invoice creation by voice (closes Gap #4, FR-005/006/007)

Add `ActionType.CREATE` to `InvoiceActionHandler`:

1. Resolve customer: `CustomerRepository` search by spoken name → 0/1/many.
   - many → `ActionResult.NeedsInput` disambiguation; 0 → offer to create or ask again.
2. Resolve each line item: `ProductRepository` search → quantity (word/number) + unit price + tax code.
3. Compute tax/totals via the tax module logic (reuse existing calculator).
4. Build an **InvoiceDraft** (transient) → return `ActionResult.NeedsInput`/a new `Confirm` summary with
   the **total formatted in the workspace locale** (FR-013). **Do not persist yet** (FR-006).
5. On user confirm (next turn), build `InvoiceEntity` + `List<InvoiceItemEntity>` and call the existing
   `InvoiceRepository.saveInvoice(entity, items)` (`feature/invoice/.../InvoiceRepository.kt:46`) →
   Room write `synced=false` + `markPending` → `CentralSyncService` pushes when online. UID generated
   in the handler/VM layer per project rules.

Confirmation model: extend `ActionResult` with `data class Confirm(summary, pendingAction)` OR carry a
pending action in `ChatUiState` and reuse `NeedsInput`. **Decision for review:** prefer a typed
`Confirm` result so the UI can render an explicit confirm/cancel affordance and TTS can read the total.

### 4.5 Model delivery & capability gating (FR-011/012)

`ModelManager` (commonMain): tracks `ModelAsset` (id, url, sizeBytes, minRamBytes, localPath, state),
downloads via Ktor to app-private storage with progress, verifies checksum, and on startup selects the
best model that fits `DeviceCapability.totalRamBytes()` — or none (→ rule-based). Persist the chosen
model id and download state in the existing DataStore (never a new instance). Wi-Fi gating + explicit
user opt-in for the download.

---

## 5. Phasing (incremental, each independently shippable)

- **Phase 0 — Wire the registry (US1 backbone).** Contribute handlers, populate `ActionRegistry`, move
  orchestrator to `WorkspaceScope`. Result: the *existing regex* assistant actually executes read-only
  queries. No new deps. (FR-001/002)
- **Phase 1 — Voice I/O (US4).** `SpeechToText`/`TextToSpeech` expect/actual with platform-native
  engines; wire `onVoiceResult` + read-back. Text+voice for the regex assistant. (FR-008/009)
- **Phase 2 — On-device LLM NLU (US3).** Add `LlmEngine` (llama.cpp) + `LlmIntentResolver` +
  `AgentGrammarBuilder` + `ModelManager`. Natural-language → valid actions. (FR-003/004/011/012)
- **Phase 3 — Invoice-by-voice (US2).** Invoice CREATE handler + `Confirm` flow + entity resolution +
  tax. (FR-005/006/007)
- **Phase 4 — Polish & online path.** whisper.cpp Desktop STT, cloud `OnlineIntentResolver`, locale
  formatting pass, accessibility, telemetry. (FR-013/014)

Phases 0–1 ship value with **no native/model integration risk**; the heavy lift (LiteRT-LM / llama.cpp
native integration) is isolated behind the `LlmEngine` port in Phase 2.

---

## 6. Build / Dependencies

- Add to `gradle/libs.versions.toml` only (no hardcoded versions):
  - **LiteRT-LM**: Kotlin API (Android + Desktop/JVM); Swift package for iOS/macOS (consumed via the
    `iosApp` Swift target / SPM, bridged into iosMain).
  - **llama.cpp** binding(s) as the fallback adapter (Android AAR/JNI, Desktop JVM JNI, iOS cinterop).
  - Phase-2 **whisper.cpp** for cross-platform STT.
- iOS: package native libs as XCFrameworks / SPM; add cinterop `.def` where needed. Desktop: bundle
  per-OS native libs (Linux/macOS/Windows).
- **Reference implementation (studied, copy-adaptable, Apache-2.0):** the **Google AI Edge Gallery**
  app — concrete LiteRT-LM recipe, `ToolSet` pattern, Model Manager, and voice UX are distilled in
  **`docs/features/AGENT_LITERTLM_REFERENCE.md`** (with file citations). The `LiteRtLmEngine` wraps
  `com.google.ai.edge.litertlm` `Engine`/`Conversation`/`tool()`; tool calls bridge to `ActionRegistry`.
- Models are **not** Gradle deps — downloaded at runtime (FR-011).
- Per `/cmp-practices` §9: if `feature/agent` ever gets `maven-publish`, pin `packageOfResClass`.

---

## 7. Risks & Mitigations

| Risk | Mitigation |
|---|---|
| Native engine integration effort (3 platforms) | Isolate behind the `LlmEngine` port; Phases 0–1 don't need it. LiteRT-LM gives one runtime across Android (Kotlin), iOS (Swift), Desktop (Kotlin/JVM); llama.cpp is the fallback adapter. Start Android → Desktop → iOS. |
| Engine/model churn over time | Ports + `ProviderRegistry` + `ModelCatalog`/`AssistantConfig` make engines and models swappable by config; adding a backend is one adapter class. |
| Small-model tool-calling unreliability | LiteRT-LM **native function calling** on mobile; **GBNF grammar** on llama.cpp — both via one `OutputSchema`; validate + re-ask guarantees valid `AgentAction` (SC-003). |
| Model size / device RAM / battery | `ModelManager` capability gating + low-RAM tier (Gemma 4 E2B / Gemma 3 1B) + rule-based fallback (SC-006). |
| LiteRT-LM Desktop/iOS binding maturity | Verify Kotlin/JVM desktop native libs + iOS Swift-package bridge early (T031); llama.cpp covers any gap with zero pipeline change. |
| Money actions executed wrongly from speech | Mandatory `Confirm` step with spoken total (FR-006, SC-002). |
| Workspace data leakage / stale model | Workspace-scoped registry + engine; close on switch via `WorkspaceClosableRegistry`. |
| STT accuracy for code-mixed/Hindi | Phase-1 native + Phase-2 whisper.cpp multilingual; show transcript for user correction. |
| iOS `Dispatchers.IO` in iosMain | Use `Dispatchers.Default` per project KMP rule. |

---

## 8. Definition of Done

- All FRs met; success criteria SC-001…SC-008 measured and passing.
- All three targets compile (SC-008); no `java.*`/`android.*` in `commonMain`.
- No `LocalAppGraph.current` in composables; ViewModels via Metro; strings from Compose resources;
  amounts via `formatMoney(..., LocalAppLocale.current)`.
- Money actions never persist without confirmation; offline create + later push verified (SC-007).
- Docs updated; tasks in `./tasks.md` checked off.

---

## Open Questions for Review

1. **Scope of v1:** ship Phase 0–1 (regex + voice) first, defer LLM (Phase 2+) to a follow-up? Or
   target the full offline-LLM experience in one release?
2. **Cloud LLM path:** do we want an online resolver (backend proxy to a hosted model) now, or
   offline-only for v1?
3. **Confirm UX:** typed `ActionResult.Confirm` (preferred) vs. reuse `NeedsInput` with pending action.
4. **Default engine + model + tiers:** confirm LiteRT-LM as the primary engine (Android/iOS/Desktop)
   with llama.cpp fallback; default model **Gemma 4 E4B**, low-RAM **Gemma 4 E2B / Gemma 3 1B**,
   llama.cpp/compat **Qwen2.5-3B**. Confirm RAM thresholds for gating.
5. **Languages:** which STT/UI languages for v1 (English only, or English + Hindi/code-mixed)?
