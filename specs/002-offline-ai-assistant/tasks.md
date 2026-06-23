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
- [x] T006 [P] Agent UI strings moved to Compose resources: created
      `feature/agent/.../composeResources/values/strings.xml` (agent_* keys); `ChatScreen`,
      `VoiceInputButton`, `ActionResultCard`, `MessageBubble` use `stringResource`, `ChatViewModel` uses
      `getString` (error/cancel). Amounts now render via `formatMoney(amount, LocalAppLocale.current)`:
      `ActionResult.Success/Confirm` carry a structured `amount` → `AgentResponse` → `ChatMessage` →
      `MessageBubble` "Total: …" line in the workspace business currency (FR-013).
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
- [x] T010 **Backend (ampairs repo):** `ai-assistant` master module seeded in
      `MasterModuleSeederService` (moduleCode = `ai-assistant`, matches `ModuleCodes.AI_ASSISTANT`) so
      it appears as installable per workspace. COMMUNICATION category, FREE tier, depends on
      customer-management / product-management / invoice-billing. Runtime seed (CommandLineRunner) — no
      migration. Lives in the ampairs repo on branch `claude/nice-gauss-0ndtbn`.

---

## Phase 1 — Voice I/O (US4) · FR-008/009

- [x] T011 `SpeechToText` port in `feature/agent/speech/SpeechToText.kt` — cold `Flow<SttEvent>`
      (`Partial`/`Final`/`Error`/`EndOfSpeech`), `listen(languageTag)`/`stop()`/`isAvailable`. Plain
      interface (not `expect`) so platforms contribute via Metro DI, matching the codebase idiom.
- [x] T012 `TextToSpeech` port in `feature/agent/speech/TextToSpeech.kt` — `speak`/`stop`/`isAvailable`
      (mute handled in `ChatUiState`, T017).
- [~] T013 [P] Android actuals: `AndroidSpeechToText` over `SpeechRecognizer` (`EXTRA_PREFER_OFFLINE`,
      partial results, main-thread `callbackFlow` → `SttEvent`) + `AndroidTextToSpeech` over
      `android.speech.tts.TextToSpeech` (async-init aware), bound in `SpeechAndroidModule` with the app
      `Context`. Compiles on the Android CI job; runtime needs `RECORD_AUDIO` (T016) + a device to verify.
- [ ] T014 [P] iOS actuals: `SFSpeechRecognizer` (`requiresOnDeviceRecognition = true`) +
      `AVSpeechSynthesizer` (use `Dispatchers.Default`, `@OptIn(ExperimentalForeignApi::class)`).
- [x] T015 [P] Desktop fallback: shared commonMain `UnsupportedSpeechToText` (emits one `Error` →
      text-only, US4-3) + `NoOpTextToSpeech`, unit-tested (`UnsupportedSpeechTest`), bound in
      `SpeechDesktopModule`. Android/iOS bind the same fallbacks as placeholders (`SpeechAndroidModule`/
      `SpeechIosModule`) so T013/T014 are drop-in body swaps.
- [ ] T016 Mic permission flow via Moko Permissions; deny → graceful text fallback (US4-3). NOTE: the
      ChatViewModel wiring is permission-agnostic (it just collects `SpeechToText.listen()`), so the
      RECORD_AUDIO gate slots in at the Android actual (T013) without touching commonMain.
- [x] T017 Wire `ChatViewModel`: injects `SpeechToText`/`TextToSpeech`; `toggleVoiceInput()` collects
      the recognition flow (partials → `liveTranscript`, `Final` → submit, `Error`/unavailable →
      graceful stop/notice); speaks non-error replies unless muted; `toggleMute()` + `isTtsMuted`/
      `liveTranscript` in `ChatUiState`.
- [~] T018 [P] `ChatScreen`: voice button → `toggleVoiceInput()`, mute toggle in the app bar
      (`VolumeUp`/`VolumeOff` + `contentDescription`); listening state already shown by
      `VoiceInputButton`. Remaining: surface `liveTranscript` inline while listening.
