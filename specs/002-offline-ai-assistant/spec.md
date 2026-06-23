# Feature Specification: Offline AI Assistant (Text & Voice Agentic Chat)

**Feature Branch**: `claude/nice-gauss-0ndtbn`

**Created**: 2026-06-23

**Status**: Draft

**Input**: User description: "Text or voice based chat bot on the App side, fully offline, that can answer business queries and create bills/invoices automatically using voice commands. Use offline models like Gemma or others on app/desktop. Determine what capabilities can be supported."

**Design reference**: A reviewed technical plan exists at `specs/002-offline-ai-assistant/plan.md` (the HOW). This spec captures the WHAT/WHY. Background research lives in the session report; the existing agent scaffold is in `feature/agent/` and `data/common/src/commonMain/kotlin/com/ampairs/common/agent/`.

---

## Context: what already exists

The app already ships a half-wired **agentic scaffold** that this feature completes and extends:

- Contracts in `data/common/.../agent/`: `AgentAction` (serializable `{actionType, moduleName, params}`), `ActionType`, `ActionDescriptor`/`ActionParameter` (self-describing tool metadata), `ActionResult` (Success / Error / **NeedsInput**), `ActionHandler`, `ActionHandlerProvider`.
- Pipeline in `feature/agent/core/`: `AgentOrchestrator` → `IntentResolver` (online/offline) → `ActionRegistry.dispatch()` → per-module `ActionHandler`. `ChatViewModel` + `ChatScreen` + `VoiceInputButton`.
- Per-module handlers: customer (CRUD+sync), product (CRUD), order/invoice/inventory (read-only).
- `RuleBasedIntentResolver` (regex) for offline command parsing.

**Known gaps this feature must close** (all confirmed in code):

1. `ActionRegistry.register(...)` is never called → registry is empty at runtime → no action ever executes.
2. Both online and offline resolvers are bound to `RuleBasedIntentResolver` → there is no LLM, on-device or cloud.
3. Voice is UI-only: `VoiceInputButton` toggles state; `ChatViewModel.onVoiceResult()` is never called by any speech engine.
4. Invoice and order handlers are read-only — no CREATE path for "make a bill by voice".

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Answer business queries by text, fully offline (Priority: P1)

A shop owner with no internet connection opens the assistant and types or speaks everyday questions
about their own data — "how many customers do I have?", "show unpaid invoices", "which items are low
on stock?", "what did I sell today?" — and gets correct answers drawn entirely from the on-device
database.

**Why this priority**: This is the lowest-risk, highest-frequency value and is mostly unblocked by the
existing scaffold (read-only handlers + registry wiring). It proves the end-to-end loop with no
money-mutating risk.

**Independent Test**: Put the device in airplane mode, ask each of the five reference questions, and
confirm answers match what the corresponding list screens show.

**Acceptance Scenarios**:

1. **Given** the device is offline, **When** the user asks "how many invoices do I have?", **Then** the
   assistant returns the same count shown on the invoice list.
2. **Given** unpaid invoices exist, **When** the user asks "show me unpaid invoices", **Then** the
   assistant lists them (number, customer, amount, status) sourced from Room.
3. **Given** stock data exists, **When** the user asks "what's low on stock?", **Then** the assistant
   lists items at or below the low-stock threshold.
4. **Given** an ambiguous question the assistant cannot map to an action, **When** it cannot resolve an
   intent, **Then** it responds with a helpful prompt of example commands rather than failing silently.

---

### User Story 2 — Create a bill/invoice by voice, offline (Priority: P1)

A cashier, hands busy, says "make a bill for Ramesh — two boxes of widget and one cable". The
assistant transcribes the speech, resolves the customer and products against local data, builds a
draft invoice with correct line items and tax, **reads the total back, and asks for confirmation
before saving**. On "yes", the invoice is saved locally and will sync when online.

**Why this priority**: This is the flagship request. It exercises STT + NLU slot-filling + multi-entity
resolution + a money-mutating action, and is the feature's headline differentiator.

**Independent Test**: Offline, with at least one matching customer and two matching products, speak the
command, confirm at the prompt, and verify a draft invoice is created with the right lines, tax, and
total, and that it appears in the pending-sync queue.

**Acceptance Scenarios**:

1. **Given** a customer "Ramesh" and products "widget" and "cable" exist, **When** the user speaks the
   command, **Then** the assistant produces a draft invoice with the right customer, two line items at
   the right quantities, computed tax, and a spoken/written total.
2. **Given** a draft invoice is proposed, **When** the user has not yet confirmed, **Then** nothing is
   persisted; the assistant explicitly asks for confirmation.
3. **Given** the user confirms, **When** the invoice is saved, **Then** it is written to the local DB
   (`synced = false`) and flagged for push; no network call is required to create it.
4. **Given** a spoken product or customer cannot be uniquely matched, **When** resolution is ambiguous,
   **Then** the assistant asks a disambiguation question (`NeedsInput`) instead of guessing.
5. **Given** the user declines or says "cancel", **When** at the confirmation prompt, **Then** the
   draft is discarded and nothing is saved.

---

