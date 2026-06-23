# Tasks: Offline AI Assistant (Text & Voice Agentic Chat)

**Spec**: `./spec.md` · **Plan**: `./plan.md` · **Branch**: `claude/nice-gauss-0ndtbn`

Tasks are grouped by the phases in `plan.md` §5 and ordered by dependency. `[P]` = parallelizable with
sibling `[P]` tasks. Each phase is independently shippable. Compile all three targets at the end of any
phase that touches `commonMain`:
`androidApp:compileDebugKotlinAndroid` · `shared:compileKotlinIosSimulatorArm64` · `desktopApp:compileKotlin`.

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

- [ ] T020 Add llama.cpp bindings to `gradle/libs.versions.toml` (Android JNI/AAR, Desktop JVM JNI,
      iOS cinterop `.def`); package XCFramework (iOS) and native libs (Desktop).
- [ ] T021 `expect class LlmEngine` (`feature/agent/llm/LlmEngine.kt`):
      `load/generateConstrained/generate/isLoaded/close` + `LlmParams`.
- [ ] T022 [P] Android/Desktop JNI actual; T023 [P] iOS cinterop actual (`Dispatchers.Default`).
- [ ] T024 `DeviceCapability` expect/actual in `data/common/.../agent/` (`totalRamBytes()`).
- [ ] T025 `ModelManager` (commonMain): `ModelAsset` state, Ktor download to app-private dir, progress,
      checksum, Wi-Fi gating, capability-based selection; persist choice in existing DataStore.
- [ ] T026 `AgentGrammarBuilder`: turn `ActionRegistry` `ActionDescriptor`s into a GBNF grammar + system
      prompt constraining output to a valid `AgentAction` (+ intent discriminator).
- [ ] T027 `LlmIntentResolver : IntentResolver`: prompt + `generateConstrained` → parse → `ResolvedIntent`;
      low-confidence/parse-fail → `Clarification`.
- [ ] T028 Composite offline resolver: use `LlmIntentResolver` when a model is loaded, else
      `RuleBasedIntentResolver`; bind to `@OfflineIntentResolver`. Register engine with
      `WorkspaceClosableRegistry` (close on workspace switch).
- [ ] T029 [P] Model download UI: progress, Wi-Fi prompt, "reduced mode" banner when no model (US5).
- [ ] T030 [P] Evaluation harness: 20 paraphrase variants → assert valid action + correct mapping
      (SC-003); 30-question query set (SC-001).
- [ ] T031 Compile all three targets (checkpoint).

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
