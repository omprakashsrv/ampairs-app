package com.ampairs.tally.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Update
import com.ampairs.tally.db.entity.StockGroupEntity

@Dao
interface StockGroupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stockGroup: StockGroupEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stockGroups: List<StockGroupEntity>)
    
    @Update
    suspend fun update(stockGroup: StockGroupEntity)
    
    @Query("SELECT * FROM stockGroupEntity")
    suspend fun selectAll(): List<StockGroupEntity>
    
    @Query("SELECT * FROM stockGroupEntity WHERE guid = :guid")
    suspend fun getByGuid(guid: String): StockGroupEntity?
    
    @Query("SELECT * FROM stockGroupEntity WHERE unitName = :unitName")
    suspend fun getByUnitName(unitName: String): StockGroupEntity?
    
    @Query("SELECT * FROM stockGroupEntity WHERE reservedName = :reservedName")
    suspend fun getByReservedName(reservedName: String): StockGroupEntity?
    
    @Query("SELECT * FROM stockGroupEntity WHERE parent = :parent")
    suspend fun getByParent(parent: String): List<StockGroupEntity>
    
    @Query("SELECT * FROM stockGroupEntity WHERE parentGuid = :parentGuid")
    suspend fun getByParentGuid(parentGuid: String): List<StockGroupEntity>
    
    @Query("SELECT * FROM stockGroupEntity WHERE alterId = :alterId")
    suspend fun getByAlterId(alterId: String): StockGroupEntity?
    
    @Query("SELECT * FROM stockGroupEntity WHERE parent = '' OR parent IS NULL")
    suspend fun getRootGroups(): List<StockGroupEntity>
    
    @Query("SELECT * FROM stockGroupEntity WHERE parent != '' AND parent IS NOT NULL")
    suspend fun getChildGroups(): List<StockGroupEntity>
    
    @Query("SELECT * FROM stockGroupEntity WHERE unitName LIKE '%' || :search || '%' OR reservedName LIKE '%' || :search || '%'")
    suspend fun searchStockGroups(search: String): List<StockGroupEntity>
    
    @RewriteQueriesToDropUnusedColumns
    @Query("""
        WITH RECURSIVE group_hierarchy AS (
            SELECT *, 0 as level FROM stockGroupEntity WHERE guid = :rootGuid
            UNION ALL
            SELECT sg.*, gh.level + 1 FROM stockGroupEntity sg
            INNER JOIN group_hierarchy gh ON sg.parentGuid = gh.guid
        )
        SELECT * FROM group_hierarchy ORDER BY level, unitName
    """)
    suspend fun getGroupHierarchy(rootGuid: String): List<StockGroupEntity>
    
    @RewriteQueriesToDropUnusedColumns
    @Query("""
        WITH RECURSIVE parent_hierarchy AS (
            SELECT *, 0 as level FROM stockGroupEntity WHERE guid = :childGuid
            UNION ALL
            SELECT sg.*, ph.level + 1 FROM stockGroupEntity sg
            INNER JOIN parent_hierarchy ph ON sg.guid = ph.parentGuid
        )
        SELECT * FROM parent_hierarchy ORDER BY level DESC
    """)
    suspend fun getParentHierarchy(childGuid: String): List<StockGroupEntity>
    
    @Query("SELECT COUNT(*) FROM stockGroupEntity WHERE parentGuid = :parentGuid")
    suspend fun countChildGroups(parentGuid: String): Int
    
    @Query("DELETE FROM stockGroupEntity WHERE guid = :guid")
    suspend fun deleteByGuid(guid: String)
    
    @Query("DELETE FROM stockGroupEntity")
    suspend fun deleteAll()
    
    @Query("SELECT COUNT(*) FROM stockGroupEntity")
    suspend fun count(): Int
}