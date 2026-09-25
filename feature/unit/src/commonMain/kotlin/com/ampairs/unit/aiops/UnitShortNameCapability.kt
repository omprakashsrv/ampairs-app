package com.ampairs.unit.aiops

import com.ampairs.common.aiops.AiOpsRiskLevel
import com.ampairs.common.aiops.AiOpsScope
import com.ampairs.common.aiops.CapabilityKey
import com.ampairs.common.aiops.FieldNormalizationCapability
import com.ampairs.common.aiops.Finding
import com.ampairs.common.aiops.FindingContext
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.unit.data.repository.UnitRepository
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

/**
 * AI Ops **unit standardization** capability (Slice 1, deliverable C) — the first capability proving the
 * app-side engine end-to-end. It standardizes a unit's free-text short name to a single canonical spelling
 * (`Kgs`/`Kilo` → `KG`, `LTR`/`Litres` → `L`) using the deterministic [UnitAliasCatalog] — **no LLM** — so
 * matches score at HIGH confidence and, at autonomy ≥ AUTO_CORRECT, auto-fix through the normal unit write
 * + offline-sync path (see [UnitShortNameExecutor]).
 *
 * Now built on the shared [FieldNormalizationCapability] base like the other normalizers, but its
 * "normalize" is an **alias-catalog lookup**, not a pure transform: [normalize] returns the canonical
 * spelling for a known alias and leaves an unknown value unchanged, [needsNormalization] fires only for a
 * known alias that isn't already canonical, and a [rejectTarget] guard keeps the target constrained to the
 * catalog's canonicals (which the generic canonical check can't enforce, since an unknown value normalizes
 * to itself). Risk is LOW: `short_name` is a display label on the unit's own row, and every change is
 * reversible via the recorded `before`.
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@CapabilityKey("unit.shortname")
class UnitShortNameCapability(
    private val unitRepository: UnitRepository,
) : FieldNormalizationCapability() {

    override val key: String = KEY
    override val entityType: String = ENTITY_TYPE
    override val riskLevel: AiOpsRiskLevel = AiOpsRiskLevel.LOW
    override val field: String = FIELD_SHORT_NAME
    override val evidenceTag: String = "alias_catalog"

    /** Canonical spelling for a known alias; an unknown value is left unchanged. */
    override fun normalize(value: String): String = UnitAliasCatalog.canonicalFor(value) ?: value

    /** Fires only for a known alias whose canonical spelling differs from the current value. */
    override fun needsNormalization(value: String?): Boolean {
        if (value.isNullOrBlank()) return false
        val canonical = UnitAliasCatalog.canonicalFor(value) ?: return false
        return canonical != value
    }

    /** Constrain the target to a catalog canonical — the generic check can't (unknowns normalize to self). */
    override fun rejectTarget(after: String): String? =
        if (after !in UnitAliasCatalog.canonicals) "target \"$after\" is not a canonical unit" else null

    override fun rationale(current: String, canonical: String): String =
        "\"$current\" is a known alias of the canonical unit \"$canonical\"."

    override fun summarize(current: String, canonical: String): String =
        "Standardize unit short name \"$current\" → \"$canonical\""

    /** Emit a (stable-id) finding for every active unit whose short name isn't already canonical. */
    override suspend fun detect(scope: AiOpsScope): List<Finding> =
        unitRepository.getActiveUnits().mapNotNull { findingFor(it.uid, it.shortName) }

    override suspend fun gather(finding: Finding): FindingContext {
        val unit = unitRepository.getUnitById(finding.entityId)
        return FindingContext(
            values = buildMap {
                unit?.let {
                    put("current", it.shortName)
                    put("name", it.name)
                }
            },
        )
    }

    companion object {
        const val KEY = "unit.shortname"
        const val ENTITY_TYPE = "unit"
        const val FIELD_SHORT_NAME = "short_name"

        /** Stable per-unit finding id so re-detection upserts the same row instead of duplicating it. */
        fun findingId(unitUid: String): String = "AIO-$KEY-$unitUid"
    }
}
