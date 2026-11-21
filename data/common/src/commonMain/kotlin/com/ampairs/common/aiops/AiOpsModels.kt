package com.ampairs.common.aiops

/**
 * Core value types for the AI Business Operations Manager engine (app-side host).
 *
 * These are **plain KMP `commonMain` types** — no `java.*`/`android.*`/Koog — so the engine compiles on
 * every target and so any feature module can contribute a capability/executor by depending only on
 * `data/common` (mirrors the agent's `ModuleQueryExecutor`/`ModuleQuerySchema` split). See
 * `docs/design/ai-ops-manager/` (this repo) and `ampairs/docs/ai-ops-manager/` (program + ADR 0005/0006).
 */

/** What a [Candidate] would do to the target entity. Drives both apply and rollback. */
enum class AiOpsActionType { UPDATE_FIELD, MERGE, LINK, DEACTIVATE, CREATE, SPLIT, NO_OP }

/** Confidence band the gate reasons over. */
enum class AiOpsBand { HIGH, MEDIUM, LOW }

/** How much damage a wrong action does — a property of the field/action, not the model. */
enum class AiOpsRiskLevel { LOW, MEDIUM, HIGH }

/**
 * Per-workspace autonomy level (stored in workspace settings; default [RECOMMEND]).
 * The ordinal IS the level (L0–L4), matching the framework's L0–L4.
 */
enum class AiOpsAutonomyLevel {
    OBSERVE,        // L0 — detect only
    RECOMMEND,      // L1 — propose, never change (default)
    AUTO_CORRECT,   // L2 — auto-fix low-risk, high-confidence, reversible
    AUTO_EXECUTE,   // L3 — routine ops per policy (backend tier)
    AUTONOMOUS;     // L4 — continuous (backend tier)

    val level: Int get() = ordinal

    companion object {
        val Default: AiOpsAutonomyLevel = RECOMMEND

        /** Parse a stored level (int or name); falls back to [Default] on anything unrecognized. */
        fun fromStored(value: String?): AiOpsAutonomyLevel {
            if (value.isNullOrBlank()) return Default
            value.toIntOrNull()?.let { i -> entries.getOrNull(i)?.let { return it } }
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: Default
        }
    }
}

/** Where a capability runs / reads. Slice-1 needs only the workspace id (one instance = one workspace). */
data class AiOpsScope(
    val workspaceId: String,
)

/** A problem a [AiOpsCapability] detected. [entityType]/[entityId] point at the owning module's aggregate. */
data class Finding(
    val id: String,
    val capability: String,          // e.g. "product.unit"
    val entityType: String,          // e.g. "product"
    val entityId: String,            // the owning module's aggregate uid
    val field: String? = null,       // affected field, for a single-field issue
    val summary: String = "",        // human-readable "what's wrong"
    val signals: Map<String, String> = emptyMap(), // raw evidence the detector saw
)

/** Facts gathered for a [Finding] so the generator/scorer can reason. A simple string bag for now. */
data class FindingContext(
    val values: Map<String, String> = emptyMap(),
)

/** A proposed fix. [before]/[after] are what make the action reversible (rollback re-applies [before]). */
data class Candidate(
    val field: String? = null,
    val before: String? = null,
    val after: String? = null,
    val action: AiOpsActionType = AiOpsActionType.NO_OP,
    val rationale: String = "",              // why — evidence-based, not "the LLM said so"
    val evidence: List<String> = emptyList(),// deterministic facts supporting it
)

/** Result of validating a [Candidate] against DB/rules before it can be scored/applied. */
data class Validation(
    val valid: Boolean,
    val reason: String = "",
)

/** Ensemble confidence for a [Candidate]. [contributors] keeps the per-source scores for the audit. */
data class Confidence(
    val value: Double,
    val band: AiOpsBand,
    val contributors: Map<String, Double> = emptyMap(),
)

/** Outcome of applying a [Candidate] through an [AiOpsExecutor]. */
data class ExecResult(
    val success: Boolean,
    val message: String = "",
)
