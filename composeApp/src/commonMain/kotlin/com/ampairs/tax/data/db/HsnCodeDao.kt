package com.ampairs.tax.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HsnCodeDao {

    @Query("SELECT * FROM hsn_codes WHERE is_active = 1 ORDER BY hsn_code ASC")
    fun getAllActiveHsnCodes(): Flow<List<HsnCodeEntity>>

    @Query("SELECT * FROM hsn_codes WHERE id = :id")
    suspend fun getHsnCodeById(id: String): HsnCodeEntity?

    @Query("SELECT * FROM hsn_codes WHERE hsn_code = :hsnCode LIMIT 1")
    suspend fun getHsnCodeByCode(hsnCode: String): HsnCodeEntity?

    @Query("""
        SELECT * FROM hsn_codes
        WHERE is_active = 1
        AND (
            hsn_code LIKE '%' || :query || '%'
            OR description LIKE '%' || :query || '%'
        )
        ORDER BY
            CASE WHEN hsn_code = :query THEN 1 ELSE 2 END,
            hsn_code ASC
        LIMIT :limit
    """)
    suspend fun searchHsnCodes(query: String, limit: Int = 50): List<HsnCodeEntity>

    @Query("""
        SELECT * FROM hsn_codes
        WHERE category = :category
        AND (:activeOnly = 0 OR is_active = 1)
        ORDER BY hsn_code ASC
    """)
    suspend fun getHsnCodesByCategory(category: String, activeOnly: Boolean = true): List<HsnCodeEntity>

    @Query("""
        SELECT * FROM hsn_codes
        WHERE chapter = :chapter
        AND (:activeOnly = 0 OR is_active = 1)
        ORDER BY hsn_code ASC
    """)
    suspend fun getHsnCodesByChapter(chapter: String, activeOnly: Boolean = true): List<HsnCodeEntity>

    @Query("SELECT DISTINCT chapter FROM hsn_codes WHERE is_active = 1 ORDER BY chapter ASC")
    suspend fun getAllChapters(): List<String>

    @Query("SELECT DISTINCT category FROM hsn_codes WHERE is_active = 1 ORDER BY category ASC")
    suspend fun getAllCategories(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHsnCode(hsnCode: HsnCodeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHsnCodes(hsnCodes: List<HsnCodeEntity>)

    @Update
    suspend fun updateHsnCode(hsnCode: HsnCodeEntity)

    @Delete
    suspend fun deleteHsnCode(hsnCode: HsnCodeEntity)

    @Query("DELETE FROM hsn_codes WHERE id = :id")
    suspend fun deleteHsnCodeById(id: String)

    @Query("UPDATE hsn_codes SET is_active = 0 WHERE id = :id")
    suspend fun deactivateHsnCode(id: String)

    @Query("UPDATE hsn_codes SET is_active = 1 WHERE id = :id")
    suspend fun activateHsnCode(id: String)

    @Query("SELECT COUNT(*) FROM hsn_codes WHERE is_active = 1")
    suspend fun getActiveHsnCodeCount(): Int

    @Query("SELECT * FROM hsn_codes WHERE sync_status != 'SYNCED'")
    suspend fun getUnsyncedHsnCodes(): List<HsnCodeEntity>

    @Query("UPDATE hsn_codes SET sync_status = :status, last_sync = :timestamp WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String, timestamp: Long)
}