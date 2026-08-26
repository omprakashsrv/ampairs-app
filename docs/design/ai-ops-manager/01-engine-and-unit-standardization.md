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
`@ContributesIntoMap(WorkspaceScope::class) @CapabilityKey("product.unit")`. The **engine runner**
(`AiOpsRunner`, `@Inject`, WorkspaceScope) wires the fixed flow and the gate.

---

## 3. The `product.unit` capability (this slice)

- **Detector** — read products via `WorkspaceDatabaseProvider` reader connection (the same handle the
  agent SafeQuery path uses — do **not** depend on `:feature:product` impl); flag any product whose unit
  string isn't a canonical unit for the workspace.
- **ContextGatherer** — load canonical units (`UnitService` / unit tables) + the bundled alias table.
- **CandidateGenerator** — deterministic alias map → `UPDATE_FIELD before="Kgs" after="KG"`. No LLM.
- **CandidateValidator** — assert `after` is a real canonical unit in this workspace.
- **ConfidenceScorer** — deterministic alias hit ⇒ `value≈0.999, band=HIGH, contributors={rule:1.0}`.
  (Ensemble contract still applies; this capability just happens to be rule-only.)

Ambiguous case (`unit="L"` on "500 ML bottle") → `Reasoner` would parse it, but for slice 01 we simply
emit **MEDIUM → suggestion chip**, deferring LLM wiring to slice 2+.

---

## 4. Ports — app adapters

- **`Executor` (write path):** applies `UPDATE_FIELD` by calling the **product repository's** update so
  the write is `synced=false` and `CentralSyncService` pushes via `ProductSyncDelegate` (`/offline-sync`).
  Because `feature/aiops` must not depend on `feature/product` impl, the product-side Executor is
  **contributed from `feature/product`** into the WorkspaceScope map (`@CapabilityKey("product.unit")`)
  — exactly the QueryExecutor split the agent module already uses. *(Open: confirm the product repo
  exposes a field-update that flags `synced=false`; if not, add a thin one.)*
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

- **Implicit on-save:** after a product create/edit, the ViewModel asks `AiOpsRunner` to run
  `product.unit` on that entity. On an L2 auto-fix → a subtle snackbar **"AI set unit → KG · Undo"**;
  on MEDIUM → an inline **suggestion chip** (Approve / Ignore). Keep the runner call off the UI thread;
  never block save on it.
- **No new nav.** This slice adds only the inline surface; a full "review inbox" screen is a later slice.

---

## 8. DI wiring (Metro, WorkspaceScope) — checklist
- [ ] `feature/aiops` DB `@Provides @SingleIn(WorkspaceScope::class)` + `closableRegistry.register`.
- [ ] Engine runner, DAOs, capability stages: `@Inject`, unscoped; capability + executor into
      `@ContributesIntoMap(WorkspaceScope::class)` maps with `@CapabilityKey`.
- [ ] `feature/product` contributes the `product.unit` Executor (write via its repo).
- [ ] `Reasoner` adapter stub contributed (real wiring slice 2).

## 9. Tests (DoD)
- Engine runner unit test with a fake capability (deterministic).
- `product.unit` stage tests: alias detection, validator rejects unknown unit, scorer bands.
- Gate tests: L1 never auto-fixes; L2 auto-fixes HIGH/low-risk, routes MEDIUM to suggestion.
- Undo test: apply → revert restores prior unit + writes feedback.
- **Compile all 3 targets** (`androidApp:compileDebugKotlinAndroid`,
  `shared:compileKotlinIosSimulatorArm64`, `desktopApp:compileKotlin`).

## 10. Open points (resolve in review)
1. Executor field-update on the product repo flags `synced=false`? (else add a thin method).
2. Home for the `Reasoner`-over-`LlmEngine` adapter (`feature/agent` vs `shared`) without an impl-dep cycle.
3. ✅ **Decided:** `aiops_*` live in a **dedicated `feature/aiops` workspace Room DB** (own module DB
   per the `/metro-di` workspace-DB checklist) — not the consolidated `AmpairsAppDatabase`.
4. Autonomy-level storage key + default surface in settings UI.