- [ ] T019 Compile all three targets (checkpoint).

**Phase 1 acceptance:** US4 scenarios pass on Android & iOS (Desktop = text-only).

---

## Phase 2 — On-device LLM NLU (US3) · FR-003/004/011/012

> Concrete LiteRT-LM recipe + `ToolSet` pattern + Model Manager + voice UX (from the studied Google AI
> Edge Gallery, Apache-2.0): **`docs/features/AGENT_LITERTLM_REFERENCE.md`**. Mirror it.

### 2a. Provider abstraction (do first — the adaptability backbone)
- [x] T020 Ports + descriptors in `feature/agent/llm/`: `LlmEngine` (load/generate/generateConstrained/
      close), `LlmBackend` (id/supports/create), `ModelDescriptor` (+`ModelRole`), `OutputSchema`
      (structured + `toJsonSchema()`/`toGbnf()`), `LlmParams`. Pure commonMain, no native code.
- [~] T021 `ModelCatalog` (commonMain) — role-split entries (FunctionGemma-270m / Gemma 3n E2B/E4B /
      Qwen2.5-3B) with provisional download metadata (confirm T031); `byId`/`byRole`/`byBackend`.
      `AssistantConfig` (engine/model ids, llm/stt/tts flags, `safeQueryEnabled` default off). Unit-
      tested (`ModelCatalogTest`). Remaining: DataStore persistence binding — lands with `ProviderRegistry`
      (T023), kept out here so the shape stays pure/testable.
- [~] T022 `PlatformDefaults` expect/actual (primary/fallback engine id: Android/iOS → litert-lm,
      Desktop → llamacpp until T031) + `DeviceCapability` expect/actual `totalRamBytes()` (Android
      `/proc/meminfo`, Desktop `com.sun.management` MXBean, iOS `NSProcessInfo.physicalMemory`) in
      `feature/agent/llm/`. RAM tiers encoded in pure `RamTiers` (<3 GB rule-based / 3–6 GB E2B / ≥6 GB
      E4B), unit-tested (`RamTiersTest`). Remaining: `ProviderRegistry` (T023) consumes these. No
      external deps — CI compile-validates all targets; actual RAM values verified on device.
- [~] T023 `ProviderRegistry` (`@Inject`, `@SingleIn(WorkspaceScope)`): picks an `LlmBackend` from the
      Metro `Set<LlmBackend>` via the pure, unit-tested `BackendSelector` (`BackendSelectorTest`) —
      preference order config override → `PlatformDefaults` primary → fallback, requiring
      `supports(model)`. Chat model is RAM-gated through `RamTiers`/`DeviceCapability` (sub-3 GB or
      `llmEnabled=false` → null engine → rule-based only). Engine created + loaded lazily behind a
      `Mutex`; registered with `WorkspaceClosableRegistry` (suspend `close()` bridged via a fire-and-
      forget cleanup scope). Exposes suspend `engineOrNull()` / `isLlmReady()` as the seams for the
      T029 `engineProvider` and T030 `isLlmReady`. DI: `AgentLlmModule` `@Multibinds(allowEmpty=true)`
      `Set<LlmBackend>` (engines contribute at T025/T027) + `@Provides AssistantConfig.Default`.
      Remaining: bind `engineProvider`/`isLlmReady` into the `@OfflineIntentResolver` (T030) and
      DataStore-back `AssistantConfig` (T032/T033). CI compile-validates all targets; engine
      create/load path verified on device once an adapter lands.

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
- [x] T028 `AgentSchemaBuilder`: `ActionDescriptor`s → `OutputSchema` (JSON-schema + GBNF renderings)
      + `systemPrompt`; engine-agnostic. Unit-tested (`AgentSchemaBuilderTest`): output constrained to
      exactly the registered action types/modules, required-param markers in the prompt (SC-003 backbone).
