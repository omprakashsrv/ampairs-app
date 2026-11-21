# 07 — Customer GSTIN normalization (fifth capability)

**Status:** implemented. **Purpose:** a third field on the `customer` entity (`gstNumber`), continuing
Epic-1 Slice 2 (field format/validation) and showing the plug-in recipe scales field-by-field with no
engine work. **In scope note:** this is an identifier *format* fix (letter case + edge whitespace) — it is
**not** the roadmap's tax/HSN advisory work and never suggests or changes a tax classification.

## What it does

On a workspace scan (and on customer save, via the existing `onEntitySaved("customer", uid)` hook), the
engine standardizes a customer's `gstNumber` to canonical form — trim + upper-case — when it isn't
already: `  22aaaaa0000a1z5 ` → `22AAAAA0000A1Z5`. A GSTIN is a regulated 15-character identifier that is
matched and validated exactly and is canonically upper-case, so trimming and upper-casing is a safe,
reversible fix (users routinely paste it lower-cased or padded from emails/portals). Only edge whitespace
and case change — internal characters are never removed, so the value is always reversible. Deterministic
(no LLM) ⇒ HIGH confidence; a format touch-up on an identifier ⇒ LOW risk; so at autonomy ≥ AUTO_CORRECT
it auto-fixes, otherwise it's recorded for review — exactly like the other normalizers.

## How it plugs in (the point)

Three small files in `feature/customer`, depending only on the `com.ampairs.common.aiops` contracts (+ the
module's own `CustomerDao`/`CustomerRepository`) — **no dependency on `feature/aiops`**:

- `aiops/CustomerGstNormalizer.kt` — pure decision logic (`normalize`, `needsNormalization`); blank is
  never flagged.
- `aiops/CustomerGstCapability.kt` — `@Inject @ContributesIntoMap(WorkspaceScope::class)
  @CapabilityKey("customer.gst")`; the five stages. `detect` reads active customers via `CustomerDao`;
  `validate` rejects a non-canonical target; `score` = HIGH.
- `aiops/CustomerGstExecutor.kt` — `@AiOpsExecutorKey("customer.gst")`; rewrites `gstNumber` via
  `CustomerRepository.updateCustomer` (`synced = false` + pending-push → `CustomerSyncDelegate`). The same
  path serves undo (the recorded `before` is re-applied).

`customer` now carries three AI Ops capabilities (email, phone, GSTIN); the runner filters by
`entityType` then runs each match, so they compose with zero engine/gate/DB/DI/nav edits — the Metro
multibindings pick up the new `@CapabilityKey`/`@AiOpsExecutorKey` contributions automatically. Triggering
is already wired both ways (Scan + customer-save).

## Tests

`CustomerGstNormalizerTest` covers the decision logic (trim + upper-case, lower/mixed/padded detection,
already-canonical, blank/null, idempotence). `CustomerGstCapabilityTest` is a stage test reusing the
module's in-memory `FakeCustomerDao` so `detect` runs against seeded rows (flags a non-canonical GSTIN,
skips canonical/blank/inactive; propose/validate/score/metadata). The engine pipeline itself
(detect→gate→execute→audit→undo) is already covered generically by the `feature/aiops` runner/gate/undo
tests.
