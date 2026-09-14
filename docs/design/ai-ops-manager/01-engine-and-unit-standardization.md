# Slice 01 — Shared AI-Ops engine + unit standardization

**Epic:** 1 (app) · **Status:** Draft design (no code) · **Proves:** the whole machine end-to-end —
detect → candidate → gate → auto-fix → sync → audit → undo.
**Program:** `ampairs/docs/ai-ops-manager/README.md` · **Engine design:** `.../framework.md` ·
**Decisions:** ADR 0005 (app-side), ADR 0006 (shared KMP engine).

This slice stands up the reusable engine as a KMP module and ships **one** capability —
**unit standardization** (`Kgs`/`Kg`/`Kilo` → `KG`, `LTR`/`Litres` → `L`) — because it is the highest
confidence, lowest risk, and needs no LLM, so it isolates the *engine* from *reasoning*.

---

## 1. Module & placement

New KMP module **`feature/aiops`** (add to `settings.gradle.kts`), engine in `commonMain` only —
**no `java.*`/`android.*`/Koog** in the SPI (ADR 0006; `/cmp-practices` KMP rules).

```
feature/aiops/src/commonMain/kotlin/com/ampairs/aiops/
├── engine/            # AiOpsRunnerImpl (the fixed pipeline); SPI itself is in data/common (landed)
├── gate/              # ConfidenceRiskGate + autonomy read
├── di/                # Metro WorkspaceScope wiring (runner, gate, maps)
└── ui/                # suggestion chip + "AI fixed … · Undo" surface (composable)
# NOTE: the aiops_* Room tables + AiOpsDao live in data/database (consolidated DB, §6),
# NOT here — feature/aiops depends on projects.data.database to inject AiOpsDao.
```

