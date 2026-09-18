# 02 — Customer email normalization (second capability)

**Status:** implemented. **Purpose:** prove the AI Ops plug-in model is genuinely mechanical across a
*different* entity/module — no engine changes, just a new capability + executor contributed from the
owning feature. This is the first landed piece of Epic-1 Slice 2 (field format/validation).

## What it does

On customer save, the engine standardizes the customer's `email` to its canonical form
(trim + lowercase) when it isn't already — e.g. `  Jo@X.CoM ` → `jo@x.com`. Email addresses are
case-insensitive in practice (domain part always; local part for effectively every provider), so this
is a safe, reversible data-quality fix. Deterministic (no LLM) ⇒ HIGH confidence; `email` is a plain
contact field ⇒ LOW risk; so at autonomy ≥ AUTO_CORRECT it auto-fixes, otherwise it's recorded for
review — exactly like `unit.shortname`.

## How it plugs in (the point)

Two small files in `feature/customer`, depending only on the `com.ampairs.common.aiops` contracts
(+ this module's own `CustomerDao`/`CustomerRepository`) — **no dependency on `feature/aiops`**:

- `aiops/CustomerEmailNormalizer.kt` — pure decision logic (`normalize`, `needsNormalization`).
- `aiops/CustomerEmailCapability.kt` — `@Inject @ContributesIntoMap(WorkspaceScope::class)
  @CapabilityKey("customer.email")`; the five stages. `detect` reads active customers via `CustomerDao`;
  `score` = HIGH for a deterministic match.
- `aiops/CustomerEmailExecutor.kt` — `@AiOpsExecutorKey("customer.email")`; rewrites `email` via
  `CustomerRepository.updateCustomer` (`synced = false` + pending-push → `CustomerSyncDelegate`). The
  same path serves undo (the recorded `before` is re-applied).
- Trigger: `CustomerFormViewModel` calls `AiOpsRunner.onEntitySaved("customer", uid)` best-effort after
  a successful save (never blocks/fails the save).

No edits to the engine, gate, DB, DI aggregation, or navigation were needed — the Metro multibindings
(`Map<String, AiOpsCapability>` / `Map<String, AiOpsExecutor>`) pick up the new contributions on the
WorkspaceGraph classpath automatically. That's the extensibility claim, demonstrated.

## Tests

`CustomerEmailNormalizerTest` covers the decision logic (normalize, mixed-case/padded detection,
already-canonical, blank/null/non-email, idempotence). The engine pipeline itself (detect→gate→execute→
audit→undo) is already covered generically by the `feature/aiops` runner/gate/undo tests.

## UI surfacing (parity with the unit form)

The customer form now shows the outcome as a snackbar, mirroring `UnitFormScreen`:
`CustomerFormViewModel` exposes a `CustomerFormEvent` `SharedFlow` (`AiOpsEmailFixed` /
`AiOpsSuggestion` / `AiOpsUndone`), emitted from the best-effort `onEntitySaved` pass after a
successful save. `CustomerFormScreen` hosts a `SnackbarHost` and, on `AiOpsEmailFixed`, shows
"AI normalized email to <value>" with an **Undo** action → `viewModel.undoFix(decisionId)` →
`AiOpsUndo.undo(...)` (re-applies the recorded `before` through `CustomerRepository` +
pending-push). Strings live in the module's `commonMain/composeResources` (`customer_aiops_*`).

> Note: like the unit form, `onSuccess()` navigates away on save, so the snackbar is best-effort —
> it displays only while the form remains composed. The fix + audit + undo are unaffected either way.
