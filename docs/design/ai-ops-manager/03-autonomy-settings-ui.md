# 03 — Autonomy settings UI (unlocking auto-fix)

**Status:** implemented. **Purpose:** give the user a way to actually choose the workspace autonomy
level. Until now the level was stored (default **L1 Recommend**) and read by the engine, but nothing
in the app could change it — so the auto-fix path (L2) could never fire in production. This screen
turns the already-built engine + capabilities from "inert at the default" into a usable feature.

## What it does

A settings screen (**More → AI Automation**) shows the three **app-tier** autonomy levels as a
single-choice list, each with a plain-language description:

| Level | Label | Effect |
|---|---|---|
| L0 `OBSERVE` | Observe only | Detect issues, never change anything. |
| L1 `RECOMMEND` (default) | Recommend | Suggest fixes for review; nothing changes automatically. |
| L2 `AUTO_CORRECT` | Auto-correct | Auto-apply safe, high-confidence, reversible fixes (logged + undoable). |

L3/L4 (`AUTO_EXECUTE`/`AUTONOMOUS`) are **backend-managed tiers** and are deliberately not
user-selectable on device — a footnote says so, and `AiOpsSelectableLevelsTest` pins the exact set so
they can't leak into the picker.

## How it wires in

- **Port (`data/common`):** `AiOpsSettings` gained one method — `suspend fun setAutonomyLevel(level)`
  — alongside the existing reactive `autonomyLevel()`. Still a 2-method port (one-fake testable); the
  runner only reads, the screen also writes. Backed by `AiOpsSettingsImpl` →
  `AppPreferencesDataStore.setAiOpsAutonomyLevel` (the shared DataStore, never a new instance).
- **Feature (`feature/aiops/ui/settings`):** `AiOpsSettingsViewModel`
  (`@ContributesIntoMap(AppScope)` — the level is one app-wide preference) exposes
  `level: StateFlow` + `setLevel(...)`; `AiOpsSettingsScreen` renders the radio list;
  `AiOpsSettingsRoute` is the NavKey. Levels live in the pure top-level `AiOpsSelectableLevels`.
- **Shared nav:** `aiOpsEntryProvider` maps the route → screen (chained in `CombinedEntryProvider`);
  `MoreScreen` adds an "AI Automation" row. Mirrors the notification-settings route pattern exactly
  (a `data object` NavKey needs no extra `Nav3Config` registration).

## Effect on the engine

`AiOpsRunnerImpl` reads `settings.autonomyLevel().first()` on every `onEntitySaved`, so flipping the
level to **Auto-correct** takes effect on the very next save — no restart. The gate semantics
(`autoFix ⇔ HIGH ∧ LOW-risk ∧ reversible ∧ level ≥ AUTO_CORRECT`) are already covered by
`ConfidenceRiskGateTest`; this slice only adds the control that reaches that gate.

## Tests

`AiOpsSelectableLevelsTest` (pure) pins the selectable set and guards against exposing the
backend-managed tiers. The ViewModel itself is thin plumbing over the already-tested gate/settings;
the repo convention is to unit-test pure logic and the engine, not Compose ViewModels (which need a
Main dispatcher), so no VM test is added.
