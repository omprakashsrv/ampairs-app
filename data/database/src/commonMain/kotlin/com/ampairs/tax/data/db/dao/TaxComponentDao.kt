package com.ampairs.tax.data.db.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import com.ampairs.tax.data.db.entity.TaxComponentEntity
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

/**
 * Tax Component DAO
 *
 * Note: No workspace_id in queries - workspace isolation handled by WorkspaceAwareDatabaseFactory
 */
@Dao
interface TaxComponentDao {

    @Query("""
        SELECT * FROM tax_components
        WHERE is_active = 1
        ORDER BY calculation_order ASC
    """)
    fun observeTaxComponents(): Flow<List<TaxComponentEntity>>

    @Query("""
        SELECT * FROM tax_components
        WHERE jurisdiction = :jurisdiction
        AND is_active = 1
        AND effective_from <= :effectiveDate
        AND (effective_to IS NULL OR effective_to >= :effectiveDate)
        ORDER BY calculation_order ASC
    """)
    suspend fun getComponentsByJurisdiction(
        jurisdiction: String,
        effectiveDate: Instant
    ): List<TaxComponentEntity>

    @Query("SELECT * FROM tax_components WHERE id = :id")
    suspend fun getById(id: String): TaxComponentEntity?

    @Query("""
        SELECT * FROM tax_components
        WHERE component_type_id = :componentTypeId
        AND jurisdiction = :jurisdiction
        AND is_active = 1
    """)
    suspend fun getByTypeAndJurisdiction(
        componentTypeId: String,
        jurisdiction: String
    ): TaxComponentEntity?

    @Query("SELECT * FROM tax_components WHERE sync_status != 'SYNCED'")
    suspend fun getUnsyncedComponents(): List<TaxComponentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(component: TaxComponentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(components: List<TaxComponentEntity>)

    @Update
    suspend fun update(component: TaxComponentEntity)

    @Query("UPDATE tax_components SET sync_status = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)

    @Query("UPDATE tax_components SET is_active = 0 WHERE id = :id")
    suspend fun deactivate(id: String)

    @Query("DELETE FROM tax_components WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM tax_components")
    suspend fun deleteAll()
}
