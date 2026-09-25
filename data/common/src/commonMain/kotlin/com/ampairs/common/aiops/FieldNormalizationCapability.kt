package com.ampairs.common.aiops

/**
 * Reusable base for a **single-field, deterministic text-normalization** AI Ops capability — the shape
 * every normalizer capability shares (customer email/phone/GSTIN/name, product code, …). It owns the
 * field-agnostic stages `propose → validate → score` so a concrete capability only supplies:
 *
 *  - its identity/metadata ([key], [entityType], [riskLevel], [field], [evidenceTag]);
 *  - the pure decision functions ([normalize] / [needsNormalization]) — typically delegating to a small
 *    `object` normalizer;
 *  - human text ([rationale], [summarize]);
 *  - and the DAO-bound stages [detect] / [gather] (which build/enrich findings — those can't be generic
 *    because they read the owning module's own DAO). Use [findingFor] inside [detect] to turn an entity's
 *    current value into a Finding (or null when it's already canonical).
 *
 * An optional [rejectTarget] hook adds a per-capability guard on the *target* value (e.g. "must contain
 * '@'"), checked before the generic canonical check — matching the order the hand-written capabilities used.
 *
 * Lives in `data/common` next to [AiOpsCapability] so any feature can extend it while depending only on
 * the contracts (never on `feature/aiops`). It adds no state and no injected dependencies of its own — the
 * concrete subclass keeps its `@Inject` constructor and Metro wiring unchanged.
 */
abstract class FieldNormalizationCapability : AiOpsCapability {

    /** The field being normalized — used for `Finding.field` and `Candidate.field`. */
    protected abstract val field: String

    /** Short evidence/contributor tag, e.g. "email_normalize" (also the score contributor key). */
    protected abstract val evidenceTag: String

    /** Deterministic canonical form of a non-blank value. */
    protected abstract fun normalize(value: String): String

    /** Whether [value] is a non-blank, non-canonical (i.e. fixable) value. */
    protected abstract fun needsNormalization(value: String?): Boolean

    /** Human rationale for the proposed change (shown in review/audit). */
    protected abstract fun rationale(current: String, canonical: String): String

    /** One-line finding summary (shown in the review queue / activity feed). */
    protected abstract fun summarize(current: String, canonical: String): String

    /**
     * Optional extra guard on the *target* value: return an error string to reject it, or null to accept.
     * Checked before the generic canonical check, so it wins when both would fire. Default: no extra guard.
     */
    protected open fun rejectTarget(after: String): String? = null

    /**
     * Build the Finding for [entityId]'s [current] value, or null if it's already canonical / blank.
     * Concrete [detect] implementations map their DAO rows through this.
     */
    protected fun findingFor(entityId: String, current: String?): Finding? {
        if (!needsNormalization(current)) return null
        val value = current!!
        val canonical = normalize(value)
        return Finding(
            id = "AIO-$key-$entityId",
            capability = key,
            entityType = entityType,
            entityId = entityId,
            field = field,
            summary = summarize(value, canonical),
            signals = mapOf("current" to value, "canonical" to canonical),
        )
    }

    final override suspend fun propose(finding: Finding, context: FindingContext): List<Candidate> {
        val current = context.values["current"] ?: finding.signals["current"] ?: return emptyList()
        if (!needsNormalization(current)) return emptyList()
        val canonical = normalize(current)
        return listOf(
            Candidate(
                field = field,
                before = current,
                after = canonical,
                action = AiOpsActionType.UPDATE_FIELD,
                rationale = rationale(current, canonical),
                evidence = listOf("$evidenceTag:$current→$canonical"),
            ),
        )
    }

    final override suspend fun validate(finding: Finding, candidate: Candidate, context: FindingContext): Validation {
        val after = candidate.after
        return when {
            candidate.action != AiOpsActionType.UPDATE_FIELD -> Validation(false, "unexpected action")
            after.isNullOrBlank() -> Validation(false, "no target value")
            after == candidate.before -> Validation(false, "already normalized")
            else -> {
                val rejection = rejectTarget(after)
                when {
                    rejection != null -> Validation(false, rejection)
                    after != normalize(after) -> Validation(false, "target is not canonical")
                    else -> Validation(true)
                }
            }
        }
    }

    final override suspend fun score(finding: Finding, candidate: Candidate, context: FindingContext): Confidence =
        Confidence(value = 0.999, band = AiOpsBand.HIGH, contributors = mapOf(evidenceTag to 1.0))
}
