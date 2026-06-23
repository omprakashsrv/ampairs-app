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
                       LlmEngine (expect/actual: llama.cpp)  │   ModelManager (download + device-capability gating)
```

Three new capabilities, each behind a `commonMain` interface with `expect`/`actual` platform bridges:
**LlmEngine**, **SpeechToText**, **TextToSpeech**. One new `commonMain` service: **ModelManager**
(asset download + selection). Plus the registry wiring and the invoice CREATE handler.

---

## 2. Technology Choices (decisions)

| Concern | Decision | Rationale |
|---|---|---|
| LLM runtime | **llama.cpp** via `expect`/`actual` (JNI on Android + Desktop-JVM, C-interop on iOS) | Only engine that covers **all 3** targets incl. Desktop; supports **GBNF grammar** constrained decoding. |
| LLM model (default) | **Qwen2.5-3B-Instruct q4** (≈2 GB); fallback **Gemma 3 1B q4** (≈0.8 GB) for low-RAM | Strong small-model tool-calling; Gemma satisfies the "Gemma" ask and the low-RAM tier. |
| Reliable tool-calling | **Grammar-constrained decoding** to the `AgentAction` JSON schema, generated from `ActionRegistry` metadata | Makes 1–3B models emit 100% structurally-valid actions (SC-003). |
| STT (phase 1) | Platform-native: Android `SpeechRecognizer` (offline pref), iOS `SFSpeechRecognizer` (`requiresOnDeviceRecognition`), Desktop stub→Vosk/whisper | Zero model download, fastest to ship on mobile. |
| STT (phase 2) | **whisper.cpp** (base ≈140 MB) via `expect`/`actual` | Uniform cross-platform quality incl. Desktop; multilingual/code-mixed. |
| TTS | Platform-native (`TextToSpeech` / `AVSpeechSynthesizer`); Desktop via system/Piper | Offline, no download on mobile. |
| Model delivery | On-demand download to app-private dir; Wi-Fi gated; device-RAM gating | Models too large to bundle (FR-011/FR-012). |
| Online path | Keep `OnlineIntentResolver` slot for a cloud LLM (backend proxy) — optional | Orchestrator already does online→offline fallback. |

`expect`/`actual` note: per project KMP rules, the iOS actual for IO dispatch uses
`Dispatchers.Default`, not `Dispatchers.IO`.

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
│   ├── offline/LlmIntentResolver.kt              ★ new — grammar-constrained, uses LlmEngine
│   ├── offline/AgentGrammarBuilder.kt            ★ new — ActionDescriptors → GBNF/JSON schema + system prompt
│   ├── llm/LlmEngine.kt                          ★ new — expect interface (load/generateConstrained/close)
│   ├── speech/SpeechToText.kt                    ★ new — expect interface (Flow<partial>, final)
│   ├── speech/TextToSpeech.kt                    ★ new — expect interface (speak/stop)
│   ├── model/ModelManager.kt                     ★ new — download, capability gating, ModelAsset state
│   └── ui/ (ChatViewModel/ChatScreen/components)  (exists; wire voice + confirm UI)
├── androidMain/.../agent/   ★ LlmEngine/STT/TTS/ModelManager android actuals
├── iosMain/.../agent/       ★ ... ios actuals (Dispatchers.Default)
└── desktopMain/.../agent/   ★ ... desktop actuals

data/common/src/commonMain/.../agent/
├── (existing contracts)
└── DeviceCapability.kt                           ★ new — expect: totalRamBytes() etc. for model gating

feature/invoice/.../agent/InvoiceActionHandler.kt  (add CREATE; add InvoiceDraft build via saveInvoice)
feature/order/.../agent/OrderActionHandler.kt       (optional: add CREATE later)
```

Registry wiring: contribute each handler into a Metro map and populate `ActionRegistry` from it.

---

## 4. Detailed Design

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
2. Build a **GBNF grammar** (`AgentGrammarBuilder`) that constrains output to a JSON object matching
   `AgentAction` — `actionType` limited to the enum, `moduleName` limited to registered modules, and
   `params` keys hinted from the relevant `ActionDescriptor`. Include an `"intent": "conversation" |
   "clarify" | "action"` discriminator so the model can ask questions or chat.
3. Call `LlmEngine.generateConstrained(prompt, grammar)` → parse JSON → `ResolvedIntent`.
4. On parse/confidence failure → return `ResolvedIntent.Clarification` (never execute a wrong action).

`LlmEngine` (expect):

```kotlin
expect class LlmEngine {
    suspend fun load(modelPath: String, params: LlmParams)
    suspend fun generateConstrained(prompt: String, grammar: String, maxTokens: Int): String
    suspend fun generate(prompt: String, maxTokens: Int): String   // for result phrasing
    fun isLoaded(): Boolean
    suspend fun close()
}
```

Actuals: Android/Desktop JNI to llama.cpp; iOS via cinterop. Engine is created lazily on first LLM use
and closed on workspace switch (register with `WorkspaceClosableRegistry`).

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

Phases 0–1 ship value with **no native/model integration risk**; the heavy lift (llama.cpp cinterop +
JNI) is isolated to Phase 2.

---

## 6. Build / Dependencies

- Add to `gradle/libs.versions.toml` only (no hardcoded versions): llama.cpp binding(s) per platform
  (Android AAR/JNI, Desktop JVM JNI artifact, iOS via cinterop def), and Phase-2 whisper.cpp.
- iOS: package llama/whisper as XCFrameworks; add cinterop `.def`. Desktop: bundle native libs per OS.
- Models are **not** Gradle deps — downloaded at runtime (FR-011).
- Per `/cmp-practices` §9: if `feature/agent` ever gets `maven-publish`, pin `packageOfResClass`.

---

## 7. Risks & Mitigations

| Risk | Mitigation |
|---|---|
| Native llama.cpp integration effort (3 platforms) | Isolate behind `LlmEngine` expect/actual; Phases 0–1 don't need it; start with Android actual, then Desktop, then iOS. |
| Small-model tool-calling unreliability | **Grammar-constrained decoding** (GBNF) — non-negotiable; guarantees valid `AgentAction`. |
| Model size / device RAM / battery | `ModelManager` capability gating + low-RAM Gemma-1B tier + rule-based fallback (SC-006). |
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
4. **Default model + tiers:** confirm Qwen2.5-3B (default) / Gemma-3-1B (low-RAM), and the RAM
   thresholds for gating.
5. **Languages:** which STT/UI languages for v1 (English only, or English + Hindi/code-mixed)?
