package com.ampairs.agent.data.db.entity

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * One persisted assistant chat message (resume-on-reopen). The thread is workspace-isolated because
 * [com.ampairs.agent.data.db.ChatDatabase] is a workspace-scoped DB file; [seq] is the message's
 * position in the (capped) thread, so the table is rewritten in order on each save. [actionResultJson]
 * holds the serialized [com.ampairs.agent.core.ActionResultSummary] (null for plain messages).
 * Voice-note bubbles are not persisted (their audio is in-memory), so no audio columns here.
 */
@Entity(
    tableName = "chat_messages",
    indices = [Index(value = ["id"], unique = true, name = "chat_message_id_idx")],
)
data class ChatMessageEntity(
    @PrimaryKey val seq: Int,
    val id: String,
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long,
    val isError: Boolean,
    val amount: Double?,
    val actionResultJson: String?,
)
