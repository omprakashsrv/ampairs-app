# Offline AI Assistant — implementation status & architecture

Spec: `specs/002-offline-ai-assistant/` (spec.md / plan.md / tasks.md). This doc captures **what is
built today** (all `commonMain`, unit-tested, CI-green on Android + iOS + Desktop) and the **seams**
the device-side native tier plugs into. Companion: `AGENT_LITERTLM_REFERENCE.md` (LiteRT-LM recipe).

---

## Pipeline

```
mic ─▶ SpeechToText* ─▶ ChatViewModel ─▶ AgentOrchestrator
text ───────────────▶                     ├─ OnlineIntentResolver  (cloud, optional)*
                                          ├─ OfflineIntentResolver = CompositeOfflineResolver
                                          │     ├─ LlmIntentResolver  (on-device, via engineProvider*)
                                          │     └─ RuleBasedIntentResolver  (regex fallback)
                                          ├─ ActionRegistry.dispatch(AgentAction) ─▶ per-module ActionHandler
                                          └─ SafeQueryService  (read-only SQL fallback, FR-016)
spk ◀─ TextToSpeech* ◀─ ChatViewModel
```
`*` = device/native, not yet implemented (see “Pending”).

## Implemented (commonMain, tested)

| Area | Key types | Notes |
|---|---|---|
| Action dispatch | `ActionRegistry` (Metro `Map<String, ActionHandler>`), per-module `*ActionHandler` | `@ContributesIntoMap(WorkspaceScope)` + `@ActionHandlerKey` |
| Orchestration | `AgentOrchestrator`, `AgentResponse` | routes Action / Conversation / Clarification / **SafeQuery** / Error; `confirmAction()` for the confirm turn |
| Confirm-before-persist (FR-006) | `ActionResult.Confirm(summary, pendingAction, amount)`, `CONFIRMED_PARAM` | invoice CREATE is two-phase (propose → persist); nothing writes before confirm |
| Money totals (FR-013) | `ActionResult.amount` → `AgentResponse` → `ChatMessage` → `MessageBubble` | rendered with `formatMoney(amount, LocalAppLocale.current)` |
| GST | `InvoiceActionHandler.applyTaxes()` | mirrors `InvoiceViewModel.computeTotals()` (TaxRateProvider + DocumentTotalsCalculator) |
| On-device NLU | `LlmIntentResolver`, `CompositeOfflineResolver` | strict validate → Clarification on unregistered/missing-param (SC-003); LLM-when-ready else rule-based |
| LLM ports | `LlmEngine`, `LlmBackend`, `ModelDescriptor`, `LlmParams`, `OutputSchema` (`toJsonSchema`/`toGbnf`) | engine-agnostic |
| Schema/prompt | `AgentSchemaBuilder` | ActionDescriptors → OutputSchema + system prompt |
| Catalog/config | `ModelCatalog` (FunctionGemma-270m / Gemma 3n E2B-E4B / Qwen2.5-3B), `AssistantConfig` | `safeQueryEnabled` defaults off |
| SAFE_QUERY (FR-016) | `SafeSqlValidator`, `BuiltInQuerySchemas`, `SafeQueryService`, `ModuleQueryExecutor` | SELECT-only, single-module, read-only; bad SQL never reaches the executor (SC-009) |
| Module install | `ModuleCodes.AI_ASSISTANT`, `DynamicModuleNavigationService`, `AppBottomNavigation`, `agentEntryProvider` | backend catalog seed in `ampairs` `MasterModuleSeederService` |
| Strings/locale | `feature/agent/.../composeResources/values/strings.xml` (`agent_*`) | all UI text via `stringResource`/`getString` |

## Extension seams for the native tier (already in place)

- **LLM engine:** implement `LlmBackend`/`LlmEngine` (LiteRtLmEngine, LlamaCppEngine), contribute the
  backend to the Metro `Set<LlmBackend>`. `ProviderRegistry` (T023) then supplies
  `LlmIntentResolver.engineProvider` and `CompositeOfflineResolver.isLlmReady`, and binds the composite
  to `@OfflineIntentResolver` (replacing the current rule-based binding in `AgentModule`).
- **SAFE_QUERY execution:** implement `ModuleQueryExecutor` per module (Room `@RawQuery` on a reader
  connection), contribute with `@ContributesIntoMap(WorkspaceScope) @QueryExecutorKey("<module>")`. The
  empty map is already allowed via `@Multibinds(allowEmpty = true)` (`SafeQueryExecutorModule`).
- **Capability gating:** add `PlatformDefaults` + `DeviceCapability` (expect/actual) — RAM tiers are
  specified in plan Open-Q#4 and encoded in the T022 task note.
- **Voice:** `expect`/`actual` `SpeechToText`/`TextToSpeech`; wire `ChatViewModel.onVoiceResult` + TTS.

## Pending (require a device/native build — cannot be verified in CI alone)

Phase 1 voice (T011–T019) · `PlatformDefaults`/`DeviceCapability` (T022) · `ProviderRegistry` (T023) ·
LiteRT-LM/llama.cpp engines (T024–T027) · model verification (T031) · `ModelManager` download (T032) ·
model/engine UI (T033) · device eval for SC-001/003/004/006 (T034) · per-module `SqlQueryDelegate`
(T037) · TTS read-back (T046) · Phase 4 polish (T050–T053).

## Success criteria status

SC-008 (3-target compile) ✅ continuously green. SC-009 (SAFE_QUERY rejects 100% bad SQL, 0 writes) ✅
unit-proven. SC-002/005/007 partially proven at the orchestrator level (`AgentOrchestratorTest`); the
20-utterance + restart-and-push e2e and SC-001/003/004/006 require on-device measurement (T034/T047).