**Reuse, don't reinvent** — this mirrors the existing agent extension pattern
(`feature/agent` QueryExecutor/QuerySchema maps, see the app's agent-models memory): capabilities and
per-module executors are contributed into Metro maps, so adding a capability never edits the engine.

---

## 2. The engine SPI (`commonMain`)

Host-agnostic (ADR 0006). Ports are filled by app adapters (§4).

```kotlin
data class Finding(val id: String, val capability: String, val entityType: String,
                   val entityId: String, val field: String?, val summary: String,
                   val signals: Map<String, String>)
data class Candidate(val field: String?, val before: String?, val after: String?,
                     val action: ActionType, val rationale: String, val evidence: List<String>)
enum class ActionType { UPDATE_FIELD, MERGE, LINK, DEACTIVATE, CREATE, SPLIT, NO_OP }

fun interface Detector           { suspend fun detect(scope: Scope): List<Finding> }
fun interface ContextGatherer    { suspend fun gather(f: Finding): FindingContext }
fun interface CandidateGenerator { suspend fun propose(f: Finding, ctx: FindingContext): List<Candidate> }
fun interface CandidateValidator { suspend fun validate(f: Finding, c: Candidate, ctx: FindingContext): Validation }
fun interface ConfidenceScorer   { suspend fun score(f: Finding, c: Candidate, ctx: FindingContext): Confidence }

// Ports the app host implements:
fun interface Reasoner { suspend fun <T> structured(req: ReasonRequest<T>): T }   // → LlmEngine (unused in slice 01)
fun interface Executor { suspend fun apply(c: Candidate, f: Finding): ExecResult } // → repo write + offline-sync

data class Confidence(val value: Double, val band: Band, val contributors: Map<String, Double>)
enum class Band { HIGH, MEDIUM, LOW }
```

A **capability** = a keyed set of the five stage plug-ins. Contributed via Metro:
`@ContributesIntoMap(WorkspaceScope::class) @CapabilityKey("unit.shortname")`. The **engine runner**
(`AiOpsRunner`, `@Inject`, WorkspaceScope) wires the fixed flow and the gate.

---

## 3. The `unit.shortname` capability (this slice)

**Correction (grounding, 2026-09):** the first-planned `product.unit` capability assumed a product's
unit was a **free-text** field to rewrite. It isn't — `ProductEntity.base_unit` stores a **unit UID
(foreign key)** into the `units` table (`ProductFormViewModel` sets it from the picked unit's `uid`;
`ProductSyncDelegate` uses it as a map key). Rewriting `base_unit` to a text code like `"KG"` would
**corrupt the FK** and never match anyway. The genuinely inconsistent free-text field is the **unit
record's own `short_name`** (`Kgs` / `Kg` / `Kilo` for the same unit). So the slice-1 capability
standardizes `units.short_name`, lives in **`feature/unit`**, and triggers on **unit save**.

- **Detector** — read active units via `UnitRepository.getActiveUnits()` (the capability lives in
  `feature/unit`, so it uses that module's own repository/DAO — no cross-feature reach); flag any unit
  whose `short_name` is a known alias whose canonical spelling differs.
- **ContextGatherer** — load the unit's current `short_name`/`name` (bundled alias table is static).
- **CandidateGenerator** — deterministic `UnitAliasCatalog` → `UPDATE_FIELD before="Kgs" after="KG"`. No LLM.
- **CandidateValidator** — assert `after` is a declared canonical and actually changes the value.
- **ConfidenceScorer** — deterministic alias hit ⇒ `value≈0.999, band=HIGH, contributors={alias_catalog:1.0}`.
  (Ensemble contract still applies; this capability just happens to be rule-only.)

An unknown spelling → no finding (no LLM this slice; ambiguous parsing defers to the `Reasoner` in
slice 2+).

---

## 4. Ports — app adapters

- **`Executor` (write path):** applies `UPDATE_FIELD` by calling `UnitRepository.updateUnit(...)` so the
  write is `synced=false` and `CentralSyncService` pushes via `UnitSyncDelegate` (`/offline-sync`).
  Because `feature/aiops` must not depend on other feature impls, both the capability and its Executor are
  **contributed from `feature/unit`** into the WorkspaceScope maps (`@CapabilityKey`/`@AiOpsExecutorKey`
  `"unit.shortname"`) depending only on the `com.ampairs.common.aiops` contracts — exactly the
  QueryExecutor split the agent module already uses. The same Executor serves rollback (undo passes a
  candidate whose `after` is the original value).
- **`Reasoner`:** an adapter over `feature/agent`'s `LlmEngine` (`feature/agent/.../llm/LlmEngine.kt`),
  contributed where `LlmEngine` is visible (`feature/agent` or `shared`). **Not used in slice 01** —
  stub it and wire for real in slice 2.

---

## 5. Gate & autonomy

`autoFix ⇔ confidence.band=HIGH ∧ risk=LOW ∧ candidate.reversible`, evaluated against the workspace
**autonomy level** (default **L1 Recommend** → never auto; **L2** → auto-fix low-risk). Autonomy is a
DataStore/setting value (reuse the existing preferences; do **not** create a new DataStore). `unit` is
low-sensitivity/reversible → eligible at L2.

---

## 6. Data model & audit (local now, sync later) — CORRECTED to the consolidated DB

**Correction (grounding, 2026-08):** the app **consolidated every workspace table into one Room DB**,
`data/database/AmpairsWorkspaceDatabase` (currently **version 5**). Feature modules no longer own a
`@Database` — their entities/DAOs physically live in `data/database` (e.g.
`com.ampairs.unit.data.db.entity.UnitEntity`, `com.ampairs.product.db.dao.ProductDao`) and features
depend on `projects.data.database` to inject them. A *dedicated* `feature/aiops` DB is therefore **not
viable** (a DAO may have only one Room-generated impl per classpath; the per-feature DB classes were
deleted). This supersedes the earlier "dedicated aiops workspace DB" decision.

So the `aiops_*` tables are **added to the consolidated DB** (mirror `UnitEntity`/`UnitDao`):
- New `AiOpsFindingEntity`, `AiOpsDecisionEntity` (before/after/confidence/reversible/source),
  `AiOpsFeedbackEntity` + `AiOpsDao` under `data/database/.../com/ampairs/aiops/db/{entity,dao}`.
- Register the three entities in `AmpairsWorkspaceDatabase.entities`, add `abstract fun aiOpsDao()`,
  bump `version = 5 → 6`, add `WorkspaceMigration5To6` (`CREATE TABLE aiops_*`, additive) and wire it
  into the DB builder's migration list; provide `AiOpsDao` from `WorkspaceDatabaseDaoModule`.
- `Clock.System.now()` epoch-millis timestamps; `@ColumnInfo` snake_case; `exportSchema` JSON regen.
- `feature/aiops` (and the runner) `implementation(projects.data.database)` to inject `AiOpsDao` — the
  same way every feature gets its DAOs.

**Undo** reads the `aiops_decision`, re-applies `before` through the same Executor, records
`aiops_feedback(REJECT)`. **Audit is local-only this slice** — syncing it to the server is Epic-2 (needs
the backend endpoint); add a `SyncEntity.AIOPS_*` + delegate then, not now.

---

## 7. Trigger & UX (slice 01)

- **Implicit on-save:** after a unit create/edit, `UnitFormViewModel` asks `AiOpsRunner` to run
  `onEntitySaved("unit", uid)`, which **returns an `AiOpsOutcome`**. At L2 a high-confidence alias
  auto-fixes → the VM emits a `UnitFormEvent.AiOpsShortNameFixed` and the screen shows a snackbar
  **"AI standardized short name to KG"** with an **Undo** action (→ `AiOpsUndo.undo(decisionId)`); at
  L1 a `PENDING_REVIEW` finding surfaces as a lighter suggestion snackbar. The runner is best-effort
  (`runCatching`) and never blocks or fails the save.
- **Undo port:** `AiOpsUndo` (data/common) is bound by `AiOpsUndoService` (`@ContributesBinding`
  WorkspaceScope) so the unit UI can roll back without depending on the `feature/aiops` impl.
- **No new nav.** This slice adds the on-save hook + audit + the inline snackbar/Undo; a full "review
  inbox" screen and an always-visible suggestion chip are a later increment.

---

## 8. DI wiring (Metro, WorkspaceScope) — checklist
- [x] `aiops_*` tables in the consolidated DB + `AiOpsDao` via `WorkspaceDatabaseDaoModule` (see §6).
- [x] Engine runner `@ContributesBinding(WorkspaceScope)` for `AiOpsRunner`; gate/undo `@Inject`;
      `@Multibinds(allowEmpty=true)` capability + executor maps in `feature/aiops`.
- [x] `feature/unit` contributes the `unit.shortname` capability **and** Executor (write via its repo).
- [x] `AiOpsSettings` port (data/common) backed by `AppPreferencesDataStore` in `feature/aiops`.
- [x] `AiOpsRunner.onEntitySaved` returns `AiOpsOutcome`; `AiOpsUndo` port bound by `AiOpsUndoService`;
      unit form shows the auto-fix/Undo snackbar.
- [ ] `Reasoner` adapter stub contributed (real wiring slice 2).

## 9. Tests (DoD)
- Engine runner unit test with a fake capability (deterministic) — autonomy L0/L1/L2, filtering, audit link.
- `unit.shortname` stage tests: alias detection, validator rejects unknown/unchanged, scorer band; plus
  `UnitAliasCatalog` pure tests.
- Gate tests: L1 never auto-fixes; L2 auto-fixes HIGH/low-risk/reversible, else suggestion.
- Undo test: apply → revert restores prior value + writes REJECT feedback.
- **Compile all 3 targets** (`androidApp:compileDebugKotlinAndroid`,
  `shared:compileKotlinIosSimulatorArm64`, `desktopApp:compileKotlin`).

## 10. Open points (resolve in review)
1. Executor field-update on the product repo flags `synced=false`? (else add a thin method).
2. Home for the `Reasoner`-over-`LlmEngine` adapter (`feature/agent` vs `shared`) without an impl-dep cycle.
3. ✅ **Decided:** `aiops_*` live in a **dedicated `feature/aiops` workspace Room DB** (own module DB
   per the `/metro-di` workspace-DB checklist) — not the consolidated `AmpairsAppDatabase`.
4. Autonomy-level storage key + default surface in settings UI.
