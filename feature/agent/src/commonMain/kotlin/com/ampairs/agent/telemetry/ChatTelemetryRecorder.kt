package com.ampairs.agent.telemetry

import com.ampairs.agent.data.api.ChatLogApi
import com.ampairs.agent.data.api.ChatLogRequest
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Best-effort uploader for opt-in chat telemetry. Each recorded turn is sent immediately (together
 * with any previously-failed turns); on failure the batch is re-buffered and retried on the next
 * record. The buffer is in-memory (a singleton that outlives the chat ViewModel) and capped, so an
 * extended offline period drops the oldest turns rather than growing without bound. This is telemetry
 * to improve the assistant later — guaranteed delivery is intentionally not a goal, so there is no
 * local DB or sync-delegate machinery.
 *
 * The opt-in gate (default OFF) lives at the call site (ChatViewModel) — nothing is recorded unless
 * the user enabled telemetry.
 */
@Inject
@SingleIn(AppScope::class)
class ChatTelemetryRecorder(
    private val api: ChatLogApi,
) {
    private val mutex = Mutex()
    private val buffer = ArrayDeque<ChatLogRequest>()

    /** Buffer + flush one turn. Safe to call frequently; sends are de-duplicated by draining. */
    suspend fun record(entry: ChatLogRequest) {
        val batch = mutex.withLock {
            buffer.addLast(entry)
            trimToCap()
            buffer.toList().also { buffer.clear() }
        }
        send(batch)
    }

    private suspend fun send(batch: List<ChatLogRequest>) {
        if (batch.isEmpty()) return
        val ok = runCatching {
            val response = api.upload(batch)
            response.data != null && response.error == null
        }.getOrDefault(false)
        if (!ok) {
            // Re-buffer (front) so the next record retries these first, then re-apply the cap.
            mutex.withLock {
                batch.asReversed().forEach { buffer.addFirst(it) }
                trimToCap()
            }
        }
    }

    /** Drop oldest entries beyond [MAX_BUFFER] (called while holding [mutex]). */
    private fun trimToCap() {
        while (buffer.size > MAX_BUFFER) buffer.removeFirst()
    }

    companion object {
        private const val MAX_BUFFER = 100
    }
}
