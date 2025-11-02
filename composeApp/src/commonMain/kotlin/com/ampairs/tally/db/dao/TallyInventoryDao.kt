package com.ampairs.tally.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Update
import com.ampairs.tally.db.entity.TallyInventoryEntity

@Dao
interface TallyInventoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(inventory: TallyInventoryEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(inventories: List<TallyInventoryEntity>)
    
    @Update
    suspend fun update(inventory: TallyInventoryEntity)
    
    @Query("SELECT * FROM inventoryEntity")
    suspend fun selectAll(): List<TallyInventoryEntity>
    
    @Query("SELECT * FROM inventoryEntity WHERE guid = :guid")
    suspend fun getByGuid(guid: String): TallyInventoryEntity?
    
    @Query("SELECT * FROM inventoryEntity WHERE name = :name")
    suspend fun getByName(name: String): TallyInventoryEntity?
    
    @Query("SELECT * FROM inventoryEntity WHERE parent = :parent")
    suspend fun getByParent(parent: String): List<TallyInventoryEntity>
    
    @Query("SELECT * FROM inventoryEntity WHERE parent_id = :parentId")
    suspend fun getByParentId(parentId: String): List<TallyInventoryEntity>
    
    @Query("SELECT * FROM inventoryEntity WHERE alterId = :alterId")
    suspend fun getByAlterId(alterId: String): TallyInventoryEntity?
    
    @Query("SELECT * FROM inventoryEntity WHERE parent = '' OR parent IS NULL")
    suspend fun getRootInventories(): List<TallyInventoryEntity>
    
    @Query("SELECT * FROM inventoryEntity WHERE parent != '' AND parent IS NOT NULL")
    suspend fun getChildInventories(): List<TallyInventoryEntity>
    
    @Query("SELECT * FROM inventoryEntity WHERE name LIKE '%' || :search || '%'")
    suspend fun searchInventories(search: String): List<TallyInventoryEntity>
    
    @RewriteQueriesToDropUnusedColumns
    @Query("""
        WITH RECURSIVE inventory_hierarchy AS (
            SELECT *, 0 as level FROM inventoryEntity WHERE guid = :rootGuid
            UNION ALL
            SELECT i.*, ih.level + 1 FROM inventoryEntity i
            INNER JOIN inventory_hierarchy ih ON i.parent_id = ih.guid
        )
        SELECT * FROM inventory_hierarchy ORDER BY level, name
    """)
    suspend fun getInventoryHierarchy(rootGuid: String): List<TallyInventoryEntity>
    
    @RewriteQueriesToDropUnusedColumns
    @Query("""
        WITH RECURSIVE parent_hierarchy AS (
            SELECT *, 0 as level FROM inventoryEntity WHERE guid = :childGuid
            UNION ALL
            SELECT i.*, ph.level + 1 FROM inventoryEntity i
            INNER JOIN parent_hierarchy ph ON i.guid = ph.parent_id
        )
        SELECT * FROM parent_hierarchy ORDER BY level DESC
    """)
    suspend fun getParentHierarchy(childGuid: String): List<TallyInventoryEntity>
    
    @Query("SELECT COUNT(*) FROM inventoryEntity WHERE parent_id = :parentId")
    suspend fun countChildInventories(parentId: String): Int
    
    @Query("SELECT DISTINCT parent FROM inventoryEntity WHERE parent != ''")
    suspend fun getAllParents(): List<String>
    
    @Query("SELECT DISTINCT parent_id FROM inventoryEntity WHERE parent_id != ''")
    suspend fun getAllParentIds(): List<String>
    
    @Query("DELETE FROM inventoryEntity WHERE guid = :guid")
    suspend fun deleteByGuid(guid: String)
    
    @Query("DELETE FROM inventoryEntity")
    suspend fun deleteAll()
    
    @Query("SELECT COUNT(*) FROM inventoryEntity")
    suspend fun count(): Int
}