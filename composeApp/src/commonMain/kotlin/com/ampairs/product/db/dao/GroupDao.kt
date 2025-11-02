package com.ampairs.product.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.ampairs.product.db.entity.GroupEntity

@Dao
interface GroupDao {

    @Query("SELECT * FROM groupEntity WHERE id = :id")
    suspend fun groupById(id: String): GroupEntity?

    @Query("SELECT * FROM groupEntity ORDER BY name ASC, active DESC")
    suspend fun getGroups(): List<GroupEntity>

    @Query("SELECT * FROM groupEntity WHERE synced = 0")
    suspend fun unSyncedGroups(): List<GroupEntity>

    @Query("SELECT * FROM groupEntity WHERE active = 1 ORDER BY name ASC")
    suspend fun getActiveGroups(): List<GroupEntity>

    @Query("SELECT * FROM groupEntity WHERE name LIKE '%' || :searchText || '%' ORDER BY name ASC")
    suspend fun getGroupsByName(searchText: String): List<GroupEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(group: GroupEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(groups: List<GroupEntity>)

    @Update
    suspend fun update(group: GroupEntity)

    @Query("DELETE FROM groupEntity WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM groupEntity")
    suspend fun deleteAll()

    @Query("UPDATE groupEntity SET synced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)

    @Query("UPDATE groupEntity SET soft_deleted = 1 WHERE id = :id")
    suspend fun softDelete(id: String)

    @Query("UPDATE groupEntity SET active = :active WHERE id = :id")
    suspend fun updateActiveStatus(id: String, active: Int)

    @Transaction
    suspend fun insertGroups(groups: List<GroupEntity>) {
        insertAll(groups)
    }
}