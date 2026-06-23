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

Models by **role** (per the Gallery): **FunctionGemma-270m** for intent→action tool-calling,
**Gemma 3n E2B/E4B** for conversational answers, **Qwen2.5-3B** GGUF as the llama.cpp fallback. Engine +
model are runtime-selectable via `ProviderRegistry` + `PlatformDefaults` + `AssistantConfig` +
`ModelCatalog` — never hardcoded. Tool-calling: LiteRT-LM native `ToolSet`/`@Tool` on mobile/JVM, GBNF on
llama.cpp. Reference recipe: `docs/features/AGENT_LITERTLM_REFERENCE.md`.

---

## Phase 0 — Wire the registry (US1 backbone) · FR-001/002

- [x] T001 Add `@ActionHandlerKey` map-key annotation + Metro `@ContributesIntoMap(WorkspaceScope::class)`
      to each handler: customer, product, order, invoice, inventory (`feature/*/agent/*ActionHandler.kt`).
      → `ActionHandlerKey` added in `data/common/.../agent/`; all 5 handlers annotated.
- [x] T002 `ActionRegistry` now consumes injected `Map<String, ActionHandler>` (Metro multibinding) and
      is self-populating; removed the unused `register()`/`ActionHandlerProvider` path
      (`ActionHandlerProvider.kt` deleted).
- [x] T003 `AgentOrchestrator` + `ActionRegistry` stay unscoped `@Inject` and resolve inside the
      WorkspaceGraph via `ChatViewModel` (already `WorkspaceScope`); handlers' map is WorkspaceScope. No
      `AppScope` consumer of the orchestrator exists.
- [x] T004 `AgentModule` still binds `@OfflineIntentResolver`/`@OnlineIntentResolver` (unchanged).
- [~] T005 [P] Smoke-test offline: registry non-empty, "how many invoices", "low stock", "search
      orders 1001" execute end-to-end against Room. → `ActionRegistryTest` added (dispatch routing,
      params passthrough, unknown-module error, handler-exception handling, capabilities, empty-registry)
      + agent module folded into Kover. Full handler-against-Room e2e still pending (needs the heavy
      repo fakes; see T007 build note).
- [ ] T006 [P] Move any hardcoded chat strings to Compose resources; format amounts via
      `formatMoney(..., LocalAppLocale.current)` in result rendering. (deferred — `MessageBubble`/result
      rendering pass)
- [ ] T007 Compile all three targets (checkpoint). ⚠ Could not run in the dev sandbox: the Gradle
      daemon is pinned to a JetBrains JDK (`gradle/gradle-daemon-jvm.properties`) that can't be
      downloaded offline, and plugin repos (AGP) are unreachable. Run locally/CI:
      `androidApp:compileDebugKotlinAndroid` · `shared:compileKotlinIosSimulatorArm64` · `desktopApp:compileKotlin`.

**Phase 0 acceptance:** US1 scenarios pass in airplane mode using the existing rule-based resolver.
**Status:** wiring complete (T001–T004); compile + smoke-test (T005/T007) pending a build environment.

### Phase 0.5 — Ship the assistant as an installable product module · (new requirement)
The AI assistant is surfaced through the dynamic-module system (installed per workspace), not always-on.
- [x] T008 Add `ModuleCodes.AI_ASSISTANT = "ai-assistant"`; mark it implementation-available in
      `DynamicModuleNavigationService.isModuleImplementationAvailable`.
- [x] T009 Map the module to the chat route: `moduleCodeToRoute(AI_ASSISTANT) → Route.Agent`,
      `resolveActiveModuleCode(Route.Agent) → AI_ASSISTANT`, display name (`nav_assistant`), icon
      (`AutoAwesome`), desktop menu path (`agent`). `Route.Agent` was previously unreachable; it now
      appears only when the workspace has the module installed + active.
- [ ] T010 **Backend (ampairs repo):** register an `ai-assistant` module in the workspace module
      catalog so it shows up as installable to users. Until then the app maps it correctly but it won't
      appear in the install list. (Cross-repo; not in this PR.)

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

> Concrete LiteRT-LM recipe + `ToolSet` pattern + Model Manager + voice UX (from the studied Google AI
> Edge Gallery, Apache-2.0): **`docs/features/AGENT_LITERTLM_REFERENCE.md`**. Mirror it.

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
- [ ] T025 [P] `LiteRtLmEngine : LlmEngine` — Android (Kotlin API) + Desktop (Kotlin/JVM) actuals,
      wrapping `Engine(EngineConfig(modelPath, backend, maxNumTokens, …))` + `engine.createConversation(
      ConversationConfig(samplerConfig, systemInstruction, tools))` + `conversation.sendMessageAsync(
      Contents, MessageCallback)` (see reference doc).
