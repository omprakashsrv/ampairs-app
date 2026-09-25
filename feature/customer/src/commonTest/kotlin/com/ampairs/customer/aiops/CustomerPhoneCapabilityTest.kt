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
 * Stage tests for the customer phone-normalization capability. Reuses the module's in-memory customer
 * DAO fake (shared with the repository test) so `detect` runs against real seeded rows — no Room.
 */
class CustomerPhoneCapabilityTest {

    private val customerDao = FakeCustomerDao()
    private val repository = CustomerRepository(customerDao, RecordingSyncStateDao())
    private val capability = CustomerPhoneCapability(customerDao)

    private suspend fun seed(uid: String, phone: String?, active: Boolean = true) {
        repository.createCustomer(Customer(uid = uid, name = "Name-$uid", phone = phone, active = active))
    }

    private fun finding(entityId: String = "C1") =
        Finding(id = "x", capability = CustomerPhoneCapability.KEY, entityType = "customer", entityId = entityId)

    @Test
    fun `detect flags a formatted phone and skips a canonical one`() = runTest {
        seed("C1", "234-567-8900")
        seed("C2", "9876543210")

        val findings = capability.detect(AiOpsScope("W1"))

        assertEquals(1, findings.size)
        val f = findings.single()
        assertEquals("C1", f.entityId)
        assertEquals("customer", f.entityType)
        assertEquals("2345678900", f.signals["canonical"])
        assertEquals(CustomerPhoneCapability.findingId("C1"), f.id)
    }

    @Test
    fun `detect ignores blank, digitless, and inactive rows`() = runTest {
        seed("C1", null)
        seed("C2", "n/a")
        seed("C3", "234-567-8900", active = false)

        assertTrue(capability.detect(AiOpsScope("W1")).isEmpty())
    }

    @Test
    fun `propose returns a reversible UPDATE_FIELD candidate to the canonical phone`() = runTest {
        val candidate = capability.propose(
            finding = finding(),
            context = FindingContext(values = mapOf("current" to "+1 (234) 567-8900")),
        ).single()

        assertEquals(AiOpsActionType.UPDATE_FIELD, candidate.action)
        assertEquals("+1 (234) 567-8900", candidate.before)
        assertEquals("+12345678900", candidate.after)
        assertEquals("phone", candidate.field)
    }

    @Test
    fun `validate rejects unchanged, digitless, and non-canonical targets, accepts a real rewrite`() = runTest {
        val f = finding()
        val ctx = FindingContext()

        val unchanged = Candidate(field = "phone", before = "2345678900", after = "2345678900", action = AiOpsActionType.UPDATE_FIELD)
        assertFalse(capability.validate(f, unchanged, ctx).valid)

        val digitless = Candidate(field = "phone", before = "n / a", after = "n/a", action = AiOpsActionType.UPDATE_FIELD)
        assertFalse(capability.validate(f, digitless, ctx).valid)

        val notCanonical = Candidate(field = "phone", before = "234-567-8900", after = "234 5678900", action = AiOpsActionType.UPDATE_FIELD)
        assertFalse(capability.validate(f, notCanonical, ctx).valid)

        val good = Candidate(field = "phone", before = "234-567-8900", after = "2345678900", action = AiOpsActionType.UPDATE_FIELD)
        assertTrue(capability.validate(f, good, ctx).valid)
    }

    @Test
    fun `score is HIGH band for a deterministic normalization`() = runTest {
        val candidate = Candidate(field = "phone", before = "234-567-8900", after = "2345678900", action = AiOpsActionType.UPDATE_FIELD)
        val confidence = capability.score(finding(), candidate, FindingContext())

        assertEquals(AiOpsBand.HIGH, confidence.band)
        assertTrue(confidence.value >= 0.99)
    }

    @Test
    fun `capability metadata is customer low-risk`() {
        assertEquals("customer.phone", capability.key)
        assertEquals("customer", capability.entityType)
        assertEquals(AiOpsRiskLevel.LOW, capability.riskLevel)
    }
}