- [x] T029 `LlmIntentResolver : IntentResolver` — builds the schema/prompt (`AgentSchemaBuilder`), calls
      `engine.generateConstrained`, extracts the JSON (even from prose), and **strictly validates**:
      unregistered action/module or missing required param → `Clarification` (never a wrong action,
      SC-003); one re-ask on parse failure. Engine resolved lazily via `engineProvider`, now DI-bound to
      `ProviderRegistry.engineOrNull()` in `AgentLlmModule`. Unit-tested with a fake engine
      (`LlmIntentResolverTest`).
- [x] T030 `CompositeOfflineResolver`: uses `LlmIntentResolver` when `isLlmReady()` else
      `RuleBasedIntentResolver`, and falls back to rule-based if the LLM path throws (graceful
      degradation, FR-004/FR-012). Bound to `@OfflineIntentResolver` in WorkspaceScope (`AgentLlmModule`)
      with `isLlmReady`/`engineProvider` from `ProviderRegistry`. Unit-tested
      (`CompositeOfflineResolverTest`).
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
      engines (LiteRT-LM, llama.cpp) to compare. Also capture end-of-speech→response latency on a
      mid-range (4–8 GB) device and assert the median ≤ 3 s (SC-004).
- [ ] T035 Compile all three targets (checkpoint).

**Phase 2 acceptance:** US3 + US5 scenarios pass; SC-001/003/004/006 measured.

### Phase 2g — Safe read-only SQL fallback (SAFE_QUERY) · FR-016 (see plan §4.6)
- [x] T036 `SafeSqlValidator` + `ModuleQuerySchema`/`TableSchema`/`ColumnSchema` (commonMain,
      `feature/agent/query/`): SELECT-only, single-statement, no-comments, keyword-deny, table-allowlist
      (+ CTE), LIMIT-enforce → `Valid`/`Rejected`. Unit-tested (`SafeSqlValidatorTest`, SC-009).
- [x] T036b `BuiltInQuerySchemas` — curated read-only schemas for invoice/customer/product/inventory,
      derived from the Room `@Entity`s (internal/sync/JSON columns hidden) + business descriptions for
      text-to-SQL. Tested (`BuiltInQuerySchemasTest`). NOTE: move to per-module ownership with T037 so
      they can't drift from the entities.
- [x] T037 Per-module `ModuleQueryExecutor` + `ModuleQuerySchema` (WorkspaceScope), fully per-module
      owned. Executors run validated SQL via Room KMP `useReaderConnection { usePrepared(sql) {
      SQLiteStatement } }` on a **reader** connection → `QueryResultSet`. Each of invoice/customer/
      product/inventory contributes both its `@QueryExecutorKey` executor and its `@Provides @IntoMap
      @QuerySchemaKey` curated schema (types in `data/common/agent`). `SafeQueryExecutorModule`
      `@Multibinds(allowEmpty=true)` both maps; `SafeQueryService` reads them directly —
      `BuiltInQuerySchemas` retired. Remaining: device-verify a real read-only round-trip.
- [~] T038 `ResolvedIntent.SafeQuery(moduleName, sql)` + `SafeQueryService` (validate via curated
      schema + `SafeSqlValidator` → execute on the module's read-only `ModuleQueryExecutor` →
      `SafeQueryOutcome` rendered to chat). `AgentOrchestrator` routes the `SafeQuery` intent through
      it. `ModuleQueryExecutor`/`QueryResultSet`/`QueryExecutorKey` contract added in `data/common`;
      empty executor map allowed via `@Multibinds(allowEmpty = true)` until T037 contributes per-module
      executors. Remaining: the LLM resolver must *emit* `SafeQuery` (Phase 2) and the `AssistantConfig`
      opt-in gate (T021) — until then no resolver produces it.
