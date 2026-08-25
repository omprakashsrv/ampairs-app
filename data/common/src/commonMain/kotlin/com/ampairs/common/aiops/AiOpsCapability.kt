package com.ampairs.common.aiops

import dev.zacsweers.metro.MapKey

/**
 * One AI-Ops **capability** (e.g. unit standardization, dedup) — a keyed bundle of the engine's
 * pipeline stages plus metadata. The engine runner (`feature/aiops`) resolves the
 * `Map<String, AiOpsCapability>` and runs `detect → gather → propose → validate → score` for the saved
 * entity's capabilities, then gates the result.
 *
 * Each owning feature module contributes its own capability, depending only on `data/common`
 * (mirrors `ModuleQueryExecutor` for the agent SAFE_QUERY path). Contributed via
 * `@Inject @ContributesIntoMap(WorkspaceScope::class) @CapabilityKey("<key>")`.
 *
 * Implementations MUST be read-only in [detect]/[gather]/[propose]/[validate]/[score] — the only write
 * happens later, through an [AiOpsExecutor], and only when the gate approves.
 */
interface AiOpsCapability {
    /** Stable key, e.g. "product.unit". Must match this capability's [AiOpsExecutorKey]. */
    val key: String

    /** The owning entity type, e.g. "product". Runner dispatches by this on `onEntitySaved`. */
    val entityType: String

    /** Field-sensitivity/blast-radius class for the gate. Tax/HSN capabilities MUST be HIGH. */
    val riskLevel: AiOpsRiskLevel

    /** Find problems (deterministic queries + heuristics) within [scope]. */
    suspend fun detect(scope: AiOpsScope): List<Finding>

    /** Enrich a [finding] with the facts the later stages need. */
    suspend fun gather(finding: Finding): FindingContext

    /** Propose fixes (deterministic and/or LLM via the host's Reasoner — unused for rule-only caps). */
    suspend fun propose(finding: Finding, context: FindingContext): List<Candidate>

    /** Reject impossible/unsafe candidates against DB + rules. */
    suspend fun validate(finding: Finding, candidate: Candidate, context: FindingContext): Validation

    /** Ensemble confidence — never the LLM alone; LLM contribution is capped by the caller/gate. */
    suspend fun score(finding: Finding, candidate: Candidate, context: FindingContext): Confidence
}

/**
 * Applies an approved [Candidate] to the owning module's data through that module's normal
 * write + offline-sync path (so the fix rides the existing `SyncDelegate`), and can re-apply a prior
 * value for rollback. Contributed by the owning feature via
 * `@Inject @ContributesIntoMap(WorkspaceScope::class) @AiOpsExecutorKey("<key>")` — keyed to match its
 * [AiOpsCapability.key].
 */
interface AiOpsExecutor {
    val capabilityKey: String

    /** Apply [candidate] to [finding]'s entity. [Candidate.before] enables the inverse for rollback. */
    suspend fun apply(candidate: Candidate, finding: Finding): ExecResult
}

/**
 * The engine entry point a feature calls after a successful local write, e.g.
 * `runner.onEntitySaved("product", uid)`. The implementation (`feature/aiops`) runs the matching
 * capabilities off the UI path and surfaces auto-fixes/suggestions + audit. Exposed here (in
 * `data/common`) so features depend only on the contract, not the aiops impl.
 */
interface AiOpsRunner {
    suspend fun onEntitySaved(entityType: String, entityId: String)
}

/** Metro map key — contributes an [AiOpsCapability] keyed by capability key (e.g. "product.unit"). */
@MapKey
annotation class CapabilityKey(val value: String)

/** Metro map key — contributes an [AiOpsExecutor] keyed by capability key (e.g. "product.unit"). */
@MapKey
annotation class AiOpsExecutorKey(val value: String)
