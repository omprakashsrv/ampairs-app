package com.ampairs.tally.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Update
import com.ampairs.tally.db.entity.StockCategoryEntity

@Dao
interface StockCategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stockCategory: StockCategoryEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stockCategories: List<StockCategoryEntity>)
    
    @Update
    suspend fun update(stockCategory: StockCategoryEntity)
    
    @Query("SELECT * FROM stockCategoryEntity")
    suspend fun selectAll(): List<StockCategoryEntity>
    
    @Query("SELECT * FROM stockCategoryEntity WHERE guid = :guid")
    suspend fun getByGuid(guid: String): StockCategoryEntity?
    
    @Query("SELECT * FROM stockCategoryEntity WHERE unitName = :unitName")
    suspend fun getByUnitName(unitName: String): StockCategoryEntity?
    
    @Query("SELECT * FROM stockCategoryEntity WHERE reservedName = :reservedName")
    suspend fun getByReservedName(reservedName: String): StockCategoryEntity?
    
    @Query("SELECT * FROM stockCategoryEntity WHERE parent = :parent")
    suspend fun getByParent(parent: String): List<StockCategoryEntity>
    
    @Query("SELECT * FROM stockCategoryEntity WHERE parentGuid = :parentGuid")
    suspend fun getByParentGuid(parentGuid: String): List<StockCategoryEntity>
    
    @Query("SELECT * FROM stockCategoryEntity WHERE alterId = :alterId")
    suspend fun getByAlterId(alterId: String): StockCategoryEntity?
    
    @Query("SELECT * FROM stockCategoryEntity WHERE parent = '' OR parent IS NULL")
    suspend fun getRootCategories(): List<StockCategoryEntity>
    
    @Query("SELECT * FROM stockCategoryEntity WHERE parent != '' AND parent IS NOT NULL")
    suspend fun getChildCategories(): List<StockCategoryEntity>
    
    @Query("SELECT * FROM stockCategoryEntity WHERE unitName LIKE '%' || :search || '%' OR reservedName LIKE '%' || :search || '%'")
    suspend fun searchStockCategories(search: String): List<StockCategoryEntity>
    
    @RewriteQueriesToDropUnusedColumns
    @Query("""
        WITH RECURSIVE category_hierarchy AS (
            SELECT *, 0 as level FROM stockCategoryEntity WHERE guid = :rootGuid
            UNION ALL
            SELECT sc.*, ch.level + 1 FROM stockCategoryEntity sc
            INNER JOIN category_hierarchy ch ON sc.parentGuid = ch.guid
        )
        SELECT * FROM category_hierarchy ORDER BY level, unitName
    """)
    suspend fun getCategoryHierarchy(rootGuid: String): List<StockCategoryEntity>
    
    @RewriteQueriesToDropUnusedColumns
    @Query("""
        WITH RECURSIVE parent_hierarchy AS (
            SELECT *, 0 as level FROM stockCategoryEntity WHERE guid = :childGuid
            UNION ALL
            SELECT sc.*, ph.level + 1 FROM stockCategoryEntity sc
            INNER JOIN parent_hierarchy ph ON sc.guid = ph.parentGuid
        )
        SELECT * FROM parent_hierarchy ORDER BY level DESC
    """)
    suspend fun getParentHierarchy(childGuid: String): List<StockCategoryEntity>
    
    @Query("SELECT COUNT(*) FROM stockCategoryEntity WHERE parentGuid = :parentGuid")
    suspend fun countChildCategories(parentGuid: String): Int
    
    @Query("DELETE FROM stockCategoryEntity WHERE guid = :guid")
    suspend fun deleteByGuid(guid: String)
    
    @Query("DELETE FROM stockCategoryEntity")
    suspend fun deleteAll()
    
    @Query("SELECT COUNT(*) FROM stockCategoryEntity")
    suspend fun count(): Int
}