### User Story 3 — Natural-language understanding via an on-device model (Priority: P1)

A user phrases commands naturally and imperfectly — "add a new client called Priya Traders", "kitne
customer hain", "find that order from yesterday" — and the assistant understands intent and fills slots
without requiring exact keyword syntax, all on-device.

**Why this priority**: The current regex resolver is brittle; natural phrasing (and code-mixed/Hindi
input) is required for the experience to feel like an assistant rather than a command line. This is the
on-device LLM tier.

**Independent Test**: Offline, issue 20 paraphrased variants of the supported actions and confirm the
correct `AgentAction` is produced for ≥ the success target (see Success Criteria), with the model
output always being a structurally valid action (grammar-constrained).

**Acceptance Scenarios**:

1. **Given** the on-device model is available, **When** the user types a paraphrased command, **Then**
   the assistant maps it to the correct action and parameters.
2. **Given** any model output, **When** an intent is produced, **Then** it is always a structurally
   valid `AgentAction` (no malformed/hallucinated action types or modules).
3. **Given** the model cannot confidently resolve an intent, **When** confidence is low, **Then** the
   assistant asks a clarifying question rather than executing a wrong action.

---

### User Story 4 — Voice input and spoken responses (Priority: P2)

A user taps the mic, speaks, sees the transcription, and hears the assistant's answer read aloud —
on Android, iOS, and Desktop.

**Why this priority**: Voice is the requested modality but depends on US1–US3 working first; text-only
is a viable interim release.

**Independent Test**: On each platform, tap mic, speak a US1 query, confirm accurate transcription
feeds the pipeline and the answer is spoken back.

**Acceptance Scenarios**:

1. **Given** mic permission is granted, **When** the user speaks, **Then** the transcribed text is
   shown and submitted to the assistant.
2. **Given** an answer is produced, **When** TTS is enabled, **Then** the answer is read aloud and can
   be muted.
3. **Given** mic permission is denied, **When** the user taps mic, **Then** the app explains and falls
   back to text input gracefully.

---

### User Story 5 — Graceful online/offline behavior and model availability (Priority: P2)

The assistant works offline by default and transparently uses a higher-quality cloud path when online
and available. If the on-device model is not yet downloaded, the assistant still works in a reduced
(rule-based) mode and offers to download the model on Wi-Fi.

**Why this priority**: Large models cannot ship in the binary; the feature must degrade predictably and
not block the user.

**Independent Test**: With no model downloaded and offline, confirm rule-based mode still answers US1
queries; download the model on Wi-Fi; confirm US3 natural-language mode activates; go online and
confirm the cloud path is preferred with offline fallback.

**Acceptance Scenarios**:

1. **Given** no model is downloaded, **When** the user uses the assistant offline, **Then** rule-based
   mode handles supported commands and the UI offers a model download.
2. **Given** the device is online, **When** a query is sent, **Then** the cloud resolver is tried first
   and falls back to on-device on failure (existing orchestrator behavior).
3. **Given** a model download is in progress, **When** the user interacts, **Then** progress is visible
   and the assistant remains usable in reduced mode.

---

### Edge Cases

- Money actions (create/finalize invoice, delete) MUST require explicit confirmation; they MUST NOT
  auto-execute from a single voice utterance.
- Speech that resolves to no action returns guidance, never a silent no-op.
- Low-RAM devices that cannot run the chosen model fall back to rule-based mode (no crash).
- Multi-tenant safety: the assistant only ever reads/writes the **active workspace's** data; switching
  workspace must not leak data or reuse a stale model/session context.
- Numbers spoken as words ("two", "do") must map to quantities; currency must use the workspace
  business locale, never a hardcoded symbol.
- A confirmed invoice created offline must survive app restart and still push on reconnect.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST populate `ActionRegistry` from all module `ActionHandler`s so dispatched
  actions actually execute.
- **FR-002**: The assistant MUST answer read-only business queries (count, list, search, read, get
  inventory, low-stock) from the on-device database for customer, product, order, invoice, and
  inventory modules, with no network dependency.
- **FR-003**: The system MUST provide an on-device intent resolver that converts natural-language text
  into a structurally valid `AgentAction` using a constrained-decoding (grammar/JSON-schema) mechanism
  derived from the registry's action metadata.
- **FR-004**: The system MUST retain the rule-based resolver as a fallback when the on-device model is
  unavailable or not yet downloaded.
- **FR-005**: The system MUST support creating a draft invoice from natural language, resolving the
  customer and product line items against local data, computing tax via the existing tax logic, and
  building the invoice through the existing `InvoiceRepository.saveInvoice(...)` path (offline-first;
  `synced = false` + pending push).
- **FR-006**: Money-mutating actions (create/finalize invoice, delete) MUST present a confirmation step
  with a human-readable summary (and total) before persisting.
- **FR-007**: When an entity (customer/product) cannot be uniquely resolved, the system MUST ask a
  disambiguation question via `ActionResult.NeedsInput` rather than guess.
- **FR-008**: The system MUST provide speech-to-text on Android, iOS, and Desktop, feeding transcribed
  text into the existing chat pipeline (wiring `ChatViewModel.onVoiceResult`).
