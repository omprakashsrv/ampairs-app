# 10 — Product name whitespace normalization (seventh capability)

**Status:** implemented. **Purpose:** a second field on the `product` entity (`name`, alongside
`product.code`), and the first capability written *directly on the shared base* rather than migrated onto
it — so it doubles as a demonstration that a new single-field normalizer is now ~30 lines. Continues
Epic-1 Slice 2 (field format/validation).

## What it does

On a workspace scan (and on product save, via the existing `onEntitySaved("product", uid)` hook), the
engine tidies a product's `name` when it carries stray whitespace — edge padding or runs of spaces/tabs
between words: `  Blue   Widget ` → `Blue Widget`, `Steel  Bolt\tM8` → `Steel Bolt M8`. Only spacing
changes — never the words, order, or case — and the original is preserved in the audit trail (undo
re-applies it), so it's reversible. Deterministic (no LLM) ⇒ HIGH confidence; spacing cleanup on a display
field ⇒ LOW risk; so at autonomy ≥ AUTO_CORRECT it auto-fixes, otherwise it's recorded for review.

## How it plugs in (the point)

Three small files in `feature/product`, on the shared [FieldNormalizationCapability] base, depending only
on the `com.ampairs.common.aiops` contracts (+ the shared `ProductDao`/`ProductRepository`):

- `aiops/ProductNameNormalizer.kt` — pure `normalize` (`trim()` + collapse each internal `\s+` run to one
  space, KMP-safe `Regex`) / `needsNormalization` (blank never flagged).
- `aiops/ProductNameCapability.kt` — `@Inject @ContributesIntoMap(WorkspaceScope::class)
  @CapabilityKey("product.name")` extending the base; supplies only its metadata, the normalizer, the
  rationale/summary text, and the DAO-bound `detect`/`gather` (reads active products via `ProductDao`).
- `aiops/ProductNameExecutor.kt` — `@AiOpsExecutorKey("product.name")`; rewrites `name` via
  `ProductRepository.updateProduct` (offline-first write; the same path serves undo).

`product` now carries two AI Ops capabilities (code, name); the runner filters by `entityType` then runs
each match, so they compose with zero engine/gate/DB/DI/nav edits. Triggering is already wired both ways
(Scan + product-save), so `product.name` is live the moment it's contributed — no VM change needed.

## Tests

`ProductNameNormalizerTest` covers the decision logic (trim + collapse runs, words/order/case preserved,
padded/double-space detection, already-tidy, blank/null, idempotence). `ProductNameCapabilityTest` is a
stage test reusing the module's in-memory `FakeProductDao` so `detect` runs against seeded rows (flags a
double-spaced name, skips tidy/blank/inactive; propose/validate/score/metadata). The shared pipeline logic
it inherits is covered by `FieldNormalizationCapabilityTest`; the engine loop (detect→gate→execute→audit→
undo) by the `feature/aiops` runner/gate/undo tests.
