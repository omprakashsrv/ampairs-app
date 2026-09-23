# 09 — Shared `FieldNormalizationCapability` base (framework consolidation)

**Status:** in progress (base landed; `product.code` migrated; other normalizers to follow).
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

The remaining five (customer email/phone/GSTIN/name, unit short-name) migrate onto the base in follow-up
increments, one at a time, each validated by its existing stage test — no behavior change, just less
boilerplate.
