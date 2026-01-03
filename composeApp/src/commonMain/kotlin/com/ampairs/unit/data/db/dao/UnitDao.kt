package com.ampairs.unit.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.ampairs.unit.data.db.entity.UnitEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Unit entity
 *
 * Provides reactive Flow-based queries for UI and suspend functions for one-time operations
 */
@Dao
interface UnitDao {

    /**
     * Get all active units as a reactive Flow
     * UI observes this for real-time updates
     */
    @Query("SELECT * FROM units WHERE active = 1 ORDER BY name ASC")
    fun getAllUnits(): Flow<List<UnitEntity>>

    /**
     * Search units by name as a reactive Flow
     */
    @Query("SELECT * FROM units WHERE name LIKE '%' || :query || '%' AND active = 1 ORDER BY name ASC")
    fun searchUnits(query: String): Flow<List<UnitEntity>>

    /**
     * Get unit by ID (one-time fetch)
     */
    @Query("SELECT * FROM units WHERE id = :id")
    suspend fun getUnitById(id: String): UnitEntity?

    /**
     * Get unit by name (one-time fetch for validation)
     */
    @Query("SELECT * FROM units WHERE name = :name AND active = 1")
    suspend fun getUnitByName(name: String): UnitEntity?

    /**
     * Get all unsynced units (for offline-first sync)
     */
    @Query("SELECT * FROM units WHERE synced = 0")
    suspend fun getUnsyncedUnits(): List<UnitEntity>

    /**
     * Insert or replace a single unit
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnit(unit: UnitEntity)

    /**
     * Insert or replace multiple units (batch operation)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnits(units: List<UnitEntity>)

    /**
     * Update an existing unit
     */
    @Update
    suspend fun updateUnit(unit: UnitEntity)

    /**
     * Soft delete: Mark unit as inactive
     */
    @Query("UPDATE units SET active = 0 WHERE id = :id")
    suspend fun deleteUnit(id: String)

    /**
     * Hard delete: Permanently remove unit from database
     * Use with caution - prefer soft delete
     */
    @Query("DELETE FROM units WHERE id = :id")
    suspend fun hardDeleteUnit(id: String)

    /**
     * Clear all units (for testing/reset)
     */
    @Query("DELETE FROM units")
    suspend fun clearAll()

    /**
     * Get count of active units
     */
    @Query("SELECT COUNT(*) FROM units WHERE active = 1")
    suspend fun getActiveUnitsCount(): Int

    /**
     * Transaction: Batch update units
     */
    @Transaction
    suspend fun updateUnits(units: List<UnitEntity>) {
        insertUnits(units)
    }
}
