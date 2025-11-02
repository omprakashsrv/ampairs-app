package com.ampairs.product.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.ampairs.product.db.entity.UnitConversionEntity

@Dao
interface UnitConversionDao {

    @Query("SELECT * FROM unitConversionEntity WHERE id = :id")
    suspend fun unitConversionById(id: String): UnitConversionEntity?

    @Query("SELECT * FROM unitConversionEntity")
    suspend fun getAllUnitConversions(): List<UnitConversionEntity>

    @Query("SELECT * FROM unitConversionEntity WHERE product_id = :productId")
    suspend fun getUnitConversionsByProductId(productId: String): List<UnitConversionEntity>

    @Query("SELECT * FROM unitConversionEntity WHERE base_unit_id = :baseUnitId")
    suspend fun getUnitConversionsByBaseUnit(baseUnitId: String): List<UnitConversionEntity>

    @Query("SELECT * FROM unitConversionEntity WHERE derived_unit_id = :derivedUnitId")
    suspend fun getUnitConversionsByDerivedUnit(derivedUnitId: String): List<UnitConversionEntity>

    @Query("SELECT * FROM unitConversionEntity WHERE product_id = :productId AND base_unit_id = :baseUnitId AND derived_unit_id = :derivedUnitId")
    suspend fun getUnitConversion(productId: String, baseUnitId: String, derivedUnitId: String): UnitConversionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(unitConversion: UnitConversionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(unitConversions: List<UnitConversionEntity>)

    @Update
    suspend fun update(unitConversion: UnitConversionEntity)

    @Query("DELETE FROM unitConversionEntity WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM unitConversionEntity WHERE product_id = :productId")
    suspend fun deleteByProductId(productId: String)

    @Query("DELETE FROM unitConversionEntity")
    suspend fun deleteAll()

    @Transaction
    suspend fun updateUnitConversions(unitConversions: List<UnitConversionEntity>) {
        insertAll(unitConversions)
    }
}