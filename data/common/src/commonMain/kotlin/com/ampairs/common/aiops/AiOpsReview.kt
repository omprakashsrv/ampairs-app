package com.ampairs.common.aiops

/**
 * Human review actions on a pending AI Ops **suggestion** (a finding recorded at autonomy ≤ RECOMMEND,
 * i.e. `PENDING_REVIEW`). Exposed as a port so a feature's UI can accept/dismiss without depending on
 * the `feature/aiops` impl. Bound in `WorkspaceScope`.
 *
 * - [accept] re-derives the candidate through the owning capability (gather → propose → validate) and
 *   applies it via the capability's executor, recording a reversible audit decision (source HUMAN) —
 *   so an accepted suggestion shows up in the activity feed with Undo, exactly like an auto-fix.
 * - [dismiss] marks the suggestion ignored (no data change) and records the verdict for the learning
 *   loop.
 *
 * Both are no-ops (return false) unless the finding exists and is still `PENDING_REVIEW`.
 */
interface AiOpsReview {
    /** @return true if the suggestion was pending and its fix was successfully applied. */
    suspend fun accept(findingId: String): Boolean

    /** @return true if the suggestion was pending and is now dismissed. */
    suspend fun dismiss(findingId: String): Boolean
}
