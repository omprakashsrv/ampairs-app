# 09 — Shared `FieldNormalizationCapability` base (framework consolidation)

**Status:** in progress (base landed; `product.code` + all four customer capabilities migrated; only
`unit.shortname` remains).
**Purpose:** the six normalization capabilities (customer email/phone/GSTIN/name, product code, unit
short-name) had grown near-identical `propose → validate → score` bodies plus the same `detect`/`gather`
shape, differing only in the normalizer, field name, and human text. This extracts that shared shape into
a reusable base so a new single-field normalizer is ~30 lines instead of ~100, and the pipeline logic
lives in one audited place.

## What landed

`data/common/.../aiops/FieldNormalizationCapability.kt` — an abstract class implementing `AiOpsCapability`
that owns the field-agnostic stages as `final`:

- **`propose`** — reads the current value (context → finding signals), and if still non-canonical, emits
  one reversible `UPDATE_FIELD` `Candidate` (before → `normalize(before)`) with a rationale + evidence tag.
- **`validate`** — the standard guard chain: action is `UPDATE_FIELD`, target non-blank, target ≠ before,
  optional per-capability `rejectTarget` hook, then target is canonical.
- **`score`** — `Confidence(0.999, HIGH, {evidenceTag: 1.0})` for a deterministic match.
- **`findingFor(entityId, current)`** — helper that turns an entity's current field value into a `Finding`
  (or null when already canonical), for concrete `detect` implementations to map their DAO rows through.

A concrete capability now supplies only: identity/metadata (`key`, `entityType`, `riskLevel`, `field`,
`evidenceTag`), the pure `normalize`/`needsNormalization` (delegating to its `object` normalizer), the
`rationale`/`summarize` text, an optional `rejectTarget` guard, and the DAO-bound `detect`/`gather` (which
can't be generic — they read the owning module's own DAO).

The base lives in `data/common` next to `AiOpsCapability`, so any feature extends it while still depending
only on the contracts — never on `feature/aiops`. It holds no state and injects nothing, so each concrete
capability keeps its `@Inject` constructor and `@ContributesIntoMap(WorkspaceScope)` / `@CapabilityKey`
wiring unchanged — Metro is unaffected.

## Migration

`ProductCodeCapability` is migrated as the proof: it drops its hand-written `propose`/`validate`/`score`
and inherits them, keeping identical behavior (its `ProductCodeCapabilityTest` exercises
detect/propose/validate/score and stays green). The `rejectTarget` hook preserves the two capabilities
that need an extra target guard — email (`must contain '@'`) and phone (`must contain a digit`) — checked
before the generic canonical check, matching the order the hand-written versions used.

The four customer capabilities (email, phone, GSTIN, name) are now migrated too. Email and phone exercise
the `rejectTarget` hook — email keeps its "must contain '@'" guard, phone its "must contain a digit" guard,
both checked before the generic canonical check exactly as the hand-written versions did. Their
`detect` keeps the `.filter { it.active }` pass (customer's `getAllCustomers` isn't active-scoped like
product's `observeAllProducts`), then maps rows through `findingFor`. All four stage tests stay green — no
behavior change, just less boilerplate.

Only `unit.shortname` remains unmigrated; it resolves a short-name via `UnitAliasCatalog` (an alias-table
lookup) rather than a pure per-value normalizer, so it doesn't fit the base as cleanly and is left as-is
for now.

## Tests

`FieldNormalizationCapabilityTest` pins the base contract directly (via a trivial trim+lowercase fake
subclass), so the shared logic is covered once in its own right rather than only indirectly through each
capability's stage test: `findingFor` builds a stable finding for a non-canonical value and returns null
for canonical/blank; `propose` emits one reversible `UPDATE_FIELD` candidate (reading context then the
finding signal, empty when canonical/absent); `validate` walks the guard chain; `score` is HIGH with the
evidence tag as the sole contributor. It also locks the ordering that matters — a `rejectTarget` rejection
wins over the canonical check — which is what keeps email's `@` / phone's digit guards behaving as before.
(It lives in `feature/customer`'s test source set, which already depends on `data/common` and has the test
deps, so the base gets a direct test with no new source set / build changes.)
