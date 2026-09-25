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
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Stage tests for the customer contact-completeness advisory. This is the first advisory (detect-only,
 * never auto-fixed) capability, so the test also pins the advisory contract from the shared base: the
 * proposed candidate is a non-reversible NO_OP (which is what forces the engine gate to route it to
 * review, never auto-fix). Reuses the module's in-memory `FakeCustomerDao`.
 */
class CustomerContactCapabilityTest {

    private val customerDao = FakeCustomerDao()
    private val repository = CustomerRepository(customerDao, RecordingSyncStateDao())
    private val capability = CustomerContactCapability(customerDao)

    private suspend fun seed(uid: String, email: String?, phone: String?, active: Boolean = true) {
        repository.createCustomer(Customer(uid = uid, name = "Name-$uid", email = email, phone = phone, active = active))
    }

    private fun finding(entityId: String = "C1") =
        Finding(id = "x", capability = CustomerContactCapability.KEY, entityType = "customer", entityId = entityId)

    @Test
    fun `detect flags a customer with neither email nor phone`() = runTest {
        seed("C1", email = null, phone = null)
        seed("C2", email = "a@b.com", phone = null)   // has email
        seed("C3", email = null, phone = "123")        // has phone
        seed("C4", email = null, phone = null, active = false) // inactive
        seed("C5", email = "  ", phone = "")            // blank both → also flagged

        val findings = capability.detect(AiOpsScope("W1"))

        assertEquals(setOf("C1", "C5"), findings.map { it.entityId }.toSet())
        val c1 = findings.single { it.entityId == "C1" }
        assertEquals("customer", c1.entityType)
        assertEquals(CustomerContactCapability.findingId("C1"), c1.id)
        assertTrue(c1.summary.contains("no email or phone"))
    }

    @Test
    fun `propose emits a non-reversible NO_OP advisory candidate`() = runTest {
        val candidate = capability.propose(finding(), FindingContext()).single()

        assertEquals(AiOpsActionType.NO_OP, candidate.action)
        assertNull(candidate.before, "advisory must be non-reversible so the gate never auto-fixes it")
        assertNull(candidate.after)
    }

    @Test
    fun `validate accepts the advisory candidate and rejects a field-write candidate`() = runTest {
        val advisory = Candidate(before = null, after = null, action = AiOpsActionType.NO_OP)
        assertTrue(capability.validate(finding(), advisory, FindingContext()).valid)

        val fieldWrite = Candidate(field = "email", before = "x", after = "y", action = AiOpsActionType.UPDATE_FIELD)
        assertFalse(capability.validate(finding(), fieldWrite, FindingContext()).valid)
    }

    @Test
    fun `score is HIGH band for a deterministic flag`() = runTest {
        val advisory = Candidate(before = null, after = null, action = AiOpsActionType.NO_OP)
        val confidence = capability.score(finding(), advisory, FindingContext())

        assertEquals(AiOpsBand.HIGH, confidence.band)
        assertTrue(confidence.value >= 0.99)
    }

    @Test
    fun `capability metadata is customer low-risk`() {
        assertEquals("customer.contact", capability.key)
        assertEquals("customer", capability.entityType)
        assertEquals(AiOpsRiskLevel.LOW, capability.riskLevel)
    }
}
