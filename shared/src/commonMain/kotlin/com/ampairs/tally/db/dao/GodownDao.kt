package com.ampairs.tally.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Update
import com.ampairs.tally.db.entity.GodownEntity

@Dao
interface GodownDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(godown: GodownEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(godowns: List<GodownEntity>)
    
    @Update
    suspend fun update(godown: GodownEntity)
    
    @Query("SELECT * FROM godownEntity")
    suspend fun selectAll(): List<GodownEntity>
    
    @Query("SELECT * FROM godownEntity WHERE guid = :guid")
    suspend fun getByGuid(guid: String): GodownEntity?
    
    @Query("SELECT * FROM godownEntity WHERE name = :name")
    suspend fun getByName(name: String): GodownEntity?
    
    @Query("SELECT * FROM godownEntity WHERE parent = :parent")
    suspend fun getByParent(parent: String): List<GodownEntity>
    
    @Query("SELECT * FROM godownEntity WHERE alterId = :alterId")
    suspend fun getByAlterId(alterId: String): GodownEntity?
    
    @Query("SELECT * FROM godownEntity WHERE parent = '' OR parent IS NULL")
    suspend fun getRootGodowns(): List<GodownEntity>
    
    @Query("SELECT * FROM godownEntity WHERE parent != '' AND parent IS NOT NULL")
    suspend fun getChildGodowns(): List<GodownEntity>
    
    @Query("SELECT * FROM godownEntity WHERE name LIKE '%' || :search || '%'")
    suspend fun searchGodowns(search: String): List<GodownEntity>
    
    @RewriteQueriesToDropUnusedColumns
    @Query("""
        WITH RECURSIVE godown_hierarchy AS (
            SELECT *, 0 as level FROM godownEntity WHERE guid = :rootGuid
            UNION ALL
            SELECT g.*, gh.level + 1 FROM godownEntity g
            INNER JOIN godown_hierarchy gh ON g.parent = gh.name
        )
        SELECT * FROM godown_hierarchy ORDER BY level, name
    """)
    suspend fun getGodownHierarchy(rootGuid: String): List<GodownEntity>
    
    @RewriteQueriesToDropUnusedColumns
    @Query("""
        WITH RECURSIVE parent_hierarchy AS (
            SELECT *, 0 as level FROM godownEntity WHERE guid = :childGuid
            UNION ALL
            SELECT g.*, ph.level + 1 FROM godownEntity g
            INNER JOIN parent_hierarchy ph ON g.name = ph.parent
        )
        SELECT * FROM parent_hierarchy ORDER BY level DESC
    """)
    suspend fun getParentHierarchy(childGuid: String): List<GodownEntity>
    
    @Query("SELECT COUNT(*) FROM godownEntity WHERE parent = :parent")
    suspend fun countChildGodowns(parent: String): Int
    
    @Query("SELECT DISTINCT parent FROM godownEntity WHERE parent != ''")
    suspend fun getAllParents(): List<String>
    
    @Query("DELETE FROM godownEntity WHERE guid = :guid")
    suspend fun deleteByGuid(guid: String)
    
    @Query("DELETE FROM godownEntity")
    suspend fun deleteAll()
    
    @Query("SELECT COUNT(*) FROM godownEntity")
    suspend fun count(): Int
}