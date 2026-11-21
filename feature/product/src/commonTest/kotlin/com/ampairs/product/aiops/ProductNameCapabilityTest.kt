package com.ampairs.product.aiops

import com.ampairs.common.aiops.AiOpsActionType
import com.ampairs.common.aiops.AiOpsBand
import com.ampairs.common.aiops.AiOpsRiskLevel
import com.ampairs.common.aiops.AiOpsScope
import com.ampairs.common.aiops.Candidate
import com.ampairs.common.aiops.Finding
import com.ampairs.common.aiops.FindingContext
import com.ampairs.product.data.repository.FakeProductDao
import com.ampairs.product.domain.Product
import com.ampairs.product.domain.asDatabaseModel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Stage tests for the product name-whitespace-normalization capability. Reuses the module's in-memory
 * product DAO fake (shared with the repository test) so `detect` runs against real seeded rows — no Room.
 */
class ProductNameCapabilityTest {

    private val productDao = FakeProductDao()
    private val capability = ProductNameCapability(productDao)

    private suspend fun seed(id: String, name: String, active: Boolean = true) {
        productDao.insert(Product(id = id, name = name, code = "SKU-$id", active = active).asDatabaseModel())
    }

    private fun finding(entityId: String = "P1") =
        Finding(id = "x", capability = ProductNameCapability.KEY, entityType = "product", entityId = entityId)

    @Test
    fun `detect flags a double-spaced name and skips a tidy one`() = runTest {
        seed("P1", "Blue  Widget")
        seed("P2", "Steel Bolt")

        val findings = capability.detect(AiOpsScope("W1"))

        assertEquals(1, findings.size)
        val f = findings.single()
        assertEquals("P1", f.entityId)
        assertEquals("product", f.entityType)
        assertEquals("Blue Widget", f.signals["canonical"])
        assertEquals(ProductNameCapability.findingId("P1"), f.id)
    }

    @Test
    fun `detect ignores blank and inactive rows`() = runTest {
        seed("P1", "   ")
        seed("P2", "Big  Box", active = false)

        assertTrue(capability.detect(AiOpsScope("W1")).isEmpty())
    }

    @Test
    fun `propose returns a reversible UPDATE_FIELD candidate to the tidied name`() = runTest {
        val candidate = capability.propose(
            finding = finding(),
            context = FindingContext(values = mapOf("current" to "  Blue   Widget ")),
        ).single()

        assertEquals(AiOpsActionType.UPDATE_FIELD, candidate.action)
        assertEquals("  Blue   Widget ", candidate.before)
        assertEquals("Blue Widget", candidate.after)
        assertEquals("name", candidate.field)
    }

    @Test
    fun `validate rejects unchanged and non-canonical targets, accepts a real rewrite`() = runTest {
        val f = finding()
        val ctx = FindingContext()

        val unchanged = Candidate(field = "name", before = "Blue Widget", after = "Blue Widget", action = AiOpsActionType.UPDATE_FIELD)
        assertFalse(capability.validate(f, unchanged, ctx).valid)

        val notCanonical = Candidate(field = "name", before = "Blue Widget", after = "Blue  Widget", action = AiOpsActionType.UPDATE_FIELD)
        assertFalse(capability.validate(f, notCanonical, ctx).valid)

        val good = Candidate(field = "name", before = "Blue  Widget", after = "Blue Widget", action = AiOpsActionType.UPDATE_FIELD)
        assertTrue(capability.validate(f, good, ctx).valid)
    }

    @Test
    fun `score is HIGH band for a deterministic normalization`() = runTest {
        val candidate = Candidate(field = "name", before = "Blue  Widget", after = "Blue Widget", action = AiOpsActionType.UPDATE_FIELD)
        val confidence = capability.score(finding(), candidate, FindingContext())

        assertEquals(AiOpsBand.HIGH, confidence.band)
        assertTrue(confidence.value >= 0.99)
    }

    @Test
    fun `capability metadata is product low-risk`() {
        assertEquals("product.name", capability.key)
        assertEquals("product", capability.entityType)
        assertEquals(AiOpsRiskLevel.LOW, capability.riskLevel)
    }
}
