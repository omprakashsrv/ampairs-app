package com.ampairs.tax.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ampairs.tax.data.db.entity.TaxCodeEntity
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

/**
 * Tax Code DAO
 *
 * Note: No workspace_id in queries - workspace isolation handled by WorkspaceAwareDatabaseFactory
 * Each workspace gets its own database instance
 */
@Dao
interface TaxCodeDao {

    @Query("SELECT * FROM tax_codes WHERE is_active = 1 ORDER BY usage_count DESC")
    fun observeTaxCodes(): Flow<List<TaxCodeEntity>>

    @Query("SELECT * FROM tax_codes WHERE id = :id")
    suspend fun getById(id: String): TaxCodeEntity?

    @Query("SELECT * FROM tax_codes WHERE code = :code AND is_active = 1")
    suspend fun getByCode(code: String): TaxCodeEntity?

    @Query("SELECT * FROM tax_codes WHERE is_favorite = 1 AND is_active = 1 ORDER BY usage_count DESC")
    fun observeFavorites(): Flow<List<TaxCodeEntity>>

    @Query("""
        SELECT * FROM tax_codes
        WHERE is_active = 1
        AND (code LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR short_description LIKE '%' || :query || '%')
        ORDER BY usage_count DESC
        LIMIT :limit
    """)
    suspend fun searchTaxCodes(query: String, limit: Int = 50): List<TaxCodeEntity>

    @Query("SELECT * FROM tax_codes WHERE sync_status != 'SYNCED'")
    suspend fun getUnsyncedCodes(): List<TaxCodeEntity>

    @Query("SELECT * FROM tax_codes WHERE updated_at > :modifiedAfter")
    suspend fun getModifiedAfter(modifiedAfter: Instant): List<TaxCodeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(code: TaxCodeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(codes: List<TaxCodeEntity>)

    @Update
    suspend fun update(code: TaxCodeEntity)

    @Query("UPDATE tax_codes SET usage_count = usage_count + 1, last_used_at = :timestamp WHERE id = :id")
    suspend fun incrementUsageCount(id: String, timestamp: Instant)

    @Query("UPDATE tax_codes SET is_favorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: String, isFavorite: Boolean)

    @Query("UPDATE tax_codes SET sync_status = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)

    @Query("UPDATE tax_codes SET is_active = 0 WHERE id = :id")
    suspend fun deactivate(id: String)

    @Query("DELETE FROM tax_codes WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM tax_codes")
    suspend fun deleteAll()
}
