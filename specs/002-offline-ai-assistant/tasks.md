# Tasks: Offline AI Assistant (Text & Voice Agentic Chat)

**Spec**: `./spec.md` · **Plan**: `./plan.md` · **Branch**: `claude/nice-gauss-0ndtbn`

Tasks are grouped by the phases in `plan.md` §5 and ordered by dependency. `[P]` = parallelizable with
sibling `[P]` tasks. Each phase is independently shippable. Compile all three targets at the end of any
phase that touches `commonMain`:
`androidApp:compileDebugKotlinAndroid` · `shared:compileKotlinIosSimulatorArm64` · `desktopApp:compileKotlin`.

### Engine decision (drives Phase 2 + Phase 4)

Best-per-platform behind one `LlmEngine` port (see `plan.md` §2/§4.0):

| Platform | Primary engine | Fallback |
|---|---|---|
| Android | **LiteRT-LM** (Kotlin API, GPU/NPU, native Gemma 4 function calling) | llama.cpp (JNI) |
| iOS | **LiteRT-LM** (Swift package, Metal; bridged from iosMain) | llama.cpp (cinterop) |
| Desktop (JVM) | **LiteRT-LM** (Kotlin/JVM) — verify desktop native libs (T031); else llama.cpp | llama.cpp (JNI) |

Default model **Gemma 4 E4B** (low-RAM **E2B / Gemma 3 1B**; llama.cpp/compat **Qwen2.5-3B**). Engine +
model are runtime-selectable via `ProviderRegistry` + `PlatformDefaults` + `AssistantConfig` +
`ModelCatalog` — never hardcoded. Tool-calling: LiteRT-LM native function calling on mobile/JVM, GBNF on
llama.cpp, both from one `OutputSchema`.

---

## Phase 0 — Wire the registry (US1 backbone) · FR-001/002

- [ ] T001 Add `@ActionHandlerKey` map-key annotation + Metro `@ContributesIntoMap(WorkspaceScope::class)`
      to each handler: customer, product, order, invoice, inventory (`feature/*/agent/*ActionHandler.kt`).
- [ ] T002 Make `ActionRegistry` consume the injected `Map<String, ActionHandler>` (or providers) and
      self-populate on init; remove the unused manual `register()` path or keep for tests
      (`feature/agent/core/ActionRegistry.kt`).
- [ ] T003 Move `AgentOrchestrator` (and the registry) to `WorkspaceScope`; verify `ChatViewModel`
      (already `WorkspaceScope`) resolves them. Confirm no `AppScope` consumer exists.
- [ ] T004 Verify `AgentModule` DI still binds `@OfflineIntentResolver`/`@OnlineIntentResolver`
      (`feature/agent/di/AgentModule.kt`).
- [ ] T005 [P] Smoke-test offline: registry non-empty, "how many invoices", "low stock", "search
      orders 1001" execute end-to-end against Room.
- [ ] T006 [P] Move any hardcoded chat strings to Compose resources; format amounts via
      `formatMoney(..., LocalAppLocale.current)` in result rendering.
- [ ] T007 Compile all three targets (checkpoint).

**Phase 0 acceptance:** US1 scenarios pass in airplane mode using the existing rule-based resolver.

---

## Phase 1 — Voice I/O (US4) · FR-008/009

- [ ] T010 `expect interface SpeechToText` in `feature/agent/speech/SpeechToText.kt` (start/stop,
      `Flow<String>` partials, final result, error).
- [ ] T011 `expect interface TextToSpeech` in `feature/agent/speech/TextToSpeech.kt` (speak/stop/mute).
- [ ] T012 [P] Android actuals: `SpeechRecognizer` (`EXTRA_PREFER_OFFLINE`) + `android.speech.tts.TextToSpeech`.
- [ ] T013 [P] iOS actuals: `SFSpeechRecognizer` (`requiresOnDeviceRecognition = true`) +
      `AVSpeechSynthesizer` (use `Dispatchers.Default`, `@OptIn(ExperimentalForeignApi::class)`).
- [ ] T014 [P] Desktop actuals: text-only stub for STT (returns unsupported) + system TTS or no-op.
- [ ] T015 Mic permission flow via Moko Permissions; deny → graceful text fallback (US4-3).
- [ ] T016 Wire `ChatViewModel`: collect STT final → `onVoiceResult()` → submit; speak responses via
      TTS with mute control in `ChatUiState`.
- [ ] T017 [P] `ChatScreen`/`VoiceInputButton`: show live transcript, listening state, mute toggle;
      `contentDescription` on controls.
- [ ] T018 Compile all three targets (checkpoint).

**Phase 1 acceptance:** US4 scenarios pass on Android & iOS (Desktop = text-only).

---

## Phase 2 — On-device LLM NLU (US3) · FR-003/004/011/012

### 2a. Provider abstraction (do first — the adaptability backbone)
- [ ] T020 Define ports + descriptors in `feature/agent/llm/`: `LlmEngine` (interface), `LlmBackend`,
      `ModelDescriptor`, `OutputSchema`, `LlmParams`.
- [ ] T021 `ModelCatalog` (commonMain) + `AssistantConfig` persisted in the existing DataStore
      (active engine id, model id, STT/TTS provider, flags).
- [ ] T022 `PlatformDefaults` expect/actual (best engine id per platform: Android/iOS/Desktop→litert-lm,
      fallback→llamacpp) + `DeviceCapability` expect/actual (`totalRamBytes()`) in `data/common/.../agent/`.
- [ ] T023 `ProviderRegistry` (`@Inject`, WorkspaceScope): pick `LlmBackend` from the Metro
      `Set<LlmBackend>` via config + defaults + capability; create/close engine lazily; register with
      `WorkspaceClosableRegistry`.