- [ ] T025b `AmpairsAgentToolSet : ToolSet` — one `@Tool`/`@ToolParam` method per supported action
      (createInvoice, searchCustomers, countInvoices, lowStockItems, …), each → `AgentAction` →
      `ActionRegistry.dispatch` (mirrors Gallery `MobileActionsTools` + `onFunctionCalled`).
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
      **iOS Swift-package** bridge works from iosMain; **FunctionGemma-270m** (tool-calling) + **Gemma 3n
      E2B/E4B** (`.litertlm`/`.task`, chat) available; **Qwen2.5-3B** GGUF set for the llama.cpp path.
      Adjust `PlatformDefaults` / catalog defaults per findings (no pipeline change required).
- [ ] T032 `ModelManager`: catalog-driven (mirror Gallery `model_allowlist.json` + `ModelAllowlist.kt`/
      `DownloadRepository.kt`) Hugging Face download to app-private dir, progress, checksum, Wi-Fi gating,
      RAM gating via `estimatedPeakMemoryInBytes` + `Capabilities(modelPath)` probe; persist choice (FR-011/012).
- [ ] T033 [P] Model/engine UI: download progress, Wi-Fi prompt, "reduced mode" banner (US5); dev
      settings engine+model picker reading `AssistantConfig`.
- [ ] T034 [P] Eval harness: 20 paraphrase variants (SC-003) + 30-question set (SC-001); run across both
      engines (LiteRT-LM, llama.cpp) to compare.
- [ ] T035 Compile all three targets (checkpoint).

**Phase 2 acceptance:** US3 + US5 scenarios pass; SC-001/003/004/006 measured.

### Phase 2g — Safe read-only SQL fallback (SAFE_QUERY) · FR-016 (see plan §4.6)
- [x] T036 `SafeSqlValidator` + `ModuleQuerySchema`/`TableSchema`/`ColumnSchema` (commonMain,
      `feature/agent/query/`): SELECT-only, single-statement, no-comments, keyword-deny, table-allowlist
      (+ CTE), LIMIT-enforce → `Valid`/`Rejected`. Unit-tested (`SafeSqlValidatorTest`, SC-009).
- [ ] T037 Per-module `SqlQueryDelegate` (WorkspaceScope): expose a curated `ModuleQuerySchema` +
      execute validated SQL via Room KMP `@RawQuery`/`RoomRawQuery` on a **reader** connection →
      `List<Map<String,Any?>>`. (Build env: needs each module's DAO.)
- [ ] T038 `ResolvedIntent.SafeQuery` + orchestrator fallback tier (Action → SAFE_QUERY →
      Clarification); module pick + text-to-SQL (`ModuleQuerySchema.toPromptText()`) → validate →
      execute → model phrases rows. Gated by `AssistantConfig` (default off); prefer online/larger model.
- [ ] T039 Tests: text-to-SQL eval set (rejection rate = 100% for bad inputs, SC-009) + read-only proof.

---

## Phase 3 — Invoice-by-voice (US2) · FR-005/006/007

- [x] T040 `ActionType.CREATE` added to `InvoiceActionHandler` — builds a **DRAFT** invoice and saves
      via `InvoiceRepository.saveInvoice(...)` (offline-first, flagged PENDING_PUSH); returns a
      navigation target to open it. Rule-based resolver routes "create/make a bill/invoice for {X}".
- [x] T041 Customer resolution via `customerDataService.listCustomers(name)` → exact/single match
      used; many → `NeedsInput` disambiguation; none → `Error` (offer to add the customer).
- [~] T042 Line-item resolution: single optional item supported (`productDataService.searchSummaries`
      + quantity). Multi-item commands ("2 widgets and 1 cable") await the LLM resolver (Phase 2);
      unit/tax-code selection still TODO.
- [ ] T043 Tax/total computation reusing the tax module calculator. → currently total = Σ(qty×price)
      on a DRAFT (no GST breakdown); user finalizes in the editor. Wire the tax calculator next.
- [ ] T044 Build transient `InvoiceDraft`; return a typed `ActionResult.Confirm` summary with total
      formatted in workspace locale; **no persistence yet** (FR-006). → current cut saves a DRAFT
      directly (DRAFT ⇒ no receivable side-effects); the explicit pre-save Confirm step is the next
      increment.
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

- [ ] T050 [P] STT: primary = **LiteRT-LM audio input** (`Content.AudioBytes` + Gemma 3n audio; mirror
      Gallery `HoldToDictate`); whisper.cpp only as the Desktop/lighter fallback `SpeechToText` actual.
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
