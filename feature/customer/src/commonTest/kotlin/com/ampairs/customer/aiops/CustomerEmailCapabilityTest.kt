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
 * Stage tests for the customer email-normalization capability. Reuses the module's in-memory customer
 * DAO fake (shared with the repository test) so `detect` runs against real seeded rows — no Room.
 */
class CustomerEmailCapabilityTest {

    private val customerDao = FakeCustomerDao()
    private val repository = CustomerRepository(customerDao, RecordingSyncStateDao())
    private val capability = CustomerEmailCapability(customerDao)

    private suspend fun seed(uid: String, email: String?, active: Boolean = true) {
        repository.createCustomer(Customer(uid = uid, name = "Name-$uid", email = email, active = active))
    }

    private fun finding(entityId: String = "C1") =
        Finding(id = "x", capability = CustomerEmailCapability.KEY, entityType = "customer", entityId = entityId)

    @Test
    fun `detect flags a non-canonical email and skips a canonical one`() = runTest {
        seed("C1", "Jo@X.CoM")
        seed("C2", "ok@x.com")

        val findings = capability.detect(AiOpsScope("W1"))

        assertEquals(1, findings.size)
        val f = findings.single()
        assertEquals("C1", f.entityId)
        assertEquals("customer", f.entityType)
        assertEquals("jo@x.com", f.signals["canonical"])
        assertEquals(CustomerEmailCapability.findingId("C1"), f.id)
    }

    @Test
    fun `detect ignores blank, non-email, and inactive rows`() = runTest {
        seed("C1", null)
        seed("C2", "NotAnEmail")
        seed("C3", "Jo@X.com", active = false)

        assertTrue(capability.detect(AiOpsScope("W1")).isEmpty())
    }

    @Test
    fun `propose returns a reversible UPDATE_FIELD candidate to the canonical email`() = runTest {
        val candidate = capability.propose(
            finding = finding(),
            context = FindingContext(values = mapOf("current" to " Jo@X.CoM ")),
        ).single()

        assertEquals(AiOpsActionType.UPDATE_FIELD, candidate.action)
        assertEquals(" Jo@X.CoM ", candidate.before)
        assertEquals("jo@x.com", candidate.after)
        assertEquals("email", candidate.field)
    }

    @Test
    fun `validate rejects unchanged, non-email, and non-canonical targets; accepts a real rewrite`() = runTest {
        val f = finding()
        val ctx = FindingContext()

        val unchanged = Candidate(field = "email", before = "a@b.com", after = "a@b.com", action = AiOpsActionType.UPDATE_FIELD)
        assertFalse(capability.validate(f, unchanged, ctx).valid)

        val notEmail = Candidate(field = "email", before = "A B", after = "a b", action = AiOpsActionType.UPDATE_FIELD)
        assertFalse(capability.validate(f, notEmail, ctx).valid)

        val notCanonical = Candidate(field = "email", before = "Jo@X.com", after = "Jo@X.com ", action = AiOpsActionType.UPDATE_FIELD)
        assertFalse(capability.validate(f, notCanonical, ctx).valid)

        val good = Candidate(field = "email", before = "Jo@X.com", after = "jo@x.com", action = AiOpsActionType.UPDATE_FIELD)
        assertTrue(capability.validate(f, good, ctx).valid)
    }

    @Test
    fun `score is HIGH band for a deterministic normalization`() = runTest {
        val candidate = Candidate(field = "email", before = "Jo@X.com", after = "jo@x.com", action = AiOpsActionType.UPDATE_FIELD)
        val confidence = capability.score(finding(), candidate, FindingContext())

        assertEquals(AiOpsBand.HIGH, confidence.band)
        assertTrue(confidence.value >= 0.99)
    }

    @Test
    fun `capability metadata is customer low-risk`() {
        assertEquals("customer.email", capability.key)
        assertEquals("customer", capability.entityType)
        assertEquals(AiOpsRiskLevel.LOW, capability.riskLevel)
    }
}
