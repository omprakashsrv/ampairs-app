package com.ampairs.product.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.ampairs.product.db.entity.UnitEntity

@Dao
interface UnitDao {

    @Query("SELECT * FROM unitEntity WHERE id = :id")
    suspend fun unitById(id: String): UnitEntity?

    @Query("SELECT * FROM unitEntity")
    suspend fun findAll(): List<UnitEntity>

    @Query("SELECT * FROM unitEntity ORDER BY name ASC")
    suspend fun getAllUnits(): List<UnitEntity>

    @Query("SELECT * FROM unitEntity WHERE name LIKE '%' || :searchText || '%' ORDER BY name ASC")
    suspend fun getUnitsByName(searchText: String): List<UnitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(unit: UnitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(units: List<UnitEntity>)

    @Update
    suspend fun update(unit: UnitEntity)

    @Query("DELETE FROM unitEntity WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM unitEntity")
    suspend fun deleteAll()

    @Transaction
    suspend fun updateUnits(units: List<UnitEntity>) {
        insertAll(units)
    }
}