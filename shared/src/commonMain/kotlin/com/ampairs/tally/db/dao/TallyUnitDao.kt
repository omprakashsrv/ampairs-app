package com.ampairs.tally.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ampairs.tally.db.entity.TallyUnitEntity

@Dao
interface TallyUnitDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(unit: TallyUnitEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(units: List<TallyUnitEntity>)
    
    @Update
    suspend fun update(unit: TallyUnitEntity)
    
    @Query("SELECT * FROM unitEntity")
    suspend fun selectAll(): List<TallyUnitEntity>
    
    @Query("SELECT * FROM unitEntity WHERE guid = :guid")
    suspend fun getByGuid(guid: String): TallyUnitEntity?
    
    @Query("SELECT * FROM unitEntity WHERE unitName = :unitName")
    suspend fun getByUnitName(unitName: String): TallyUnitEntity?
    
    @Query("SELECT * FROM unitEntity WHERE reservedName = :reservedName")
    suspend fun getByReservedName(reservedName: String): TallyUnitEntity?
    
    @Query("SELECT * FROM unitEntity WHERE gstRepUOM = :gstRepUOM")
    suspend fun getByGstRepUOM(gstRepUOM: String): List<TallyUnitEntity>
    
    @Query("SELECT * FROM unitEntity WHERE isSimpleUnit = :isSimple")
    suspend fun getBySimpleUnit(isSimple: String): List<TallyUnitEntity>
    
    @Query("SELECT * FROM unitEntity WHERE decimalPlaces = :decimalPlaces")
    suspend fun getByDecimalPlaces(decimalPlaces: String): List<TallyUnitEntity>
    
    @Query("SELECT * FROM unitEntity WHERE alterId = :alterId")
    suspend fun getByAlterId(alterId: String): TallyUnitEntity?
    
    @Query("SELECT * FROM unitEntity WHERE unitName LIKE '%' || :search || '%' OR reservedName LIKE '%' || :search || '%'")
    suspend fun searchUnits(search: String): List<TallyUnitEntity>
    
    @Query("SELECT DISTINCT gstRepUOM FROM unitEntity WHERE gstRepUOM != ''")
    suspend fun getAllGstRepUOMs(): List<String>
    
    @Query("SELECT COUNT(*) FROM unitEntity WHERE isSimpleUnit = :isSimple")
    suspend fun countBySimpleUnit(isSimple: String): Int
    
    @Query("DELETE FROM unitEntity WHERE guid = :guid")
    suspend fun deleteByGuid(guid: String)
    
    @Query("DELETE FROM unitEntity")
    suspend fun deleteAll()
    
    @Query("SELECT COUNT(*) FROM unitEntity")
    suspend fun count(): Int
}