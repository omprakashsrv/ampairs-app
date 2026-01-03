package com.ampairs.unit.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.ampairs.unit.data.db.entity.UnitConversionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for UnitConversion entity
 *
 * Handles product-specific unit conversion storage and queries
 */
@Dao
interface UnitConversionDao {

    /**
     * Get all active unit conversions as a reactive Flow
     */
    @Query("SELECT * FROM unit_conversions WHERE active = 1")
    fun getAllUnitConversions(): Flow<List<UnitConversionEntity>>

    /**
     * Get all conversions for a specific product as a reactive Flow
     */
    @Query("SELECT * FROM unit_conversions WHERE product_id = :productId AND active = 1")
    fun getUnitConversionsByProductId(productId: String): Flow<List<UnitConversionEntity>>

    /**
     * Get specific conversion (one-time fetch for conversion engine)
     *
     * Example: Convert 1 BOX to PCS for Product A
     */
    @Query("""
        SELECT * FROM unit_conversions
        WHERE product_id = :productId
        AND base_unit_id = :baseUnitId
        AND derived_unit_id = :derivedUnitId
        AND active = 1
    """)
    suspend fun getUnitConversion(
        productId: String,
        baseUnitId: String,
        derivedUnitId: String
    ): UnitConversionEntity?

    /**
     * Get conversion by ID (one-time fetch)
     */
    @Query("SELECT * FROM unit_conversions WHERE id = :id")
    suspend fun getUnitConversionById(id: String): UnitConversionEntity?

    /**
     * Get all unsynced conversions (for offline-first sync)
     */
    @Query("SELECT * FROM unit_conversions WHERE synced = 0")
    suspend fun getUnsyncedUnitConversions(): List<UnitConversionEntity>

    /**
     * Get all conversions using a specific base unit
     */
    @Query("SELECT * FROM unit_conversions WHERE base_unit_id = :baseUnitId AND active = 1")
    suspend fun getUnitConversionsByBaseUnit(baseUnitId: String): List<UnitConversionEntity>

    /**
     * Get all conversions resulting in a specific derived unit
     */
    @Query("SELECT * FROM unit_conversions WHERE derived_unit_id = :derivedUnitId AND active = 1")
    suspend fun getUnitConversionsByDerivedUnit(derivedUnitId: String): List<UnitConversionEntity>

    /**
     * Insert or replace a single conversion
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnitConversion(unitConversion: UnitConversionEntity)

    /**
     * Insert or replace multiple conversions (batch operation)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnitConversions(unitConversions: List<UnitConversionEntity>)

    /**
     * Update an existing conversion
     */
    @Update
    suspend fun updateUnitConversion(unitConversion: UnitConversionEntity)

    /**
     * Delete a specific conversion
     */
    @Query("DELETE FROM unit_conversions WHERE id = :id")
    suspend fun deleteUnitConversion(id: String)

    /**
     * Delete all conversions for a product
     * Useful when product is deleted
     */
    @Query("DELETE FROM unit_conversions WHERE product_id = :productId")
    suspend fun deleteByProductId(productId: String)

    /**
     * Clear all conversions (for testing/reset)
     */
    @Query("DELETE FROM unit_conversions")
    suspend fun clearAll()

    /**
     * Transaction: Batch update conversions
     */
    @Transaction
    suspend fun updateUnitConversions(unitConversions: List<UnitConversionEntity>) {
        insertUnitConversions(unitConversions)
    }
}
