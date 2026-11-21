# 05 — Customer phone normalization (third capability)

**Status:** implemented. **Purpose:** show capabilities compose on the *same* entity — a second field
(`phone`) on `customer`, added with zero engine/DI/nav edits, alongside the existing `customer.email`
one. Continues Epic-1 Slice 2 (field format/validation).

## What it does

On customer save (and on a workspace scan), the engine standardizes the customer's `phone` to a
digits-only canonical form when it carries display formatting — e.g. `+1 (234) 567-8900` →
`+12345678900`, `234-567.8900` → `2345678900`. Phone numbers are dialled by their digits alone;
spaces, dashes, dots, and parentheses are presentation, not data, so collapsing them (while keeping a
leading `+` country prefix) is a safe, reversible fix. Deterministic (no LLM) ⇒ HIGH confidence;
`phone` is a plain contact field ⇒ LOW risk; so at autonomy ≥ AUTO_CORRECT it auto-fixes, otherwise
it's recorded for review — exactly like `customer.email` and `unit.shortname`.

## How it plugs in (the point)

Three small files in `feature/customer`, depending only on the `com.ampairs.common.aiops` contracts
(+ this module's own `CustomerDao`/`CustomerRepository`) — **no dependency on `feature/aiops`**:

- `aiops/CustomerPhoneNormalizer.kt` — pure decision logic (`normalize`, `needsNormalization`).
  `normalize` strips every non-digit and preserves a single leading `+`; `needsNormalization` is false
  for blank/null and for digitless text (`"n/a"`), so those are never flagged.
- `aiops/CustomerPhoneCapability.kt` — `@Inject @ContributesIntoMap(WorkspaceScope::class)
  @CapabilityKey("customer.phone")`; the five stages. `detect` reads active customers via `CustomerDao`;
  `validate` rejects a digitless or non-canonical target; `score` = HIGH for a deterministic match.
- `aiops/CustomerPhoneExecutor.kt` — `@AiOpsExecutorKey("customer.phone")`; rewrites `phone` via
  `CustomerRepository.updateCustomer` (`synced = false` + pending-push → `CustomerSyncDelegate`). The
  same path serves undo (the recorded `before` is re-applied).

Trigger is already wired: `CustomerFormViewModel.onEntitySaved("customer", uid)` runs every `customer`
capability, and the activity-screen **Scan** runs all capabilities across the workspace. No edits to the
engine, gate, DB, DI aggregation, or navigation were needed — the Metro multibindings
(`Map<String, AiOpsCapability>` / `Map<String, AiOpsExecutor>`) pick up the new contributions on the
WorkspaceGraph classpath automatically. Two capabilities now share the `customer` entity type, which the
runner already handles (it filters by `entityType`, then runs each match).

## Tests

`CustomerPhoneNormalizerTest` covers the decision logic (strip formatting, preserve leading `+`, drop a
non-leading `+`, formatted/canonical detection, blank/null/digitless, idempotence).
`CustomerPhoneCapabilityTest` is a stage test reusing the module's in-memory `FakeCustomerDao` so
`detect` runs against seeded rows (flags a formatted number, skips canonical/blank/digitless/inactive;
propose/validate/score/metadata). The engine pipeline itself (detect→gate→execute→audit→undo) is already
covered generically by the `feature/aiops` runner/gate/undo tests.
