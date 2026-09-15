package com.ampairs.unit.aiops

import com.ampairs.common.aiops.AiOpsBand
import com.ampairs.common.aiops.AiOpsCapability
import com.ampairs.common.aiops.AiOpsRiskLevel
import com.ampairs.common.aiops.AiOpsScope
import com.ampairs.common.aiops.CapabilityKey
import com.ampairs.common.aiops.AiOpsActionType
import com.ampairs.common.aiops.Candidate
import com.ampairs.common.aiops.Confidence
import com.ampairs.common.aiops.Finding
import com.ampairs.common.aiops.FindingContext
import com.ampairs.common.aiops.Validation
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.unit.data.repository.UnitRepository
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

/**
 * AI Ops **unit standardization** capability (Slice 1, deliverable C) — the first capability proving
 * the app-side engine end-to-end. It standardizes a unit's free-text short name to a single canonical
 * spelling (`Kgs`/`Kilo` → `KG`, `LTR`/`Litres` → `L`) using the deterministic [UnitAliasCatalog] —
 * **no LLM** — so matches score at HIGH confidence and, at autonomy ≥ AUTO_CORRECT, auto-fix through
 * the normal unit write + offline-sync path (see [UnitShortNameExecutor]).
 *
 * Risk is LOW: `short_name` is a display label on the unit's own row (not an FK, price, or tax field),
 * and every change is reversible via the recorded `before` value.
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@CapabilityKey("unit.shortname")
class UnitShortNameCapability(
    private val unitRepository: UnitRepository,
) : AiOpsCapability {

    override val key: String = KEY
    override val entityType: String = ENTITY_TYPE
    override val riskLevel: AiOpsRiskLevel = AiOpsRiskLevel.LOW

    /** Emit a (stable-id) finding for every active unit whose short name isn't already canonical. */
    override suspend fun detect(scope: AiOpsScope): List<Finding> =
        unitRepository.getActiveUnits().mapNotNull { unit ->
            val current = unit.shortName
            val canonical = UnitAliasCatalog.canonicalFor(current) ?: return@mapNotNull null
            if (canonical == current) return@mapNotNull null // already canonical — nothing to do
            Finding(
                id = findingId(unit.uid),
                capability = KEY,
                entityType = ENTITY_TYPE,
                entityId = unit.uid,
                field = FIELD_SHORT_NAME,
                summary = "Standardize unit short name \"$current\" → \"$canonical\"",
                signals = mapOf("current" to current, "canonical" to canonical),
            )
        }

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

    /** Deterministic single-field rewrite to the canonical spelling. */
    override suspend fun propose(finding: Finding, context: FindingContext): List<Candidate> {
        val current = context.values["current"] ?: finding.signals["current"] ?: return emptyList()
        val canonical = UnitAliasCatalog.canonicalFor(current) ?: return emptyList()
        if (canonical == current) return emptyList()
        return listOf(
            Candidate(
                field = FIELD_SHORT_NAME,
                before = current,
                after = canonical,
                action = AiOpsActionType.UPDATE_FIELD,
                rationale = "\"$current\" is a known alias of the canonical unit \"$canonical\".",
                evidence = listOf("alias_catalog:$current→$canonical"),
            ),
        )
    }

    /** Reject anything that isn't a real, changing rewrite toward a known canonical. */
    override suspend fun validate(finding: Finding, candidate: Candidate, context: FindingContext): Validation {
        val after = candidate.after
        return when {
            candidate.action != AiOpsActionType.UPDATE_FIELD -> Validation(false, "unexpected action")
            after.isNullOrBlank() -> Validation(false, "no target value")
            after == candidate.before -> Validation(false, "already canonical")
            after !in UnitAliasCatalog.canonicals -> Validation(false, "target \"$after\" is not a canonical unit")
            else -> Validation(true)
        }
    }

    /** Deterministic alias match ⇒ HIGH confidence (the LLM contributes nothing here). */
    override suspend fun score(finding: Finding, candidate: Candidate, context: FindingContext): Confidence =
        Confidence(value = 0.999, band = AiOpsBand.HIGH, contributors = mapOf("alias_catalog" to 1.0))

    companion object {
        const val KEY = "unit.shortname"
        const val ENTITY_TYPE = "unit"
        const val FIELD_SHORT_NAME = "short_name"

        /** Stable per-unit finding id so re-detection upserts the same row instead of duplicating it. */
        fun findingId(unitUid: String): String = "AIO-$KEY-$unitUid"
    }
}
