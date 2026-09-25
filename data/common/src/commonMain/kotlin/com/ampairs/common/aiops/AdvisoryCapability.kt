package com.ampairs.common.aiops

/**
 * Reusable base for an **advisory / completeness** AI Ops capability — a *detect-only* check that the
 * engine records for human review but never auto-fixes, because there's no safe deterministic value to
 * write (e.g. "customer has neither email nor phone", "invoice has no line items"). This is the second
 * capability shape after [FieldNormalizationCapability]: where a normalizer proposes a reversible field
 * rewrite, an advisory proposes a **non-reversible NO_OP** — `before == null`, so the
 * `ConfidenceRiskGate` can never mark it auto-eligible (it requires a reversible candidate) and it always
 * lands as a SUGGEST (a review item), or OBSERVE at the Observe autonomy level.
 *
 * A concrete advisory supplies only its identity ([key], [entityType], [riskLevel], [evidenceTag]) and
 * the DAO-bound [detect]/[gather] (build the flagged findings via [advisory]); the base owns the fixed
 * `propose`/`validate`/`score`. Advisories have **no [AiOpsExecutor]**: they are never applied, so
 * "Accept" in the review UI is a safe no-op (the review service returns false when no executor is keyed
 * to the capability) and the user acts on the flag manually, then Dismiss-es it.
 *
 * Lives in `data/common` next to [AiOpsCapability] so any feature can contribute one depending only on
 * the contracts. Holds no state and injects nothing — the concrete keeps its `@Inject` constructor and
 * Metro wiring.
 */
abstract class AdvisoryCapability : AiOpsCapability {

    /** Short evidence/contributor tag, e.g. "contact_missing" (also the score contributor key). */
    protected abstract val evidenceTag: String

    /**
     * Build an advisory [Finding] for [entityId]. The `id` is stable (`AIO-<key>-<entityId>`) so
     * re-detection upserts the same row rather than duplicating it. Concrete [detect] maps its flagged
     * DAO rows through this.
     */
    protected fun advisory(
        entityId: String,
        summary: String,
        field: String? = null,
        signals: Map<String, String> = emptyMap(),
    ): Finding = Finding(
        id = "AIO-$key-$entityId",
        capability = key,
        entityType = entityType,
        entityId = entityId,
        field = field,
        summary = summary,
        signals = signals,
    )

    final override suspend fun propose(finding: Finding, context: FindingContext): List<Candidate> =
        listOf(
            Candidate(
                field = finding.field,
                before = null,   // non-reversible: the gate can never auto-fix this
                after = null,
                action = AiOpsActionType.NO_OP,
                rationale = finding.summary,
                evidence = listOf("$evidenceTag:${finding.entityId}"),
            ),
        )

    final override suspend fun validate(finding: Finding, candidate: Candidate, context: FindingContext): Validation =
        if (candidate.action == AiOpsActionType.NO_OP && candidate.before == null) {
            Validation(true)
        } else {
            Validation(false, "not an advisory candidate")
        }

    /** A deterministic completeness/validation check is HIGH-confidence *as a flag* (there's no fix to score). */
    final override suspend fun score(finding: Finding, candidate: Candidate, context: FindingContext): Confidence =
        Confidence(value = 1.0, band = AiOpsBand.HIGH, contributors = mapOf(evidenceTag to 1.0))
}
