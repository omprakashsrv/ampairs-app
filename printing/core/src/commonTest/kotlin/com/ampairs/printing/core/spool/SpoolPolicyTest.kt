package com.ampairs.printing.core.spool

import com.ampairs.printing.core.model.PrintJobState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SpoolPolicyTest {

    private fun job(
        state: PrintJobState = PrintJobState.QUEUED,
        attempts: Int = 0,
        createdAtMillis: Long = 0L,
    ) = PrintJob(
        id = "j1", idempotencyKey = "k1", documentType = "invoice", documentId = "INV-1",
        templateId = "t1", printerId = "p1", state = state, attempts = attempts,
        createdAtMillis = createdAtMillis, updatedAtMillis = createdAtMillis,
    )

    @Test
    fun confirmedSendIsConfirmed() {
        assertEquals(PrintJobState.CONFIRMED, SpoolPolicy.afterSend(job(), SendOutcome.CONFIRMED))
    }

    @Test
    fun unconfirmedSendNeverAutoRetries() {
        val j = job().copy(state = SpoolPolicy.afterSend(job(), SendOutcome.SENT_UNCONFIRMED))
        assertEquals(PrintJobState.UNCONFIRMED, j.state)
        assertFalse(SpoolPolicy.shouldRetry(j, nowMillis = 1_000))
        assertTrue(SpoolPolicy.needsUserDecision(j))
    }

    @Test
    fun transientFailureRetriesUntilMaxThenDead() {
        assertEquals(PrintJobState.FAILED, SpoolPolicy.afterSend(job(attempts = 0), SendOutcome.TRANSIENT_FAILURE, maxAttempts = 3))
        assertEquals(PrintJobState.DEAD, SpoolPolicy.afterSend(job(attempts = 2), SendOutcome.TRANSIENT_FAILURE, maxAttempts = 3))
    }

    @Test
    fun permanentFailureIsDead() {
        assertEquals(PrintJobState.DEAD, SpoolPolicy.afterSend(job(), SendOutcome.PERMANENT_FAILURE))
    }

    @Test
    fun failedJobRetriesWhenFreshAndUnderMax() {
        val j = job(state = PrintJobState.FAILED, attempts = 1, createdAtMillis = 0L)
        assertTrue(SpoolPolicy.shouldRetry(j, nowMillis = 1_000, maxAttempts = 5))
    }

    @Test
    fun staleJobIsExpiredAndNotRetried() {
        val j = job(state = PrintJobState.FAILED, attempts = 1, createdAtMillis = 0L)
        val now = SpoolPolicy.DEFAULT_TTL_MILLIS + 1
        assertTrue(SpoolPolicy.isExpired(j, now))
        assertFalse(SpoolPolicy.shouldRetry(j, now))
    }

    @Test
    fun backoffGrowsExponentiallyAndCaps() {
        assertEquals(1000L, SpoolPolicy.backoffMillis(0))
        assertEquals(2000L, SpoolPolicy.backoffMillis(1))
        assertEquals(64000L, SpoolPolicy.backoffMillis(6))
        assertEquals(64000L, SpoolPolicy.backoffMillis(10)) // capped
    }
}
