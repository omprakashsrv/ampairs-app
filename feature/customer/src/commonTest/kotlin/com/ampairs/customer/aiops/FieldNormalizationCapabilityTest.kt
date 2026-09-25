package com.ampairs.customer.aiops

import com.ampairs.common.aiops.AiOpsActionType
import com.ampairs.common.aiops.AiOpsBand
import com.ampairs.common.aiops.AiOpsRiskLevel
import com.ampairs.common.aiops.AiOpsScope
import com.ampairs.common.aiops.Candidate
import com.ampairs.common.aiops.FieldNormalizationCapability
import com.ampairs.common.aiops.Finding
import com.ampairs.common.aiops.FindingContext
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Direct contract test for the shared [FieldNormalizationCapability] base — the propose/validate/score/
 * findingFor logic that customer.email/phone/gst/name and product.code all inherit. The per-capability
 * stage tests exercise this indirectly through one normalizer each; this pins the base's behavior once,
 * including the guard-chain ordering (notably `rejectTarget` winning over the canonical check) that every
 * concrete capability now relies on. Uses a trivial trim+lowercase fake so the base is tested in isolation.
 */
class FieldNormalizationCapabilityTest {

    /** Minimal concrete capability over a trim+lowercase normalizer; `rejector` drives the target guard. */
    private class FakeFieldCapability(
        private val rejector: (String) -> String? = { null },
    ) : FieldNormalizationCapability() {
        override val key: String = "test.field"
        override val entityType: String = "widget"
        override val riskLevel: AiOpsRiskLevel = AiOpsRiskLevel.LOW
        override val field: String = "f"
        override val evidenceTag: String = "test_normalize"

        override fun normalize(value: String): String = value.trim().lowercase()
        override fun needsNormalization(value: String?): Boolean {
            if (value.isNullOrBlank()) return false
            return value != normalize(value)
        }

        override fun rationale(current: String, canonical: String): String = "r:$current->$canonical"
        override fun summarize(current: String, canonical: String): String = "s:$current->$canonical"
        override fun rejectTarget(after: String): String? = rejector(after)

        override suspend fun detect(scope: AiOpsScope): List<Finding> = emptyList()
        override suspend fun gather(finding: Finding): FindingContext = FindingContext()

        /** Exposes the protected base helper so the test can drive it directly. */
        fun buildFinding(entityId: String, current: String?): Finding? = findingFor(entityId, current)
    }

    private val cap = FakeFieldCapability()

    private fun finding() =
        Finding(id = "x", capability = "test.field", entityType = "widget", entityId = "E1", field = "f")

    @Test
    fun `findingFor builds a stable finding for a non-canonical value`() {
        val f = cap.buildFinding("E1", "AB ")!!

        assertEquals("AIO-test.field-E1", f.id)
        assertEquals("test.field", f.capability)
        assertEquals("widget", f.entityType)
        assertEquals("E1", f.entityId)
        assertEquals("f", f.field)
        assertEquals("AB ", f.signals["current"])
        assertEquals("ab", f.signals["canonical"])
        assertEquals("s:AB ->ab", f.summary)
    }

    @Test
    fun `findingFor returns null for an already-canonical or blank value`() {
        assertNull(cap.buildFinding("E1", "ab"))
        assertNull(cap.buildFinding("E1", ""))
        assertNull(cap.buildFinding("E1", null))
    }

    @Test
    fun `propose emits one reversible UPDATE_FIELD candidate from the context value`() = runTest {
        val candidate = cap.propose(finding(), FindingContext(values = mapOf("current" to "AB "))).single()

        assertEquals("f", candidate.field)
        assertEquals("AB ", candidate.before)
        assertEquals("ab", candidate.after)
        assertEquals(AiOpsActionType.UPDATE_FIELD, candidate.action)
        assertEquals(listOf("test_normalize:AB →ab"), candidate.evidence)
    }

    @Test
    fun `propose falls back to the finding signal when context has no value`() = runTest {
        val f = finding().copy(signals = mapOf("current" to "AB "))
        val candidate = cap.propose(f, FindingContext()).single()
        assertEquals("ab", candidate.after)
    }

    @Test
    fun `propose is empty when the value is already canonical or absent`() = runTest {
        assertTrue(cap.propose(finding(), FindingContext(values = mapOf("current" to "ab"))).isEmpty())
        assertTrue(cap.propose(finding(), FindingContext()).isEmpty())
    }

    @Test
    fun `validate rejects wrong action, blank, unchanged, and non-canonical targets`() = runTest {
        val ctx = FindingContext()
        val wrongAction = Candidate(field = "f", before = "AB", after = "ab", action = AiOpsActionType.NO_OP)
        assertFalse(cap.validate(finding(), wrongAction, ctx).valid)

        val blank = Candidate(field = "f", before = "AB", after = "", action = AiOpsActionType.UPDATE_FIELD)
        assertFalse(cap.validate(finding(), blank, ctx).valid)

        val unchanged = Candidate(field = "f", before = "ab", after = "ab", action = AiOpsActionType.UPDATE_FIELD)
        assertEquals("already normalized", cap.validate(finding(), unchanged, ctx).reason)

        val notCanonical = Candidate(field = "f", before = "AB", after = "aB", action = AiOpsActionType.UPDATE_FIELD)
        assertEquals("target is not canonical", cap.validate(finding(), notCanonical, ctx).reason)

        val good = Candidate(field = "f", before = "AB", after = "ab", action = AiOpsActionType.UPDATE_FIELD)
        assertTrue(cap.validate(finding(), good, ctx).valid)
    }

    @Test
    fun `rejectTarget guard is checked before the canonical check`() = runTest {
        // The target "AB!" is BOTH rejected by the guard AND non-canonical; the guard reason must win.
        val guarded = FakeFieldCapability(rejector = { if (it.contains("!")) "rejected" else null })
        val candidate = Candidate(field = "f", before = "x", after = "AB!", action = AiOpsActionType.UPDATE_FIELD)

        val result = guarded.validate(finding(), candidate, FindingContext())

        assertFalse(result.valid)
        assertEquals("rejected", result.reason)
    }

    @Test
    fun `score is HIGH band with the evidence tag as the sole contributor`() = runTest {
        val candidate = Candidate(field = "f", before = "AB", after = "ab", action = AiOpsActionType.UPDATE_FIELD)
        val confidence = cap.score(finding(), candidate, FindingContext())

        assertEquals(AiOpsBand.HIGH, confidence.band)
        assertTrue(confidence.value >= 0.99)
        assertEquals(mapOf("test_normalize" to 1.0), confidence.contributors)
    }
}
