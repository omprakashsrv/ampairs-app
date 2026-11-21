package com.ampairs.aiops.gate

import com.ampairs.common.aiops.AiOpsAutonomyLevel
import com.ampairs.common.aiops.AiOpsBand
import com.ampairs.common.aiops.AiOpsRiskLevel
import com.ampairs.common.aiops.Candidate
import com.ampairs.common.aiops.Confidence
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The gate is the safety kernel: `autoFix ⇔ band = HIGH ∧ risk = LOW ∧ reversible ∧ level ≥ AUTO_CORRECT`.
 * Every other detected candidate becomes a human exception (SUGGEST), except at OBSERVE (record only).
 */
class ConfidenceRiskGateTest {

    private val gate = ConfidenceRiskGate()

    private fun conf(band: AiOpsBand, value: Double = 0.99) = Confidence(value, band)
    private fun candidate(reversible: Boolean = true) =
        Candidate(field = "f", before = if (reversible) "old" else null, after = "new")

    @Test
    fun `observe autonomy always yields observe-only`() {
        val decision = gate.decide(conf(AiOpsBand.HIGH), AiOpsRiskLevel.LOW, candidate(), AiOpsAutonomyLevel.OBSERVE)
        assertEquals(GateDecision.OBSERVE_ONLY, decision)
    }

    @Test
    fun `recommend never auto-fixes even a perfect candidate`() {
        val decision = gate.decide(conf(AiOpsBand.HIGH), AiOpsRiskLevel.LOW, candidate(), AiOpsAutonomyLevel.RECOMMEND)
        assertEquals(GateDecision.SUGGEST, decision)
    }

    @Test
    fun `auto_correct auto-fixes high-confidence low-risk reversible`() {
        val decision = gate.decide(conf(AiOpsBand.HIGH), AiOpsRiskLevel.LOW, candidate(), AiOpsAutonomyLevel.AUTO_CORRECT)
        assertEquals(GateDecision.AUTO_FIX, decision)
    }

    @Test
    fun `auto_correct only suggests when confidence is not HIGH`() {
        val decision = gate.decide(conf(AiOpsBand.MEDIUM), AiOpsRiskLevel.LOW, candidate(), AiOpsAutonomyLevel.AUTO_CORRECT)
        assertEquals(GateDecision.SUGGEST, decision)
    }

    @Test
    fun `auto_correct only suggests when risk is not LOW`() {
        val decision = gate.decide(conf(AiOpsBand.HIGH), AiOpsRiskLevel.HIGH, candidate(), AiOpsAutonomyLevel.AUTO_CORRECT)
        assertEquals(GateDecision.SUGGEST, decision)
    }

    @Test
    fun `auto_correct only suggests when the candidate is not reversible`() {
        val decision = gate.decide(conf(AiOpsBand.HIGH), AiOpsRiskLevel.LOW, candidate(reversible = false), AiOpsAutonomyLevel.AUTO_CORRECT)
        assertEquals(GateDecision.SUGGEST, decision)
    }
}
