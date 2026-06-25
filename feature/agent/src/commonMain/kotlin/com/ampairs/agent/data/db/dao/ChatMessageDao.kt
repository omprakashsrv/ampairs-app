package com.ampairs.agent.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.ampairs.agent.data.db.entity.ChatMessageEntity

/**
 * DAO for the persisted assistant chat thread. The thread is small (capped at ~50), so each save
 * rewrites the table in order via [replaceAll] — exact and simple, no per-row diffing.
 */
@Dao
interface ChatMessageDao {

    @Query("SELECT * FROM chat_messages ORDER BY seq ASC")
    suspend fun getAll(): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<ChatMessageEntity>)

    @Query("DELETE FROM chat_messages")
    suspend fun clear()

    /** Replace the whole persisted thread with [messages] (already capped + seq-ordered). */
    @Transaction
    suspend fun replaceAll(messages: List<ChatMessageEntity>) {
        clear()
        insertAll(messages)
    }
}
