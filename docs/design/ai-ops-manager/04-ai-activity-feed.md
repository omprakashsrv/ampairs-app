# 04 — AI Activity feed (surfacing the audit trail)

**Status:** implemented. **Purpose:** give the user a place to see what the data-quality assistant
actually did, and undo any of it. The engine already wrote every applied fix to the `aiops_decision`
audit table and every suggestion to `aiops_finding`, but nothing surfaced them — the only view of a
fix was the transient save-time snackbar. This screen makes the audit trail visible and reversible
from one place, which matters most at **L2 Auto-correct** (fixes happen on their own) and completes
the safe-default loop at **L1** (suggestions are counted).

## What it does

**More → AI Activity** shows the audit trail newest-first. Each row is one decision:
`field: before → after`, with the capability + business-locale timestamp, and:
- an **Undo** action when the decision is reversible and not yet reverted →
  `AiOpsUndo.undo(decisionId)` (re-applies the recorded `before` through the same executor);
- a **Reverted** label once it has been rolled back.

A header line counts suggestions still pending review (`aiops_finding` status `PENDING_REVIEW`).
Empty state when the assistant hasn't changed anything yet.

## How it wires in

- **DAO (`data/database`):** one added reactive query —
  `observeRecentDecisions(limit): Flow<List<AiOpsDecisionEntity>>`. No schema change, no migration.
- **Feature (`feature/aiops/ui/activity`):** `AiOpsActivityViewModel`
  (`@ContributesIntoMap(WorkspaceScope)` — the audit DB is per-workspace) `combine`s recent decisions
  with the pending-suggestion count into `AiOpsActivityUiState`; `AiOpsActivityScreen` renders the
  list; `AiOpsActivityRoute` is the NavKey. The decision → row mapping lives in the pure
  `AiOpsDecisionEntity.toActivityItem()` (so `canUndo` is unit-tested without Compose/dispatcher).
- **Shared nav:** `aiOpsEntryProvider` gains the route → screen case; `MoreScreen` adds an
  "AI Activity" row next to "AI Automation".

Undo is fully reactive: `AiOpsUndo` marks the decision reverted, and the `observeRecentDecisions`
Flow re-emits, so the row flips to **Reverted** with no manual refresh.

## Tests

`AiOpsActivityItemTest` (pure) pins the undo eligibility: a reversible, un-reverted decision is
undoable; a reverted or non-reversible one is not. The undo mechanics themselves are covered by the
existing `AiOpsUndoServiceTest`.

## Acting on suggestions (added)

The activity screen now has a **Suggestions** section above the history: each `PENDING_REVIEW`
finding shows its summary with **Accept** / **Dismiss**. This is served by a new `AiOpsReview` port
(`data/common`) implemented by `AiOpsReviewService` (`feature/aiops`, `WorkspaceScope`):

- **Accept** re-derives the candidate for the stored finding through the owning capability
  (`gather` → `propose` → `validate`; `gather` re-reads the live entity, so the fix reflects the
  current value), applies it via the capability's executor (user-approved, so the gate is bypassed),
  and records a reversible **HUMAN** decision — so an accepted suggestion appears in the history with
  Undo, exactly like an auto-fix. The finding becomes `ACCEPTED`.
- **Dismiss** marks the finding `IGNORED` and records an `IGNORE` verdict; no data changes.

Both are no-ops unless the finding is still `PENDING_REVIEW`, and both are reactive — the finding/
decision flows re-emit, so the lists update with no manual refresh. `AiOpsReviewServiceTest` covers
accept (apply + reversible decision + status), the validation/duplicate/missing guards, and dismiss.

This makes **L1 Recommend** (the safe default) genuinely useful: the engine proposes, the user
reviews and one-taps Accept/Dismiss, and every accepted change stays auditable and reversible.

## Scan workspace (proactive)

Until now the engine only ran when you saved an individual entity (`onEntitySaved`), so pre-existing
data-quality issues stayed invisible. The activity screen's toolbar now has a **Scan** action
(`AiOpsRunner.scanWorkspace()`): it runs *every* capability's `detect → … → gate` across the whole
workspace, not scoped to one entity. Same gate — at **RECOMMEND** it fills the Suggestions queue for
every messy record; at **AUTO_CORRECT** it auto-fixes the safe ones (all audited/undoable). The button
shows a spinner while scanning; the reactive lists fill in as rows are written. `AiOpsRunnerImpl`
shares one private `run(caps, entityIdFilter)` between the on-save path (filtered to the saved entity)
and the scan path (`entityIdFilter = null`); `AiOpsRunnerImplTest` covers both scan branches.
