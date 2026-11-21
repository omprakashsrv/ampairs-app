package com.ampairs.customer.aiops

import com.ampairs.common.aiops.AiOpsActionType
import com.ampairs.common.aiops.AiOpsBand
import com.ampairs.common.aiops.AiOpsRiskLevel
import com.ampairs.common.aiops.AiOpsScope
import com.ampairs.common.aiops.Candidate
import com.ampairs.common.aiops.Finding
import com.ampairs.common.aiops.FindingContext
import com.ampairs.customer.data.repository.CustomerRepository
import com.ampairs.customer.data.repository.FakeCustomerDao
import com.ampairs.customer.data.repository.RecordingSyncStateDao
import com.ampairs.customer.domain.Customer
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Stage tests for the customer GSTIN-normalization capability. Reuses the module's in-memory customer
 * DAO fake (shared with the repository test) so `detect` runs against real seeded rows — no Room.
 */
class CustomerGstCapabilityTest {

    private val customerDao = FakeCustomerDao()
    private val repository = CustomerRepository(customerDao, RecordingSyncStateDao())
    private val capability = CustomerGstCapability(customerDao)

    private suspend fun seed(uid: String, gst: String?, active: Boolean = true) {
        repository.createCustomer(Customer(uid = uid, name = "Name-$uid", gstNumber = gst, active = active))
    }

    private fun finding(entityId: String = "C1") =
        Finding(id = "x", capability = CustomerGstCapability.KEY, entityType = "customer", entityId = entityId)

    @Test
    fun `detect flags a non-canonical gstin and skips a canonical one`() = runTest {
        seed("C1", "22aaaaa0000a1z5")
        seed("C2", "29ABCDE1234F2Z6")

        val findings = capability.detect(AiOpsScope("W1"))

        assertEquals(1, findings.size)
        val f = findings.single()
        assertEquals("C1", f.entityId)
        assertEquals("customer", f.entityType)
        assertEquals("22AAAAA0000A1Z5", f.signals["canonical"])
        assertEquals(CustomerGstCapability.findingId("C1"), f.id)
    }

    @Test
    fun `detect ignores blank and inactive rows`() = runTest {
        seed("C1", null)
        seed("C2", "22aaaaa0000a1z5", active = false)

        assertTrue(capability.detect(AiOpsScope("W1")).isEmpty())
    }

    @Test
    fun `propose returns a reversible UPDATE_FIELD candidate to the canonical gstin`() = runTest {
        val candidate = capability.propose(
            finding = finding(),
            context = FindingContext(values = mapOf("current" to " 22aaaaa0000a1z5 ")),
        ).single()

        assertEquals(AiOpsActionType.UPDATE_FIELD, candidate.action)
        assertEquals(" 22aaaaa0000a1z5 ", candidate.before)
        assertEquals("22AAAAA0000A1Z5", candidate.after)
        assertEquals("gstNumber", candidate.field)
    }

    @Test
    fun `validate rejects unchanged and non-canonical targets, accepts a real rewrite`() = runTest {
        val f = finding()
        val ctx = FindingContext()

        val unchanged = Candidate(field = "gstNumber", before = "22AAAAA0000A1Z5", after = "22AAAAA0000A1Z5", action = AiOpsActionType.UPDATE_FIELD)
        assertFalse(capability.validate(f, unchanged, ctx).valid)

        val notCanonical = Candidate(field = "gstNumber", before = "22AAAAA0000A1Z5", after = "22aaaaa0000a1z5", action = AiOpsActionType.UPDATE_FIELD)
        assertFalse(capability.validate(f, notCanonical, ctx).valid)

        val good = Candidate(field = "gstNumber", before = "22aaaaa0000a1z5", after = "22AAAAA0000A1Z5", action = AiOpsActionType.UPDATE_FIELD)
        assertTrue(capability.validate(f, good, ctx).valid)
    }

    @Test
    fun `score is HIGH band for a deterministic normalization`() = runTest {
        val candidate = Candidate(field = "gstNumber", before = "22aaaaa0000a1z5", after = "22AAAAA0000A1Z5", action = AiOpsActionType.UPDATE_FIELD)
        val confidence = capability.score(finding(), candidate, FindingContext())

        assertEquals(AiOpsBand.HIGH, confidence.band)
        assertTrue(confidence.value >= 0.99)
    }

    @Test
    fun `capability metadata is customer low-risk`() {
        assertEquals("customer.gst", capability.key)
        assertEquals("customer", capability.entityType)
        assertEquals(AiOpsRiskLevel.LOW, capability.riskLevel)
    }
}