### 2b. LiteRT-LM adapter (primary on all platforms)
- [ ] T024 Add LiteRT-LM to `gradle/libs.versions.toml`: Kotlin API (Android + Desktop/JVM); iOS via
      Swift package consumed by the `iosApp` target.
- [ ] T025 [P] `LiteRtLmEngine : LlmEngine` — Android (Kotlin API) + Desktop (Kotlin/JVM) actuals; use
      LiteRT-LM **native function calling** to satisfy `OutputSchema`.
- [ ] T026 [P] `LiteRtLmEngine` iOS actual: thin Swift bridge in `iosApp` exposing the LiteRT-LM Swift
      package to iosMain (`Dispatchers.Default`).

### 2c. llama.cpp adapter (fallback + parity)
- [ ] T027 [P] Add llama.cpp bindings (Android JNI, Desktop JNI, iOS cinterop `.def` / XCFramework);
      `LlamaCppEngine : LlmEngine` with GBNF from `OutputSchema`.

### 2d. Resolver + schema + delivery
- [ ] T028 `AgentSchemaBuilder`: `ActionRegistry` `ActionDescriptor`s → `OutputSchema` (GBNF + JSON-schema
      / function-call renderings) + system prompt; engine-agnostic.
- [ ] T029 `LlmIntentResolver : IntentResolver` via `ProviderRegistry.llmEngine()`; parse → `ResolvedIntent`;
      validate against schema, re-ask on failure; low confidence → `Clarification`.
- [ ] T030 Composite offline resolver: `LlmIntentResolver` when a model is loaded else
      `RuleBasedIntentResolver`; bind `@OfflineIntentResolver`.
- [ ] T031 ⚠ Verify integration assumptions: LiteRT-LM **Kotlin/JVM desktop** native libs work; LiteRT-LM
      **iOS Swift-package** bridge works from iosMain; **Gemma 4 E4B** `.litertlm` available (mobile) and
      Gemma 4 GGUF / Qwen2.5-3B set in the catalog for the llama.cpp path. Adjust `PlatformDefaults` /
      catalog defaults per findings (no pipeline change required).
- [ ] T032 `ModelManager`: catalog-driven Ktor download to app-private dir, progress, checksum, Wi-Fi
      gating, RAM-based selection; persist choice (FR-011/012). (Study Google AI Edge Gallery for UX.)
- [ ] T033 [P] Model/engine UI: download progress, Wi-Fi prompt, "reduced mode" banner (US5); dev
      settings engine+model picker reading `AssistantConfig`.
- [ ] T034 [P] Eval harness: 20 paraphrase variants (SC-003) + 30-question set (SC-001); run across both
      engines (LiteRT-LM, llama.cpp) to compare.
- [ ] T035 Compile all three targets (checkpoint).

**Phase 2 acceptance:** US3 + US5 scenarios pass; SC-001/003/004/006 measured.

---

## Phase 3 — Invoice-by-voice (US2) · FR-005/006/007

- [ ] T040 Add `ActionType.CREATE` to `InvoiceActionHandler` (`feature/invoice/agent/`).
- [ ] T041 Customer resolution: search by spoken name → 0/1/many; many → `NeedsInput`
      disambiguation; 0 → offer create/ask.
- [ ] T042 Line-item resolution: product search + quantity (word→number) + unit price + tax code.
- [ ] T043 Tax/total computation reusing the tax module calculator.
- [ ] T044 Build transient `InvoiceDraft`; return a typed `ActionResult.Confirm` summary with total
      formatted in workspace locale; **no persistence yet** (FR-006).
- [ ] T045 Confirm/cancel handling in `AgentOrchestrator`/`ChatViewModel` (carry pending action across
      turns); on confirm → build `InvoiceEntity` + items, UID in VM/handler, call
      `InvoiceRepository.saveInvoice(...)` (offline-first → pending push).
- [ ] T046 [P] Render confirm card in `ChatScreen` (confirm/cancel buttons) + TTS reads the total.
- [ ] T047 [P] Tests: 20-utterance create set (SC-002); assert 0% persisted without confirm; restart →
      persists → pushes on reconnect (SC-007).
- [ ] T048 Compile all three targets (checkpoint).

**Phase 3 acceptance:** US2 scenarios pass; SC-002/005/007 measured.

---

## Phase 4 — Polish & online path · FR-013/014

- [ ] T050 [P] whisper.cpp Desktop (and optional mobile) STT actual via `expect`/`actual` + whisper model
      in `ModelManager`.
- [ ] T051 [P] Cloud `OnlineIntentResolver` (backend proxy to hosted LLM, tool-use) bound to
      `@OnlineIntentResolver`; verify orchestrator online→offline fallback (FR-014).
- [ ] T052 [P] Locale/strings audit; accessibility pass; assistant onboarding/help.
- [ ] T053 [P] Telemetry (resolve latency, action success, fallbacks) + Sentry breadcrumbs.
- [ ] T054 Docs: update `docs/features/` and module CLAUDE notes; final SC-001…SC-008 verification.

---

## Cross-cutting compliance checklist (apply to every phase)

- [ ] No `java.*`/`android.*` imports in `commonMain`; iOS uses `Dispatchers.Default`.
- [ ] ViewModels via Metro (`@ContributesIntoMap` + `@ViewModelKey`); no `LocalAppGraph.current` in composables.
- [ ] Repositories local-only; the assistant never calls feature APIs directly (sync via `CentralSyncService`).
- [ ] Money actions confirmed before persist; UIDs generated in VM/handler, never repository.
- [ ] User-visible strings from Compose resources; amounts via `formatMoney(..., LocalAppLocale.current)`.
- [ ] New deps only via `gradle/libs.versions.toml`; models downloaded at runtime, not bundled.
- [ ] All three targets compile after any `commonMain` change.
