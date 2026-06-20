package com.ampairs.printing.core.spool

import com.ampairs.printing.core.model.PrintJobState

/** A queued print job (device-local, never synced). */
data class PrintJob(
    val id: String,
    val idempotencyKey: String,
    val documentType: String,
    val documentId: String,
    val templateId: String,
    val printerId: String,
    val copies: Int = 1,
    val state: PrintJobState = PrintJobState.QUEUED,
    val attempts: Int = 0,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val lastError: String? = null,
)

/** Outcome of a send attempt as reported by the transport/service. */
enum class SendOutcome { CONFIRMED, SENT_UNCONFIRMED, TRANSIENT_FAILURE, PERMANENT_FAILURE }

/**
 * Pure, side-effect-free spool decision logic (§19 reliability spine). Keeps the print-once / retry /
 * dead-letter / TTL rules independent of IO so they are fully unit-testable. The orchestrating
 * PrintService (app layer) applies these transitions and performs the actual transport work.
 */
object SpoolPolicy {

    const val DEFAULT_MAX_ATTEMPTS = 5
    const val DEFAULT_TTL_MILLIS = 12L * 60 * 60 * 1000 // 12h — don't print stale jobs

    /** Next state after a send attempt. UNCONFIRMED is terminal-until-user — never auto-retried. */
    fun afterSend(job: PrintJob, outcome: SendOutcome, maxAttempts: Int = DEFAULT_MAX_ATTEMPTS): PrintJobState =
        when (outcome) {
            SendOutcome.CONFIRMED -> PrintJobState.CONFIRMED
            SendOutcome.SENT_UNCONFIRMED -> PrintJobState.UNCONFIRMED
            SendOutcome.PERMANENT_FAILURE -> PrintJobState.DEAD
            SendOutcome.TRANSIENT_FAILURE ->
                if (job.attempts + 1 >= maxAttempts) PrintJobState.DEAD else PrintJobState.FAILED
        }

    /** Whether a FAILED job is eligible to retry now (not expired, attempts left). */
    fun shouldRetry(
        job: PrintJob,
        nowMillis: Long,
        maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
        ttlMillis: Long = DEFAULT_TTL_MILLIS,
    ): Boolean {
        if (job.state != PrintJobState.FAILED) return false
        if (job.attempts >= maxAttempts) return false
        if (isExpired(job, nowMillis, ttlMillis)) return false
        return true
    }

    /** Stale jobs past their TTL must never print (a queued receipt should not emerge tomorrow). */
    fun isExpired(job: PrintJob, nowMillis: Long, ttlMillis: Long = DEFAULT_TTL_MILLIS): Boolean =
        nowMillis - job.createdAtMillis > ttlMillis

    /** Exponential backoff (ms) before the next retry of a FAILED job. */
    fun backoffMillis(attempts: Int): Long {
        val capped = attempts.coerceIn(0, 6)
        return 1000L * (1 shl capped) // 1s, 2s, 4s ... capped at 64s
    }

    /** Sent-but-unconfirmed jobs require an explicit user decision (reprint / mark printed). */
    fun needsUserDecision(job: PrintJob): Boolean = job.state == PrintJobState.UNCONFIRMED

    /**
     * A job already in-flight or finished must NOT be re-sent for the same idempotency key — this is
     * what makes "print once" hold across retries/reconnects. Only QUEUED / FAILED / DEAD jobs may be
     * (re)started, and DEAD/FAILED only via an explicit user retry.
     */
    fun blocksReprint(state: PrintJobState): Boolean =
        state == PrintJobState.SENDING ||
            state == PrintJobState.SENT ||
            state == PrintJobState.CONFIRMED ||
            state == PrintJobState.UNCONFIRMED

    /** Best-effort outcome to report for an already-handled job, without touching the device again. */
    fun outcomeFor(state: PrintJobState): SendOutcome = when (state) {
        PrintJobState.CONFIRMED -> SendOutcome.CONFIRMED
        else -> SendOutcome.SENT_UNCONFIRMED
    }
}