- **FR-009**: The system MUST provide optional text-to-speech for assistant responses, with a mute
  control, on all three platforms.
- **FR-010**: The system MUST operate strictly within the active workspace's data and use a fresh
  model/conversation context per workspace session (no cross-workspace leakage).
- **FR-011**: On-device models MUST be delivered via on-demand download (not bundled in the binary),
  with visible progress, Wi-Fi gating by default, and storage in app-private storage.
- **FR-012**: The system MUST select an appropriate model size (or disable LLM mode) based on device
  capability (RAM), degrading to rule-based mode when the model cannot run.
- **FR-013**: All assistant-rendered amounts MUST use the workspace business locale formatting; all
  user-visible strings MUST come from Compose resources.
- **FR-014**: The online path MUST be tried first when connected and fall back to on-device on failure
  (preserve existing `AgentOrchestrator` behavior).
- **FR-015**: The assistant MUST never report success for an action that did not persist; failures and
  "needs input" states MUST be surfaced to the user.
- **FR-016**: When no typed action matches, the assistant MAY fall back to a **read-only SAFE_QUERY**
  over a single module's local database. It MUST be opt-in (config-gated), restricted to `SELECT`
  against a **curated per-module schema allowlist**, validated before execution (single statement, no
  comments, no DDL/DML, allow-listed tables only, enforced row `LIMIT`), and executed on a **read-only**
  connection. SAFE_QUERY MUST NOT mutate data (all writes go through typed handlers) and MUST NOT span
  modules in a single statement (separate SQLite DBs).

### Capability Scope (what offline models will and won't do) *(informative)*

- **In scope (on-device):** intent classification + slot-filling for the supported CRUD/query actions;
  short conversational phrasing of query results; clarification dialogs; spoken bill creation with
  confirmation.
- **Out of scope (route to cloud when online, otherwise decline):** complex multi-period financial
  analytics, high-accuracy GST edge-case reasoning, long free-form report generation, and any task
  requiring large context or high factual precision beyond local structured data.

### Key Entities *(data involved)*

- **AgentAction**: the structured command (actionType + module + params) produced by a resolver and
  executed by a handler. The on-device model's constrained output target.
- **ActionDescriptor**: per-action metadata used to build the model prompt and the output grammar.
- **InvoiceDraft** (new, transient): the proposed invoice (customer ref, resolved line items,
  quantities, computed tax/total) held pending user confirmation; becomes `InvoiceEntity` +
  `InvoiceItemEntity` on confirm.
- **ModelAsset** (new): a downloadable on-device model (id, size, RAM requirement, local path,
  download state) for LLM and STT.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: With the device offline, the assistant correctly answers ≥ 90% of a 30-question reference
  set of supported business queries (US1).
- **SC-002**: A spoken "create a bill for {customer} with {items}" command produces a correct draft
  invoice (right customer, items, quantities, total) in ≥ 85% of a 20-utterance test set, and **0%** of
  invoices are persisted without explicit confirmation (US2 / FR-006).
- **SC-003**: The on-device resolver emits a structurally valid `AgentAction` for **100%** of inputs
  (grammar-constrained), and maps paraphrased commands to the correct action for ≥ 85% of a 20-variant
  set (US3).
- **SC-004**: Median time from end-of-speech to assistant response is ≤ 3 s for query actions on a
  mid-range device (4–8 GB RAM) using the default on-device model.
- **SC-005**: STT, intent resolution, and invoice creation all complete with the device in airplane
  mode for the supported flows (no network dependency).
- **SC-006**: On a device that cannot run the LLM, the assistant still answers US1 queries in
  rule-based mode without crashing.
- **SC-007**: After creating an invoice offline and restarting the app, the invoice persists and pushes
  to the server on reconnect.
- **SC-008**: All three targets compile (`androidApp:compileDebugKotlinAndroid`,
  `shared:compileKotlinIosSimulatorArm64`, `desktopApp:compileKotlin`).
- **SC-009**: The SAFE_QUERY validator rejects **100%** of non-`SELECT` / multi-statement / commented /
  off-allowlist / mutating inputs, and **0** rows are ever written via the SQL path (read-only).

---

## Assumptions

- Existing read-only handlers (order/invoice/inventory) and CRUD handlers (customer/product) are
  correct and reused as-is; only invoice/order gain a guarded CREATE path.
- The offline-first sync architecture (`CentralSyncService`, repository + `SyncDelegate`) is the only
  persistence/push mechanism the assistant uses; the assistant never calls feature APIs directly.
- A cloud LLM path (e.g. via the backend) may be added as the online resolver, but the feature's
  acceptance does not depend on it — offline is the contract.
- On-device engine and model are **pluggable** (ports & adapters): primary engine **LiteRT-LM** (Google
  AI Edge — Kotlin on Android/Desktop, Swift on iOS) with **llama.cpp** fallback; default model **Gemma 4
  E4B** with low-RAM (Gemma 4 E2B / Gemma 3 1B) and llama.cpp/compat (Qwen2.5-3B) tiers. These are
  `plan.md` decisions and swappable by config; this spec stays engine-agnostic.
