package com.ampairs.common.aiops

/**
 * What the engine did for one `onEntitySaved` call, returned so the calling feature's UI can surface
 * it (an "AI fixed → … · Undo" snackbar for auto-fixes, a lighter note for suggestions). Empty when
 * nothing applied — the common case.
 */
data class AiOpsOutcome(
    val autoFixed: List<AiOpsAppliedFix> = emptyList(),
    val suggestions: List<AiOpsSuggestion> = emptyList(),
) {
    val isEmpty: Boolean get() = autoFixed.isEmpty() && suggestions.isEmpty()

    companion object {
        val None = AiOpsOutcome()
    }
}

/** An auto-applied fix, with the [decisionId] the UI needs to offer Undo. */
data class AiOpsAppliedFix(
    val decisionId: String,
    val capability: String,
    val field: String?,
    val before: String?,
    val after: String?,
    val summary: String,
)

/** A recorded suggestion the user can review (no automatic change was made). */
data class AiOpsSuggestion(
    val findingId: String,
    val capability: String,
    val summary: String,
)