- [x] T039 Tests: `SafeQueryServiceTest` proves the validation gate (mutating/multi-statement/comment/
      off-allowlist SQL all `Rejected`, executor never called — SC-009 read-only proof), unknown-module
      vs no-executor `Unavailable`, executor failure → `Failed`, and valid SELECT executes with an
      enforced LIMIT. Orchestrator routing covered in `AgentOrchestratorTest`.

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
- [x] T043 Tax/total via the tax module calculator. `InvoiceActionHandler.applyTaxes()` mirrors
      `InvoiceViewModel.computeTotals()`: resolves the scenario from the buyer GSTIN
      (`ScenarioResolver`, seller-origin state still unset like the editor), reads the workspace
      `prices_include_tax` price mode, resolves rates via `TaxRateProvider`, and runs
      `DocumentTotalsCalculator` → per-item `taxInfos`/`totalTax` + invoice `basePrice`/`totalTax`/
      `totalCost` (GST-correct grand total). Applied in both propose (Confirm summary) and persist, so
      the assistant total matches what the editor shows on open.
- [x] T044 Added `ActionResult.Confirm(summary, pendingAction)` + `CONFIRMED_PARAM` to the shared
      contract (`data/common/.../agent/`). `InvoiceActionHandler.CREATE` is now **two-phase**:
      `proposeInvoice` resolves customer/product, builds the draft **in memory** to compute the total,
      and returns `Confirm` with the resolved ids in `pendingAction` (**no persistence**, FR-006);
      `persistInvoice` (on the confirmed re-dispatch) rebuilds from those ids and saves. The total
      rides as a structured `amount` (not baked into the summary) so the UI renders it via
      `formatMoney(LocalAppLocale.current)` in the workspace currency (see T006).
- [x] T045 Confirm/cancel handling: `AgentOrchestrator.confirmAction(pendingAction)` re-dispatches the
      confirmed action; `AgentResponse.pendingConfirmation` surfaces it. `ChatViewModel` carries
      `pendingConfirmation` across turns with `confirmPending()` / `cancelPending()`; on confirm →
      handler's `persistInvoice` → `InvoiceRepository.saveInvoice(...)` (offline-first → pending push).
- [~] T046 [P] `ChatScreen` renders a `ConfirmActionBar` (Confirm/Cancel) when a confirmation is
      pending. TTS read-back of the total is deferred to the Phase 1/4 voice wiring (FR-009).
- [~] T047 [P] Tests: `AgentOrchestratorTest` proves the confirm flow — first turn returns `Confirm`
      and persists nothing (FR-006), `confirmAction` persists exactly once, plain queries carry no
      pending confirmation. The 20-utterance create set (SC-002) and restart→push (SC-007) e2e remain
      (need device/heavy repo fakes). FR-015 (only persisted actions report success) holds via the
      `success = result is ActionResult.Success` mapping (Confirm/Error/NeedsInput → not success).
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
- [~] T054 Docs: `docs/features/AGENT_OFFLINE_ASSISTANT.md` captures the implemented architecture,
      what's built (commonMain, CI-green) + the native extension seams + pending device tasks +
      SC status. Remaining: final SC-001…SC-009 verification (needs on-device measurement, T034/T047).

---

## Cross-cutting compliance checklist (apply to every phase)

- [ ] No `java.*`/`android.*` imports in `commonMain`; iOS uses `Dispatchers.Default`.
- [ ] ViewModels via Metro (`@ContributesIntoMap` + `@ViewModelKey`); no `LocalAppGraph.current` in composables.
- [ ] Repositories local-only; the assistant never calls feature APIs directly (sync via `CentralSyncService`).
- [ ] Money actions confirmed before persist; UIDs generated in VM/handler, never repository.
- [ ] User-visible strings from Compose resources; amounts via `formatMoney(..., LocalAppLocale.current)`.
- [ ] New deps only via `gradle/libs.versions.toml`; models downloaded at runtime, not bundled.
- [ ] All three targets compile after any `commonMain` change.
