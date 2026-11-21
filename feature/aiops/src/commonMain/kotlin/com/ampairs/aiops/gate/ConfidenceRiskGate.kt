package com.ampairs.aiops.gate

import com.ampairs.common.aiops.AiOpsAutonomyLevel
import com.ampairs.common.aiops.AiOpsBand
import com.ampairs.common.aiops.AiOpsRiskLevel
import com.ampairs.common.aiops.Candidate
import com.ampairs.common.aiops.Confidence
import dev.zacsweers.metro.Inject

/** What the gate decides to do with a scored candidate. */
enum class GateDecision {
    /** Apply automatically now (high confidence, low risk, reversible, autonomy allows). */
    AUTO_FIX,

    /** Surface to the user as a suggestion / review item. */
    SUGGEST,

    /** Record the finding only; take no action (autonomy = Observe). */
    OBSERVE_ONLY,
}

/**
 * The confidence × risk × autonomy gate (framework §4). The LLM is only one (capped) input to the
 * [Confidence] the caller passes here — this gate never sees the model directly.
 *
 * `autoFix ⇔ band = HIGH ∧ risk = LOW ∧ reversible ∧ level ≥ AUTO_CORRECT`. Anything else that the
 * engine detected becomes a human exception (SUGGEST), except at Observe (record only).
 */
@Inject
class ConfidenceRiskGate {

    fun decide(
        confidence: Confidence,
        risk: AiOpsRiskLevel,
        candidate: Candidate,
        level: AiOpsAutonomyLevel,
    ): GateDecision {
        if (level == AiOpsAutonomyLevel.OBSERVE) return GateDecision.OBSERVE_ONLY

        val reversible = candidate.before != null
        val autoEligible = confidence.band == AiOpsBand.HIGH &&
            risk == AiOpsRiskLevel.LOW &&
            reversible &&
            level.level >= AiOpsAutonomyLevel.AUTO_CORRECT.level

        return if (autoEligible) GateDecision.AUTO_FIX else GateDecision.SUGGEST
    }
}
