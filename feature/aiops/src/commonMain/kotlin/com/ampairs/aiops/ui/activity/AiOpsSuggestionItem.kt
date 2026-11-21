package com.ampairs.aiops.ui.activity

import com.ampairs.aiops.db.entity.AiOpsFindingEntity

/**
 * A display row for one pending AI Ops suggestion (a `PENDING_REVIEW` finding). Pure view model over
 * [AiOpsFindingEntity] so the screen stays dumb and the mapping is trivially testable.
 */
data class AiOpsSuggestionItem(
    val findingId: String,
    val capability: String,
    val summary: String,
)

fun AiOpsFindingEntity.toSuggestionItem(): AiOpsSuggestionItem = AiOpsSuggestionItem(
    findingId = id,
    capability = capability,
    summary = summary,
)
