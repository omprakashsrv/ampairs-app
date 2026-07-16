package com.ampairs.agent.data.db.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import com.ampairs.agent.data.db.entity.ChatMessageEntity

/**
 * DAO for the persisted assistant chat thread. The thread is small (capped at ~50), so each save
 * rewrites the table in order via [replaceAll] — exact and simple, no per-row diffing.
 */
@Dao
interface ChatMessageDao {

    @Query("SELECT * FROM chat_messages ORDER BY seq ASC")
    suspend fun getAll(): List<ChatMessageEntity>

    @Query("SELECT COUNT(*) FROM chat_messages")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<ChatMessageEntity>)

    @Query("DELETE FROM chat_messages")
    suspend fun clear()

    /** Launch-time housekeeping: keep only the most recent [limit] rows, delete the rest. */
    @Query("DELETE FROM chat_messages WHERE seq NOT IN (SELECT seq FROM chat_messages ORDER BY seq DESC LIMIT :limit)")
    suspend fun trimToLast(limit: Int)

    /** Replace the whole persisted thread with [messages] (already capped + seq-ordered). */
    @Transaction
    suspend fun replaceAll(messages: List<ChatMessageEntity>) {
        clear()
        insertAll(messages)
    }
}
