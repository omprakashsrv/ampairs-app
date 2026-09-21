# 06 — Product code normalization (third module)

**Status:** implemented. **Purpose:** prove the AI Ops plug-in model reaches a **third module**
(`product`) with the same mechanical recipe used for `unit` and `customer` — no engine, gate, DB, DI, or
navigation changes, just a capability + executor contributed from the owning feature. Continues Epic-1
Slice 2 (field format/validation).

## What it does

On a workspace scan (and, once wired, on product save), the engine standardizes a product's `code` (SKU)
to its canonical form — trim + upper-case — when it isn't already: `  sku-001 ` → `SKU-001`. A product
code is an exact-match machine identifier (scanned, looked up, matched), so surrounding whitespace and
inconsistent case are data-entry noise, and collapsing them is a safe, reversible fix. Deterministic
(no LLM) ⇒ HIGH confidence; `code` is a plain identifier ⇒ LOW risk; so at autonomy ≥ AUTO_CORRECT it
auto-fixes, otherwise it's recorded for review — exactly like `customer.email`/`customer.phone` and
`unit.shortname`.

## How it plugs in (the point)

Three small files in `feature/product`, depending only on the `com.ampairs.common.aiops` contracts (+ the
shared `ProductDao` and `ProductRepository` the feature already injects) — **no dependency on
`feature/aiops`**:

- `aiops/ProductCodeNormalizer.kt` — pure decision logic (`normalize`, `needsNormalization`); blank codes
  are never flagged.
- `aiops/ProductCodeCapability.kt` — `@Inject @ContributesIntoMap(WorkspaceScope::class)
  @CapabilityKey("product.code")`; the five stages. `detect` reads active products via
  `ProductDao.observeAllProducts()`; `validate` rejects a non-canonical target; `score` = HIGH.
- `aiops/ProductCodeExecutor.kt` — `@AiOpsExecutorKey("product.code")`; rewrites `code` via
  `ProductRepository.updateProduct` (`synced = false` + pending-push → `ProductSyncDelegate`). The same
  path serves undo (the recorded `before` is re-applied).

No edits to the engine, gate, DB, DI aggregation, or navigation were needed — the Metro multibindings
(`Map<String, AiOpsCapability>` / `Map<String, AiOpsExecutor>`) pick up the new contributions on the
WorkspaceGraph classpath automatically. That the recipe now spans **unit, customer, and product** with
identical shape is the extensibility claim, demonstrated across module boundaries.

## Triggering

The activity-screen **Scan** (`AiOpsRunner.scanWorkspace()`) already runs every capability across the
workspace, so `product.code` is live via Scan the moment it ships. The per-save hook — a best-effort
`AiOpsRunner.onEntitySaved("product", uid)` from the product form ViewModel, mirroring
`CustomerFormViewModel` — is the natural follow-up so a fix is offered right after an edit; the capability
needs no change for it.

## Tests

`ProductCodeNormalizerTest` covers the decision logic (trim + upper-case, lower/mixed/padded detection,
already-canonical, blank/null, idempotence). `ProductCodeCapabilityTest` is a stage test reusing the
module's in-memory `FakeProductDao` (promoted to `internal`) so `detect` runs against seeded rows
(flags a non-canonical code, skips canonical/blank/inactive; propose/validate/score/metadata). The engine
pipeline itself (detect→gate→execute→audit→undo) is already covered generically by the `feature/aiops`
runner/gate/undo tests.
