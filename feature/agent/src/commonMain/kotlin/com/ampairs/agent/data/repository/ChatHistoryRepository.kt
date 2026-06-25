package com.ampairs.agent.data.repository

import com.ampairs.agent.core.ActionResultSummary
import com.ampairs.agent.core.ChatMessage
import com.ampairs.agent.data.db.dao.ChatMessageDao
import com.ampairs.agent.data.db.entity.ChatMessageEntity
import dev.zacsweers.metro.Inject
import kotlinx.serialization.json.Json

/**
 * Local-only persistence for the assistant chat thread (Room). Restores the recent transcript on
 * open and rewrites it (capped) at each turn boundary so the conversation resumes where it left off.
 * Voice-note bubbles are dropped (audio isn't persisted). Workspace isolation comes from the
 * workspace-scoped [ChatDatabase], so this is unscoped `@Inject` like other repositories.
 */
@Inject
class ChatHistoryRepository(private val dao: ChatMessageDao) {

    private val json = Json { ignoreUnknownKeys = true }

    /** Restore the most recent [limit] messages, oldest-first. */
    suspend fun load(limit: Int): List<ChatMessage> =
        dao.getAll().takeLast(limit).map { it.toMessage() }

    /** Persist the most recent [limit] non-voice-note messages, replacing the stored thread. */
    suspend fun save(messages: List<ChatMessage>, limit: Int) {
        val capped = messages.filterNot { it.isVoiceNote }.takeLast(limit)
        dao.replaceAll(capped.mapIndexed { index, message -> message.toEntity(index) })
    }

    suspend fun clear() = dao.clear()

    private fun ChatMessage.toEntity(seq: Int) = ChatMessageEntity(
        seq = seq,
        id = id,
        text = text,
        isFromUser = isFromUser,
        timestamp = timestamp,
        isError = isError,
        amount = amount,
        actionResultJson = actionResult?.let { json.encodeToString(ActionResultSummary.serializer(), it) },
    )

    private fun ChatMessageEntity.toMessage() = ChatMessage(
        id = id,
        text = text,
        isFromUser = isFromUser,
        timestamp = timestamp,
        isError = isError,
        amount = amount,
        actionResult = actionResultJson?.let {
            runCatching { json.decodeFromString(ActionResultSummary.serializer(), it) }.getOrNull()
        },
    )
}
