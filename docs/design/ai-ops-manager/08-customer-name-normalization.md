# 08 — Customer name whitespace normalization (sixth capability)

**Status:** implemented. **Purpose:** a fourth field on the `customer` entity (`name`), and the first
capability to use a **whitespace-collapse** transform rather than case/format normalization — proving the
suite covers more than upper/lower-casing and trimming. Continues Epic-1 Slice 2 (field format/validation).

## What it does

On a workspace scan (and on customer save, via the existing `onEntitySaved` hook), the engine tidies a
customer's `name` when it carries stray whitespace — leading/trailing padding, or runs of spaces/tabs
between words: `  John   Doe ` → `John Doe`, `Blue  Widget\tXL` → `Blue Widget XL`. It only changes
spacing — never the words, their order, or their case — so it's a safe fix, and the original is preserved
in the audit trail (undo re-applies it), so it's reversible. Deterministic (no LLM) ⇒ HIGH confidence;
spacing cleanup on a display field ⇒ LOW risk; so at autonomy ≥ AUTO_CORRECT it auto-fixes, otherwise it's
recorded for review.

## How it plugs in (the point)

Three small files in `feature/customer`, depending only on the `com.ampairs.common.aiops` contracts (+ the
module's own `CustomerDao`/`CustomerRepository`) — **no dependency on `feature/aiops`**:

- `aiops/CustomerNameNormalizer.kt` — pure decision logic. `normalize` = `trim()` + collapse each internal
  `\s+` run to one space (KMP-safe `Regex`); blank is never flagged.
- `aiops/CustomerNameCapability.kt` — `@Inject @ContributesIntoMap(WorkspaceScope::class)
  @CapabilityKey("customer.name")`; the five stages. `detect` reads active customers via `CustomerDao`.
- `aiops/CustomerNameExecutor.kt` — `@AiOpsExecutorKey("customer.name")`; rewrites `name` via
  `CustomerRepository.updateCustomer` (offline-first write; the same path serves undo).

`customer` now carries four AI Ops capabilities (email, phone, GSTIN, name); the runner filters by
`entityType` then runs each match, so they compose with zero engine/gate/DB/DI/nav edits. This one shows
the transform layer isn't limited to casing/formatting — a new normalizer kind slots in the same way.

## Tests

`CustomerNameNormalizerTest` covers the decision logic (trim + collapse runs, words/order/case preserved,
padded/double-space detection, already-tidy, blank/null, idempotence). `CustomerNameCapabilityTest` is a
stage test reusing the module's in-memory `FakeCustomerDao` so `detect` runs against seeded rows (flags a
double-spaced name, skips tidy/blank/inactive; propose/validate/score/metadata). The engine pipeline itself
is already covered generically by the `feature/aiops` runner/gate/undo tests.
